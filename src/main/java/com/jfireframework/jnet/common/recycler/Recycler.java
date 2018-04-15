package com.jfireframework.jnet.common.recycler;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Recycler<T>
{
    private int                                           initElementCapacity   = 1024;
    private int                                           maxCapacityPerThread  = 1 << 13;
    private int                                           sharedCapacity        = 1 << 11;
    private int                                           linkCapacity          = 1024;
    private int                                           maxOtherRecycleThread = 128;
    private ThreadLocal<Stack>                            local                 = new ThreadLocal<Stack>() {
                                                                                    @Override
                                                                                    protected Stack initialValue()
                                                                                    {
                                                                                        return new Stack();
                                                                                    }
                                                                                };
    private ThreadLocal<WeakHashMap<Stack, RecycleQueue>> local2                = new ThreadLocal<WeakHashMap<Stack, RecycleQueue>>() {
                                                                                    
                                                                                    @Override
                                                                                    protected WeakHashMap<Stack, RecycleQueue> initialValue()
                                                                                    {
                                                                                        return new WeakHashMap<Stack, RecycleQueue>();
                                                                                    };
                                                                                };
    private static final RecycleQueue                     NO_OP_RECYCLE_QUEUE   = new RecycleQueue() {
                                                                                    
                                                                                    @Override
                                                                                    public boolean add(RecycleHandler handler)
                                                                                    {
                                                                                        return false;
                                                                                    }
                                                                                };
    
    public T get()
    {
        Stack stack = local.get();
        DefaultHandler handler = stack.pop();
        if (handler == null)
        {
            handler = new DefaultHandler();
            handler.stack = stack;
            handler.value = newObject(handler);
        }
        return handler.value;
    }
    
    protected abstract T newObject(RecycleHandler handler);
    
    public static interface RecycleHandler
    {
        boolean recycle(Object item);
    }
    
    public class DefaultHandler implements RecycleHandler
    {
        T     value;
        Stack stack;
        
        @Override
        public boolean recycle(Object item)
        {
            if (value != item)
            {
                throw new IllegalArgumentException("不是handler所属的对象");
            }
            return stack.push(this);
        }
    }
    
    class Stack
    {
        /**
         * 元素
         */
        RecycleHandler[]                         elements;
        // 代表当前stack内元素的个数
        int                                      size;
        WeakReference<Thread>                    ownThread;
        AtomicInteger                            avaliableSharedCapacity;
        private volatile OtherThreadRecycleQueue head;
        private OtherThreadRecycleQueue          pred;
        private OtherThreadRecycleQueue          cursor;
        
        public Stack()
        {
            ownThread = new WeakReference<Thread>(Thread.currentThread());
            avaliableSharedCapacity = new AtomicInteger(sharedCapacity);
            elements = new RecycleHandler[initElementCapacity];
        }
        
        public boolean push(RecycleHandler handler)
        {
            Thread currentThread = Thread.currentThread();
            if (currentThread == ownThread.get())
            {
                return pushNow(handler);
            }
            else
            {
                return pushLater(handler, currentThread);
            }
        }
        
        public synchronized void setHead(OtherThreadRecycleQueue queue)
        {
            queue.next = head;
            head = queue;
        }
        
        /**
         * 马上推送到elements数组中
         */
        private boolean pushNow(RecycleHandler handler)
        {
            if (size == elements.length)
            {
                if (size == maxCapacityPerThread)
                {
                    return false;
                }
                elements = Arrays.copyOf(elements, size << 1);
            }
            elements[size++] = handler;
            return true;
        }
        
        /**
         * 推送到Queue中
         */
        @SuppressWarnings("unchecked")
        private boolean pushLater(RecycleHandler handler, Thread thread)
        {
            WeakHashMap<Stack, RecycleQueue> weakHashMap = local2.get();
            RecycleQueue queue = weakHashMap.get(this);
            if (queue == null)
            {
                if (weakHashMap.size() >= maxOtherRecycleThread)
                {
                    weakHashMap.put(this, NO_OP_RECYCLE_QUEUE);
                    return false;
                }
                else
                {
                    for (;;)
                    {
                        int available = avaliableSharedCapacity.get();
                        if (available < linkCapacity)
                        {
                            return false;
                        }
                        if (avaliableSharedCapacity.compareAndSet(available, available - linkCapacity))
                        {
                            queue = new OtherThreadRecycleQueue(thread, avaliableSharedCapacity);
                            weakHashMap.put(this, queue);
                            setHead((Recycler<T>.OtherThreadRecycleQueue) queue);
                            break;
                        }
                    }
                }
            }
            // 如果handler不在本stack内部时，应该设置为null。这样如果stack线程死亡，不存在引用，可以使其回收。
            ((DefaultHandler) handler).stack = null;
            return queue.add(handler);
        }
        
        @SuppressWarnings("unchecked")
        public DefaultHandler pop()
        {
            if (size == 0)
            {
                if (transferSome())
                {
                    return (DefaultHandler) elements[--size];
                }
                else
                {
                    return null;
                }
            }
            else
            {
                return (DefaultHandler) elements[--size];
            }
        }
        
        /**
         * 尝试从queue中获取数据到stack中
         * 
         * @return
         */
        private boolean transferSome()
        {
            OtherThreadRecycleQueue cursor = this.cursor;
            OtherThreadRecycleQueue pred = this.pred;
            if (cursor == null)
            {
                cursor = head;
                pred = null;
                if (cursor == null)
                {
                    return false;
                }
            }
            boolean success = false;
            do
            {
                if (cursor.transfer(this))
                {
                    success = true;
                    break;
                }
                OtherThreadRecycleQueue next = cursor.next;
                // 如果当前queue的线程已经结束，则尝试将其中所有的数据都取出了
                if (cursor.ownerThread.get() == null)
                {
                    if (cursor.hasData())
                    {
                        for (;;)
                        {
                            if (cursor.transfer(this))
                            {
                                success = true;
                            }
                            else
                            {
                                break;
                            }
                        }
                    }
                    if (pred != null)
                    {
                        pred.next = next;
                    }
                }
                else
                {
                    pred = cursor;
                }
                cursor = next;
            } while (cursor != null && success != true);
            this.cursor = cursor;
            this.pred = pred;
            return success;
        }
    }
    
    interface RecycleQueue
    {
        boolean add(RecycleHandler handler);
    }
    
    class OtherThreadRecycleQueue implements RecycleQueue
    {
        Link                    cursor;
        Link                    tail;
        WeakReference<Thread>   ownerThread;
        AtomicInteger           avaliableSharedCapacity;
        OtherThreadRecycleQueue next;
        
        public OtherThreadRecycleQueue(Thread thread, AtomicInteger avaliableSharedCapacity)
        {
            this.avaliableSharedCapacity = avaliableSharedCapacity;
            ownerThread = new WeakReference<Thread>(thread);
            cursor = tail = new Link();
        }
        
        public boolean hasData()
        {
            return tail.readIndex != tail.get();
        }
        
        @Override
        public boolean add(RecycleHandler handler)
        {
            Link tail = this.tail;
            int writeIndex = tail.get();
            if (writeIndex == linkCapacity)
            {
                if (allocateSpace())
                {
                    this.tail = tail = tail.next = new Link();
                    writeIndex = tail.get();
                }
                else
                {
                    return false;
                }
            }
            tail.add(handler, writeIndex);
            return true;
        }
        
        private boolean allocateSpace()
        {
            for (;;)
            {
                int available = avaliableSharedCapacity.get();
                if (available < linkCapacity)
                {
                    return false;
                }
                if (avaliableSharedCapacity.compareAndSet(available, available - linkCapacity))
                {
                    return true;
                }
            }
        }
        
        /**
         * 如果stack没有容量了返回false，当前link已经读取完毕且没有下一个返回false，当前link内不存在可以读取的数据，返回false
         * 
         * @param stack
         * @return
         */
        @SuppressWarnings("unchecked")
        public boolean transfer(Stack stack)
        {
            Link now = cursor;
            if (now.readIndex == linkCapacity)
            {
                if (now.next == null)
                {
                    return false;
                }
                now = cursor = now.next;
            }
            RecycleHandler[] dest = stack.elements;
            int size = stack.size;
            int lenth = dest.length;
            if (size == lenth)
            {
                if (size == maxCapacityPerThread)
                {
                    return false;
                }
                dest = Arrays.copyOf(dest, size << 1);
                stack.elements = dest;
                lenth = dest.length;
            }
            RecycleHandler[] src = now.elements;
            int readIndex = now.readIndex;
            int writeIndex = now.get();
            int dataCount = writeIndex - readIndex;
            if (dataCount == 0)
            {
                return false;
            }
            int min = Math.min(lenth - size, dataCount);
            System.arraycopy(src, readIndex, dest, size, min);
            int end = size + min;
            for (int i = size; i < end; i++)
            {
                ((DefaultHandler) dest[i]).stack = stack;
            }
            stack.size = end;
            now.readIndex = readIndex + min;
            return true;
        }
        
        @Override
        protected void finalize() throws Throwable
        {
            try
            {
                super.finalize();
            }
            finally
            {
                while (cursor != null)
                {
                    avaliableSharedCapacity.addAndGet(linkCapacity);
                    cursor = cursor.next;
                }
            }
        }
    }
    
    class Link extends AtomicInteger
    {
        /**
         * 
         */
        private static final long serialVersionUID = 4142517045913821970L;
        RecycleHandler[]          elements;
        Link                      next;
        int                       readIndex;
        
        public Link()
        {
            elements = new RecycleHandler[linkCapacity];
        }
        
        public void add(RecycleHandler handler, int writeIndex)
        {
            elements[writeIndex] = handler;
            lazySet(writeIndex + 1);
        }
    }
    
}
