package com.jfireframework.jnet.common.buffer;

import com.jfireframework.jnet.common.recycler.Recycler;
import com.jfireframework.jnet.common.thread.FastThreadLocal;
import com.jfireframework.jnet.common.thread.FastThreadLocalThread;
import com.jfireframework.jnet.common.util.MathUtil;
import com.jfireframework.jnet.common.util.SystemPropertyUtil;

public class PooledBufferAllocator implements BufferAllocator
{
	public static final boolean					USE_CACHE_FOR_ALL_THREAD;
	public static final int						TINY_CACHE_SIZE;
	public static final int						SMALL_CACHE_SIZE;
	public static final int						NORMAL_CACHE_SIZE;
	public static final int						MAX_CACHEED_BUFFER_CAPACITY;
	public static final int						PAGESIZE;
	public static final int						PAGESIZE_SHIFT;
	public static final int						MAXLEVEL;
	public static final int						NUM_HEAP_ARENA;
	public static final int						NUM_DIRECT_ARENA;
	public static final PooledBufferAllocator	DEFAULT;
	static
	{
		USE_CACHE_FOR_ALL_THREAD = SystemPropertyUtil.getBoolean("io.jnet.PooledBufferAllocator.useCacheForAllThread", true);
		TINY_CACHE_SIZE = SystemPropertyUtil.getInt("io.jnet.PooledBufferAllocator.tinyCacheSize", 256);
		SMALL_CACHE_SIZE = SystemPropertyUtil.getInt("io.jnet.PooledBufferAllocator.smallCacheSize", 128);
		NORMAL_CACHE_SIZE = SystemPropertyUtil.getInt("io.jnet.PooledBufferAllocator.normalCacheSize", 64);
		PAGESIZE = SystemPropertyUtil.getInt("io.jnet.PooledBufferAllocator.pageSize", 8192);
		PAGESIZE_SHIFT = MathUtil.log2(PAGESIZE);
		MAXLEVEL = SystemPropertyUtil.getInt("io.jnet.PooledBufferAllocator.maxLevel", 11);
		NUM_HEAP_ARENA = SystemPropertyUtil.getInt("io.jnet.PooledBufferAllocator.numHeapArena", 16);
		NUM_DIRECT_ARENA = SystemPropertyUtil.getInt("io.jnet.PooledBufferAllocator.numDirectArena", 16);
		MAX_CACHEED_BUFFER_CAPACITY = SystemPropertyUtil.getInt("io.jnet.PooledBufferAllocator.maxCachedBufferCapacity", 32 * 1024);
		DEFAULT = new PooledBufferAllocator(PAGESIZE, MAXLEVEL, NUM_HEAP_ARENA, NUM_DIRECT_ARENA, MAX_CACHEED_BUFFER_CAPACITY, TINY_CACHE_SIZE, SMALL_CACHE_SIZE, NORMAL_CACHE_SIZE, USE_CACHE_FOR_ALL_THREAD);
	}
	boolean												useCacheForAllThread	= true;
	int													maxCachedBufferCapacity;
	int													tinyCacheSize;
	int													smallCacheSize;
	int													normalCacheSize;
	int													pagesize;
	int													pagesizeShift;
	int													maxLevel;
	HeapArena[]											heapArenas;
	DirectArena[]										directArenas;
	final FastThreadLocal<ThreadCache>					localCache				= new FastThreadLocal<ThreadCache>() {
																					@Override
																					protected ThreadCache initializeValue()
																					{
																						HeapArena leastUseHeapArena = (HeapArena) leastUseArena(heapArenas);
																						DirectArena leastUseDirectArena = (DirectArena) leastUseArena(directArenas);
																						leastUseHeapArena.numThreadCaches.incrementAndGet();
																						leastUseDirectArena.numThreadCaches.incrementAndGet();
																						Thread currentThread = Thread.currentThread();
																						if (useCacheForAllThread || currentThread instanceof FastThreadLocalThread)
																						{
																							ThreadCache cache = new ThreadCache(leastUseHeapArena, leastUseDirectArena, tinyCacheSize, smallCacheSize, normalCacheSize, maxCachedBufferCapacity, pagesizeShift);
																							return cache;
																						}
																						else
																						{
																							ThreadCache cache = new ThreadCache(leastUseHeapArena, leastUseDirectArena, 0, 0, 0, 0, pagesizeShift);
																							return cache;
																						}
																					};
																				};
	private static final Recycler<PooledDirectBuffer>	directBuffers			= new Recycler<PooledDirectBuffer>() {
																					
																					@Override
																					protected PooledDirectBuffer newObject(RecycleHandler handler)
																					{
																						PooledDirectBuffer buffer = new PooledDirectBuffer();
																						buffer.recycleHandler = handler;
																						return buffer;
																					}
																					
																				};
	private static final Recycler<PooledHeapBuffer>		heapBuffers				= new Recycler<PooledHeapBuffer>() {
																					
																					@Override
																					protected PooledHeapBuffer newObject(RecycleHandler handler)
																					{
																						PooledHeapBuffer buffer = new PooledHeapBuffer();
																						buffer.recycleHandler = handler;
																						return buffer;
																					}
																					
																				};
	
	public PooledBufferAllocator(int pagesize, int maxLevel, int numHeapArenas, int numDirectArenas, //
	        int maxCachedBufferCapacity, int tinyCacheSize, int smallCacheSize, int normalCacheSize, //
	        boolean useCacheForAllThread)
	{
		long t0 = System.currentTimeMillis();
		this.maxCachedBufferCapacity = maxCachedBufferCapacity;
		this.tinyCacheSize = tinyCacheSize;
		this.smallCacheSize = smallCacheSize;
		this.normalCacheSize = normalCacheSize;
		this.useCacheForAllThread = useCacheForAllThread;
		this.pagesize = pagesize;
		this.maxLevel = maxLevel;
		pagesizeShift = MathUtil.log2(pagesize);
		int subpageOverflowMask = ~(pagesize - 1);
		heapArenas = new HeapArena[numHeapArenas];
		for (int i = 0; i < numHeapArenas; i++)
		{
			heapArenas[i] = new HeapArena(this, maxLevel, pagesize, pagesizeShift, subpageOverflowMask);
		}
		directArenas = new DirectArena[numDirectArenas];
		for (int i = 0; i < numDirectArenas; i++)
		{
			directArenas[i] = new DirectArena(this, maxLevel, pagesize, pagesizeShift, subpageOverflowMask);
		}
	}
	
	private Arena<?> leastUseArena(Arena<?>[] arenas)
	{
		Arena<?> leastUseArena = arenas[0];
		for (Arena<?> each : arenas)
		{
			if (each.numThreadCaches.get() < leastUseArena.numThreadCaches.get())
			{
				leastUseArena = each;
			}
		}
		return leastUseArena;
	}
	
	public ThreadCache threadCache()
	{
		return localCache.get();
	}
	
	@Override
	public IoBuffer heapBuffer(int initializeCapacity)
	{
		ThreadCache threadCache = localCache.get();
		HeapArena heapArena = threadCache.heapArena;
		PooledHeapBuffer buffer = heapBuffers.get();
		heapArena.allocate(initializeCapacity, Integer.MAX_VALUE, buffer, threadCache);
		return buffer;
	}
	
	@Override
	public IoBuffer directBuffer(int initializeCapacity)
	{
		ThreadCache threadCache = localCache.get();
		DirectArena directArena = threadCache.directArena;
		PooledDirectBuffer buffer = directBuffers.get();
		directArena.allocate(initializeCapacity, Integer.MAX_VALUE, buffer, threadCache);
		return buffer;
	}
}
