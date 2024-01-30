package com.jfirer.jnet.common.recycler;

import com.jfirer.jnet.common.thread.FastThreadLocal;
import com.jfirer.jnet.common.util.MathUtil;
import com.jfirer.jnet.common.util.SystemPropertyUtil;
import com.jfirer.jnet.common.util.UNSAFE;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class Recycler<T>
{
    public static final AtomicInteger                               IDGENERATOR                 = new AtomicInteger(0);
    public static final int                                         recyclerId                  = IDGENERATOR.getAndIncrement();
    // Stack最大可以存储的缓存对象个数
    public static final int                                         MAX_CACHE_INSTANCE_CAPACITY = Math.max(MathUtil.normalizeSize(SystemPropertyUtil.getInt("io.jnet.recycler.maxCacheInstanceCapacity", 0)), 32 * 1024);
    // 一个线程最多持有的延迟队列个数
    public static final int                                         MAX_DELAY_QUEUE_NUM         = Math.max(SystemPropertyUtil.getInt("io.jnet.recycler.maxDelayQueueNum", 0), 256);
    // Stack最多可以在延迟队列中存放的个数
    public static final int                                         MAX_SHARED_CAPACITY         = Math.max(SystemPropertyUtil.getInt("io.jnet.recycler.maxSharedCapacity", 0), MAX_CACHE_INSTANCE_CAPACITY);
    public static final int                                         LINK_SIZE                   = Math.max(SystemPropertyUtil.getInt("io.jnet.recycler.linSize", 0), 1024);
    final               FastThreadLocal<Map<Stack, WeakOrderQueue>> delayQueues                 = FastThreadLocal.withInitializeValue(() -> new WeakHashMap<>());
    final               FastThreadLocal<Stack>                      currentStack                = FastThreadLocal.withInitializeValue(() -> new Stack());
    final               long                                        LINK_NEXT_OFFSET            = UNSAFE.getFieldOffset("next", Link.class);
    private final       WeakOrderQueue                              DUMMY                       = new WeakOrderQueue();
    /////////////////////////////////
    private final       int                                         stackInitSize;
    private final       int                                         maxCachedInstanceCapacity;
    private final       int                                         linkSize;
    private final       int                                         maxDelayQueueNum;
    private final       int                                         maxSharedCapacity;
    private final       Supplier<T>                                 supplier;
    private final       BiConsumer<T, RecycleHandler>            biConsumer;

    public Recycler(Supplier<T> supplier, BiConsumer<T, RecycleHandler> biConsumer)
    {
        this(MAX_CACHE_INSTANCE_CAPACITY, MAX_DELAY_QUEUE_NUM, LINK_SIZE, MAX_SHARED_CAPACITY, supplier, biConsumer);
    }

    public Recycler(int maxCachedInstanceCapcity, int maxDelayQueueNum, int linkSize, int maxShadCapacity, Supplier<T> supplier, BiConsumer<T, RecycleHandler> biConsumer)
    {
        this.maxCachedInstanceCapacity = maxCachedInstanceCapcity;
        this.maxDelayQueueNum          = maxDelayQueueNum;
        this.linkSize                  = linkSize;
        this.maxSharedCapacity         = maxShadCapacity;
        this.supplier                  = supplier;
        this.biConsumer                = biConsumer;
        stackInitSize                  = Math.min(maxCachedInstanceCapcity, 2048);
    }

    /**
     * 归还空间
     *
     * @param space
     * @param sharedCapacity
     */
    static void reclaimSpace(int space, AtomicInteger sharedCapacity)
    {
        assert space >= 0;
        sharedCapacity.addAndGet(space);
    }

    /**
     * 申请空间
     *
     * @param space
     * @return
     */
    static boolean reserveSpace(int space, AtomicInteger availableSharedCapacity)
    {
        int now;
        do
        {
            now = availableSharedCapacity.get();
            if (now < space)
            {
                return false;
            }
        }
        while (availableSharedCapacity.compareAndSet(now, now - space) == false);
        return true;
    }
//    protected abstract T newObject(Function<T, RecycleHandler> function);

    @SuppressWarnings("unchecked")
    public T get()
    {
        Stack          stack = currentStack.get();
        DefaultHandler pop   = stack.pop();
        if (pop == null)
        {
            T              originStance   = supplier.get();
            DefaultHandler defaultHandler = new DefaultHandler(originStance, stack);
            biConsumer.accept(originStance, defaultHandler);
            return originStance;
        }
        else
        {
            return (T) pop.value;
        }
    }

    class Stack
    {
        WeakReference<Thread> ownerThread;
        RecycleHandler[]      buffer;
        volatile WeakOrderQueue head;
        WeakOrderQueue cursor;
        /**
         * 当前可以写入的位置
         */
        int            posi = 0;
        int            capacity;
        AtomicInteger  sharedCapacity;
        Lock lock = new ReentrantLock();

        public Stack()
        {
            capacity       = stackInitSize;
            sharedCapacity = new AtomicInteger(maxSharedCapacity);
            buffer         = new RecycleHandler[capacity];
            ownerThread    = new WeakReference<Thread>(Thread.currentThread());
        }

         void setHead(WeakOrderQueue queue)
        {
            lock.lock();
            if (head != null)
            {
                head.prev  = queue;
                queue.next = head;
            }
            head = queue;
            lock.unlock();
        }

         void removeHead()
        {
            lock.lock();
            WeakOrderQueue originHead = head;
            WeakOrderQueue next       = originHead.next;
            if (next == null)
            {
                head = null;
            }
            else
            {
                next.prev       = null;
                originHead.next = null;
                head            = next;
            }
            lock.unlock();
        }

        @SuppressWarnings("unchecked")
        DefaultHandler pop()
        {
            if (posi == 0)
            {
                transfer();
                if (posi == 0)
                {
                    return null;
                }
            }
            posi -= 1;
            DefaultHandler result = (DefaultHandler) buffer[posi];
            buffer[posi] = null;
            if (result.recyclerId != result.lastRecycleId)
            {
                throw new IllegalStateException("对象被回收了多次");
            }
            result.lastRecycleId = 0;
            result.recyclerId    = 0;
            return result;
        }

        /**
         * 尝试进行扩容。如果已经达到了容量上限，返回false不执行任何操作。
         *
         * @return
         */
        void extendCapacity()
        {
            if (capacity >= maxCachedInstanceCapacity)
            {
                throw new IllegalStateException();
            }
            capacity <<= 1;
            RecycleHandler[] array = new RecycleHandler[capacity];
            System.arraycopy(buffer, 0, array, 0, posi);
            buffer = array;
        }

        void transfer()
        {
            WeakOrderQueue anchor = cursor;
            do
            {
                if (cursor == null)
                {
                    cursor = head;
                    if (cursor == null || cursor == anchor)
                    {
                        return;
                    }
                    else
                    {
                        ;
                    }
                }
                if (cursor.moveToStack(this))
                {
                    return;
                }
                else if (cursor.ownerThread.get() == null)
                {
                    // 做最后一次数据迁移尝试
                    cursor.moveToStack(this);
                    cursor.returnResidueSpace();
                    if (cursor == head)
                    {
                        removeHead();
                        if (anchor == cursor)
                        {
                            anchor = null;
                        }
                        cursor = null;
                    }
                    else
                    {
                        WeakOrderQueue prev = cursor.prev;
                        WeakOrderQueue next = cursor.next;
                        prev.next = next;
                        if (next != null)
                        {
                            next.prev = prev;
                        }
                        cursor.next = cursor.prev = null;
                        if (anchor == cursor)
                        {
                            anchor = prev;
                        }
                        cursor = next;
                    }
                }
                else
                {
                    cursor = cursor.next;
                }
            }
            while (anchor != cursor);
        }

        void push(DefaultHandler handler)
        {
            Thread currentThread = Thread.currentThread();
            if (currentThread == ownerThread.get())
            {
                pushNow(handler);
            }
            else
            {
                pushLater(handler, currentThread);
            }
        }

        private void pushNow(DefaultHandler handler)
        {
            if (handler.lastRecycleId != 0 || handler.recyclerId != 0)
            {
                throw new IllegalStateException("多次回收，错误状态");
            }
            handler.lastRecycleId = handler.recyclerId = recyclerId;
            if (posi == capacity)
            {
                if (capacity == maxCachedInstanceCapacity)
                {
                    return;
                }
                extendCapacity();
            }
            buffer[posi] = handler;
                           posi += 1;
        }

        private void pushLater(DefaultHandler handler, Thread thread)
        {
            Map<Stack, WeakOrderQueue> map          = delayQueues.get();
            WeakOrderQueue             delayedQueue = map.get(this);
            if (delayedQueue == null)
            {
                if (map.size() >= maxDelayQueueNum)
                {
                    map.put(this, DUMMY);
                    return;
                }
                if (!reserveSpace(linkSize, sharedCapacity))
                {
                    return;
                }
                delayedQueue = new WeakOrderQueue(sharedCapacity, thread);
                setHead(delayedQueue);
                delayedQueue.add(handler);
                map.put(this, delayedQueue);
            }
            else if (delayedQueue == DUMMY)
            {
                return;
            }
            else
            {
                delayedQueue.add(handler);
            }
        }
    }

    class DefaultHandler implements RecycleHandler
    {
        /**
         * 当handler被所属的stack回收的时候，被赋值。赋值的情况有三：
         * 1、handler被所属的stack的线程回收，被赋予一个全局固定的非0值，此时与lastRecycled相等。
         * 2、handler被其他线程回收。在触发转移到所属stack的时候，被赋值lastRecycleId的值。
         * 3、缓存对象被提供出去使用时，赋值0.
         */
        int recyclerId;
        /**
         * 当handler触发回收的时候，该属性总是被赋值。可能被赋值的情况有三：
         * 1、handler被所属的stack的线程回收，则被赋予一个全局固定的非0值。
         * 2、handler被其他线程回收，则被赋予其他线程中与该stack关联的weakOrderQueue的id值。
         * 3、缓存对象被提供出去使用时，赋值0.
         */
        int lastRecycleId;
        final Object               value;
        final WeakReference<Stack> stackRef;

        public DefaultHandler(Object value, Stack stack)
        {
            this.value    = value;
            this.stackRef = new WeakReference<>(stack);
        }

        @Override
        public void recycle(Object value)
        {
            if (value != this.value)
            {
                throw new IllegalArgumentException("非法回收，回收对象不是之前申请出来的对象");
            }
            Stack stack = stackRef.get();
            if (stack != null)
            {
                stack.push(this);
            }
        }
    }

    class WeakOrderQueue
    {
        int                   id = IDGENERATOR.getAndIncrement();
        Link                  cursor;
        Link                  tail;
        AtomicInteger         sharedCapacity;
        WeakReference<Thread> ownerThread;
        WeakOrderQueue        next;
        WeakOrderQueue        prev;

        public WeakOrderQueue()
        {
            ;
        }

        public WeakOrderQueue(AtomicInteger sharedCapacity, Thread currentThread)
        {
            this.sharedCapacity = sharedCapacity;
            cursor              = tail = new Link();
            ownerThread         = new WeakReference<>(currentThread);
        }

        boolean add(DefaultHandler handler)
        {
            Link link  = tail;
            int  write = link.get();
            if (write == linkSize)
            {
                if (reserveSpace(linkSize, sharedCapacity))
                {
                    tail  = link = link.next = new Link();
                    write = 0;
                }
                else
                {
                    return false;
                }
            }
            handler.lastRecycleId = id;
            tail.put(handler, write);
            return true;
        }

        void returnResidueSpace()
        {
            // 在最后一次迁移尝试后，如果该Link的read没有处于终止位置（LinkSize），则意味着该Link的空间没有通过transfer方法内部归还。
            if (cursor.read != linkSize)
            {
                reclaimSpace(linkSize, sharedCapacity);
            }
        }

        /**
         * 尽可能的移动数据到Stack中。如果有数据被移动，返回true。否则返回false.<br/>
         * 转移过程中每消耗完一个Link，则将对应的容量归还到共享容量中。
         *
         * @param stack
         */
        @SuppressWarnings("unchecked")
        boolean moveToStack(Stack stack)
        {
            boolean success = false;
            do
            {
                if (cursor.read == linkSize)
                {
                    if (cursor.next == null)
                    {
                        return success;
                    }
                    cursor = cursor.next;
                }
                int srcLen = cursor.get() - cursor.read;
                if (srcLen == 0)
                {
                    return success;
                }
                int destLen = stack.capacity - stack.posi;
                if (destLen == 0)
                {
                    if (stack.capacity == maxCachedInstanceCapacity)
                    {
                        return success;
                    }
                    stack.extendCapacity();
                    destLen = stack.capacity - stack.posi;
                }
                int len = Math.min(srcLen, destLen);
                System.arraycopy(cursor.buffer, cursor.read, stack.buffer, stack.posi, len);
                RecycleHandler[] handlers = cursor.buffer;
                int              end      = cursor.read + len;
                for (int i = cursor.read; i < end; i++)
                {
                    DefaultHandler handler = ((DefaultHandler) handlers[i]);
                    if (handler.recyclerId == 0)
                    {
                        handler.recyclerId = handler.lastRecycleId;
                    }
                    else
                    {
                        throw new IllegalArgumentException();
                    }
                }
                cursor.read = end;
                stack.posi += len;
                success     = true;
                if (cursor.read == linkSize)
                {
                    reclaimSpace(linkSize, sharedCapacity);
                    cursor.destoryBuffer();
                    Link next = cursor.next;
                    if (next != null)
                    {
                        cursor.nullNext();
                        cursor = next;
                    }
                }
            }
            while (true);
        }
    }

    class Link extends AtomicInteger
    {
        private static final long serialVersionUID = -78580484990353021L;
        RecycleHandler[] buffer;
        volatile Link next;
        int read;

        public Link()
        {
            buffer = new RecycleHandler[linkSize];
        }

        @SuppressWarnings("unchecked")
        public void put(RecycleHandler handler, int write)
        {
            buffer[write] = handler;
//            ((DefaultHandler) handler).stackRef = null;
            lazySet(write + 1);
        }

        public boolean hasData()
        {
            return read != get();
        }

        void destoryBuffer()
        {
            buffer = null;
        }

        void nullNext()
        {
            UNSAFE.putObject(this, LINK_NEXT_OFFSET, this);
        }
    }
}
