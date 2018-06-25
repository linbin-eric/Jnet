package com.jfireframework.jnet.common.recycler;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import com.jfireframework.baseutil.reflect.ReflectUtil;
import com.jfireframework.baseutil.reflect.UnsafeFieldAccess;
import com.jfireframework.jnet.common.thread.FastThreadLocal;
import com.jfireframework.jnet.common.util.MathUtil;
import com.jfireframework.jnet.common.util.SystemPropertyUtil;
import sun.misc.Unsafe;

@SuppressWarnings("restriction")
public abstract class Recycler<T>
{
	// Stack最大可以存储的缓存对象个数
	public static final int				MAX_CACHE_INSTANCE_CAPACITY;
	// 一个线程最多持有的延迟队列个数
	public static final int				MAX_DELAY_QUEUE_NUM;
	// Stack最多可以在延迟队列中存放的个数
	public static final int				MAX_SHARED_CAPACITY;
	public static final int				LINK_SIZE;
	public static final AtomicInteger	IDGENERATOR	= new AtomicInteger(0);
	public static final int				recyclerId	= IDGENERATOR.getAndIncrement();
	static
	{
		int maxCacheInstanceCapacity = SystemPropertyUtil.getInt("io.jnet.recycler.maxCacheInstanceCapacity", 0);
		MAX_CACHE_INSTANCE_CAPACITY = Math.max(MathUtil.tableSizeFor(maxCacheInstanceCapacity), 32 * 1024);
		int maxDelayQueueNum = SystemPropertyUtil.getInt("io.jnet.recycler.maxDelayQueueNum", 0);
		MAX_DELAY_QUEUE_NUM = Math.max(maxDelayQueueNum, 32);
		int maxSharedCapacity = SystemPropertyUtil.getInt("io.jnet.recycler.maxSharedCapacity", 0);
		MAX_SHARED_CAPACITY = Math.max(maxSharedCapacity, MAX_CACHE_INSTANCE_CAPACITY >>> 1);
		int linkSize = SystemPropertyUtil.getInt("io.jnet.recycler.linSize", 0);
		LINK_SIZE = Math.max(linkSize, 32);
	}
	private final FastThreadLocal<Stack>								currentStack	= new FastThreadLocal<Stack>() {
																							protected Stack initializeValue()
																							{
																								return new Stack();
																							};
																						};
	private final static FastThreadLocal<Map<Stack, WeakOrderQueue>>	delayQueues		= new FastThreadLocal<Map<Stack, WeakOrderQueue>>() {
																							protected java.util.Map<Stack, WeakOrderQueue> initializeValue()
																							{
																								return new WeakHashMap<>();
																							};
																						};
	
	protected abstract T newObject(RecycleHandler handler);
	
	private static final WeakOrderQueue	DUMMY	= new WeakOrderQueue(null, null);
	private static final RecycleHandler	NO_OP	= new RecycleHandler() {
													
													@Override
													public void recycle(Object value)
													{
														
													}
													
												};
	
	@SuppressWarnings("unchecked")
	public T get()
	{
		if (MAX_CACHE_INSTANCE_CAPACITY == 0)
		{
			return newObject(NO_OP);
		}
		Stack stack = currentStack.get();
		DefaultHandler pop = stack.pop();
		if (pop == null)
		{
			pop = new DefaultHandler();
			pop.stack = stack;
			T instance = newObject(pop);
			pop.value = instance;
		}
		return (T) pop.value;
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
		int now = availableSharedCapacity.get();
		if (now < space)
		{
			return false;
		}
		boolean success = availableSharedCapacity.compareAndSet(now, now - space);
		if (success)
		{
			return true;
		}
		do
		{
			now = availableSharedCapacity.get();
			if (now < space)
			{
				return false;
			}
		} while (availableSharedCapacity.compareAndSet(now, now - space) == false);
		return true;
	}
	
	public static class Stack
	{
		WeakReference<Thread>	ownerThread;
		RecycleHandler[]		cacheInstances;
		volatile WeakOrderQueue	head;
		WeakOrderQueue			cursor;
		WeakOrderQueue			pred;
		int						index			= 0;
		int						capacity;
		AtomicInteger			sharedCapacity	= new AtomicInteger(MAX_SHARED_CAPACITY);
		
		public Stack()
		{
			capacity = 512;
			cacheInstances = new RecycleHandler[capacity];
			ownerThread = new WeakReference<Thread>(Thread.currentThread());
		}
		
