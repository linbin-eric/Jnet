package com.jfireframework.jnet.common.recycler;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import com.jfireframework.baseutil.reflect.UNSAFE;
import com.jfireframework.jnet.common.thread.FastThreadLocal;
import com.jfireframework.jnet.common.util.MathUtil;
import com.jfireframework.jnet.common.util.SystemPropertyUtil;

public abstract class Recycler<T>
{
	public static final AtomicInteger					IDGENERATOR					= new AtomicInteger(0);
	public static final int								recyclerId					= IDGENERATOR.getAndIncrement();
	// Stack最大可以存储的缓存对象个数
	public static final int								MAX_CACHE_INSTANCE_CAPACITY	= Math.max(MathUtil.normalizeSize(SystemPropertyUtil.getInt("io.jnet.recycler.maxCacheInstanceCapacity", 0)), 32 * 1024);
	// 一个线程最多持有的延迟队列个数
	public static final int								MAX_DELAY_QUEUE_NUM			= Math.max(SystemPropertyUtil.getInt("io.jnet.recycler.maxDelayQueueNum", 0), 256);
	// Stack最多可以在延迟队列中存放的个数
	public static final int								MAX_SHARED_CAPACITY			= Math.max(SystemPropertyUtil.getInt("io.jnet.recycler.maxSharedCapacity", 0), MAX_CACHE_INSTANCE_CAPACITY);
	public static final int								LINK_SIZE					= Math.max(SystemPropertyUtil.getInt("io.jnet.recycler.linSize", 0), 1024);
	/////////////////////////////////
	private int											maxCachedInstanceCapacity;
	private int											stackInitSize				= 2048;
	private int											maxDelayQueueNum;
	private int											linkSize;
	private int											maxSharedCapacity;
	
	final FastThreadLocal<Map<Stack, WeakOrderQueue>>	delayQueues					= new FastThreadLocal<Map<Stack, WeakOrderQueue>>() {
																						protected java.util.Map<Stack, WeakOrderQueue> initializeValue()
																						{
																							return new WeakHashMap<>();
																						};
																					};
	final FastThreadLocal<Stack>						currentStack				= new FastThreadLocal<Stack>() {
																						protected Stack initializeValue()
																						{
																							return new Stack();
																						};
																					};
	
	protected abstract T newObject(RecycleHandler handler);
	
	private final WeakOrderQueue	DUMMY	= new WeakOrderQueue();
	private final RecycleHandler	NO_OP	= new RecycleHandler() {
												
												@Override
												public void recycle(Object value)
												{
													
												}
												
											};
	
	public Recycler()
	{
		maxCachedInstanceCapacity = MAX_CACHE_INSTANCE_CAPACITY;
		maxDelayQueueNum = MAX_DELAY_QUEUE_NUM;
		linkSize = LINK_SIZE;
		maxSharedCapacity = MAX_SHARED_CAPACITY;
	}
	
	public Recycler(int maxCachedInstanceCapcity, int maxDelayQueueNum, int linkSize, int maxShadCapacity)
	{
		this.maxCachedInstanceCapacity = maxCachedInstanceCapcity;
		this.maxDelayQueueNum = maxDelayQueueNum;
		this.linkSize = linkSize;
		this.maxSharedCapacity = maxShadCapacity;
	}
	
	@SuppressWarnings("unchecked")
	public T get()
	{
		if (maxCachedInstanceCapacity == 0)
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
		int now;
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
	
	class Stack
	{
		WeakReference<Thread>	ownerThread;
		RecycleHandler[]		buffer;
		volatile WeakOrderQueue	head;
		WeakOrderQueue			cursor;
		WeakOrderQueue			prev;
		/**
		 * 当前可以写入的位置
		 */
		int						posi	= 0;
		int						capacity;
		AtomicInteger			sharedCapacity;
		
		public Stack()
		{
			capacity = stackInitSize;
			sharedCapacity = new AtomicInteger(maxSharedCapacity);
			buffer = new RecycleHandler[capacity];
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
				head = queue.next;
				queue.next = queue;
			}
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
			if (result.lastRecycleId != result.lastRecycleId)
			{
				throw new IllegalStateException("对象被回收了多次");
			}
			result.lastRecycleId = 0;
			result.recyclerId = 0;
			return result;
		}
		
		/**
		 * 尝试进行扩容。如果已经达到了容量上限，返回false不执行任何操作。
		 * 
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
					if (cursor == null)
					{
						return;
					}
					else
					{
						prev = null;
						anchor = null;
					}
				}
				if (cursor.transfer(this))
				{
					return;
				}
				else if (cursor.ownerThread.get() == null)
				{
					// 做最后一次数据迁移尝试
					cursor.transfer(this);
					cursor.returnResidueSpace();
					if (prev == null)
					{
						removeHead(cursor);
						cursor = null;
					}
					else
					{
						prev.next = cursor.next;
						if (anchor == cursor)
						{
							anchor = cursor.next;
						}
						cursor = cursor.next;
					}
					continue;
				}
				else
				{
					cursor = cursor.next;
				}
			} while (anchor != cursor);
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
			Map<Stack, WeakOrderQueue> map = delayQueues.get();
			WeakOrderQueue delayedQueue = map.get(this);
			if (delayedQueue == null)
			{
				if (map.size() >= maxDelayQueueNum)
				{
					map.put(this, DUMMY);
					return;
				}
				if (reserveSpace(linkSize, sharedCapacity) == false)
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
		
	}
	
	class WeakOrderQueue
	{
		int						id	= IDGENERATOR.getAndIncrement();
		Link					cursor;
		Link					tail;
		AtomicInteger			sharedCapacity;
		WeakReference<Thread>	ownerThread;
		WeakOrderQueue			next;
		
		public WeakOrderQueue()
		{
			linkSize = 0;
			maxCachedInstanceCapacity = 0;
		}
		
		public WeakOrderQueue(AtomicInteger sharedCapacity, Thread currentThread)
		{
			this.sharedCapacity = sharedCapacity;
			cursor = tail = new Link();
			ownerThread = new WeakReference<Thread>(currentThread);
		}
		
		boolean add(DefaultHandler handler)
		{
			Link link = tail;
			int write = link.get();
			if (write == linkSize)
			{
				if (reserveSpace(linkSize, sharedCapacity))
				{
					tail = link = link.next = new Link();
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
			// 在最后一次迁移尝试后，如果该Link的read没有处于终止位置（LinkSize），则意味着该Link的空间尚未归还
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
		boolean transfer(Stack stack)
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
				int end = cursor.read + len;
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
				success = true;
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
			} while (true);
		}
		
	}
	
	final long LINK_NEXT_OFFSET = UNSAFE.getFieldOffset("next", Link.class);
	
	class Link extends AtomicInteger
	{
		private static final long	serialVersionUID	= -78580484990353021L;
		RecycleHandler[]			buffer;
		volatile Link				next;
		int							read;
		
		public Link()
		{
			buffer = new RecycleHandler[LINK_SIZE];
		}
		
		@SuppressWarnings("unchecked")
		public void put(RecycleHandler handler, int write)
		{
			buffer[write] = handler;
			((DefaultHandler) handler).stack = null;
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