		synchronized void setHead(WeakOrderQueue queue)
		{
			queue.next = head;
			head = queue;
		}
		
		synchronized void removeHead(WeakOrderQueue queue)
		{
			if (queue == head)
			{
				head = head.next;
			}
		}
		
		DefaultHandler pop()
		{
			int index = this.index;
			if (index == 0)
			{
				transfer();
				index = this.index;
				if (index == 0)
				{
					return null;
				}
			}
			index -= 1;
			DefaultHandler result = (DefaultHandler) cacheInstances[index];
			if (result.lastRecycleId != result.lastRecycleId)
			{
				throw new IllegalStateException("对象被回收了多次");
			}
			result.lastRecycleId = 0;
			result.recyclerId = 0;
			this.index = index;
			return result;
		}
		
		/**
		 * 尝试进行扩容。如果已经达到了容量上限，返回false不执行任何操作。
		 * 
		 * 
		 * @return
		 */
		boolean extendCapacity()
		{
			if (capacity >= MAX_CACHE_INSTANCE_CAPACITY)
			{
				return false;
			}
			capacity <<= 1;
			RecycleHandler[] array = new RecycleHandler[capacity];
			System.arraycopy(cacheInstances, 0, array, 0, index);
			cacheInstances = array;
			return true;
		}
		
		void transfer()
		{
			WeakOrderQueue cursor = this.cursor;
			WeakOrderQueue pred = this.pred;
			if (cursor == null)
			{
				pred = null;
				cursor = head;
				if (cursor == null)
				{
					return;
				}
			}
			if (cursor.transfer(this))
			{
				return;
			}
			else
			{
				// 线程已经消亡，将这个queue从队列中删除
				if (cursor.ownerThread.get() == null)
				{
					cursor.returnAllSpace();
					if (pred != null)
					{
						pred.next = cursor = cursor.next;
					}
					else
					{
						removeHead(cursor);
						cursor = cursor.next;
					}
				}
				else
				{
					pred = cursor;
					cursor = cursor.next;
				}
			}
			do
			{
				if (cursor == null)
				{
					pred = null;
					cursor = head;
					if (cursor == null)
					{
						break;
					}
				}
				if (cursor.transfer(this))
				{
					break;
				}
				else
				{
					// 线程已经消亡，将这个queue从队列中删除
					if (cursor.ownerThread.get() == null)
					{
						if (pred != null)
						{
							pred.next = cursor = cursor.next;
						}
						else
						{
							removeHead(cursor);
							cursor = cursor.next;
						}
					}
					else
					{
						pred = cursor;
						cursor = cursor.next;
					}
				}
			} while (true);
			this.pred = pred;
			this.cursor = cursor;
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
			int i = nextIndex();
			if (i == -1)
			{
				return;
			}
			cacheInstances[i] = handler;
		}
		
		private void pushLater(DefaultHandler handler, Thread thread)
		{
			Map<Stack, WeakOrderQueue> map = delayQueues.get();
			WeakOrderQueue delayedQueue = map.get(this);
			if (delayedQueue == null)
			{
				if (map.size() >= MAX_DELAY_QUEUE_NUM)
				{
					map.put(this, DUMMY);
					return;
				}
				if (reserveSpace(LINK_SIZE, sharedCapacity) == false)
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
		
		private int nextIndex()
		{
			int index = this.index;
			if (index == capacity)
			{
				if (capacity == MAX_CACHE_INSTANCE_CAPACITY)
				{
					return -1;
				}
				capacity <<= 1;
				cacheInstances = Arrays.copyOf(cacheInstances, capacity);
				this.index = index + 1;
				return index;
			}
			this.index = index + 1;
			return index;
		}
	}
	
	public static interface RecycleHandler
	{
		/**
		 * 对value对象进行缓存回收
		 * 
		 * @param value
		 */
		void recycle(Object value);
	}
	
	public static class DefaultHandler implements RecycleHandler
	{
		int		recyclerId;
		int		lastRecycleId;
		Object	value;
		Stack	stack;
		
		@Override
		public void recycle(Object value)
		{
			if (value != this.value)
			{
				throw new IllegalArgumentException("非法回收，回收对象不是之前申请出来的对象");
			}
			stack.push(this);
		}
		
		void setRecycleId(int recyclerId)
		{
			this.recyclerId = recyclerId;
		}
		
		void setLastRecyclerId(int lastRecycleId)
		{
			this.lastRecycleId = lastRecycleId;
		}
	}
	
	static class WeakOrderQueue
	{
		WeakOrderQueue			next;
		Link					tail;
		Link					cursor;
		AtomicInteger			sharedCapacity;
		WeakReference<Thread>	ownerThread;
		int						id	= IDGENERATOR.getAndIncrement();
		
		public WeakOrderQueue(AtomicInteger sharedCapacity, Thread thread)
		{
			this.sharedCapacity = sharedCapacity;
			cursor = tail = new Link();
			ownerThread = new WeakReference<Thread>(thread);
		}
		
		void add(DefaultHandler handler)
		{
			if (tail.isFull())
			{
				if (reserveSpace(LINK_SIZE, sharedCapacity))
				{
					tail = tail.next = new Link();
				}
				else
				{
					return;
				}
			}
			handler.lastRecycleId = id;
			tail.put(handler);
		}
		
		void returnAllSpace()
		{
			Link cursor = this.cursor;
			if (cursor.readIndex != LINK_SIZE)
			{
				reclaimSpace(LINK_SIZE, sharedCapacity);
			}
			while ((cursor = cursor.next) != null)
			{
				reclaimSpace(LINK_SIZE, sharedCapacity);
			}
		}
		
		/**
		 * 尽可能将当前的数据转移到Stack中
		 * 
		 * @param stack
		 */
		boolean transfer(Stack stack)
		{
			boolean success = false;
			do
			{
				int index = stack.index;
				int destSpace = stack.capacity - index;
				int srcSpace = cursor.avaliable();
				if (srcSpace == 0)
				{
					if (cursor.next == null)
					{
						return success;
					}
					else
					{
						cursor = cursor.next;
						srcSpace = cursor.avaliable();
					}
				}
				if (destSpace == 0)
				{
					if (stack.capacity == MAX_CACHE_INSTANCE_CAPACITY)
					{
						return success;
					}
					stack.extendCapacity();
					destSpace = stack.capacity - index;
				}
				int length = Math.min(destSpace, srcSpace);
				RecycleHandler[] dest = stack.cacheInstances;
				RecycleHandler[] src = cursor.store;
				int srcIndex = cursor.readIndex;
				for (int i = 0; i < length; i++)
				{
					DefaultHandler handler = (DefaultHandler) src[i + srcIndex];
					if (handler.recyclerId == 0)
					{
						handler.recyclerId = handler.lastRecycleId;
					}
					else
					{
						throw new IllegalStateException("回收了多次");
					}
				}
				System.arraycopy(src, srcIndex, dest, index, length);
				success = true;
				cursor.readIndex = srcIndex + length;
				int newIndex = index + length;
				stack.index = newIndex;
				if (cursor.readIndex == LINK_SIZE)
				{
					reclaimSpace(LINK_SIZE, sharedCapacity);
					if (cursor.next == null)
					{
						return success;
					}
					cursor = cursor.next;
				}
			} while (true);
		}
		
	}
	
	static class Link
	{
		RecycleHandler[]	store;
		volatile Link		next;
		int					readIndex;
		volatile int		writeIndex;
		static final long	WRITE_INDEX_ADDRESS	= UnsafeFieldAccess.getFieldOffset("writeIndex", Link.class);
		static final Unsafe	unsafe				= ReflectUtil.getUnsafe();
		
		public Link()
		{
			store = new RecycleHandler[LINK_SIZE];
		}
		
		public void putOrderedWriteIndex(int writeIndex)
		{
			unsafe.putOrderedInt(this, WRITE_INDEX_ADDRESS, writeIndex);
		}
		
		public int avaliable()
		{
			return unsafe.getIntVolatile(this, WRITE_INDEX_ADDRESS) - readIndex;
		}
		
		public boolean isFull()
		{
			return writeIndex == LINK_SIZE;
		}
		
		public void put(RecycleHandler handler)
		{
			int writeIndex = this.writeIndex;
			store[writeIndex] = handler;
			((DefaultHandler) handler).stack = null;
			putOrderedWriteIndex(writeIndex + 1);
		}
		
		public boolean hasData()
		{
			return readIndex != unsafe.getIntVolatile(this, WRITE_INDEX_ADDRESS);
		}
	}
}
