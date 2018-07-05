package com.jfireframework.jnet.common.buffer;

import java.nio.ByteBuffer;
import com.jfireframework.baseutil.reflect.ReflectUtil;
import com.jfireframework.jnet.common.util.MathUtil;

public class ThreadCache
{
	final HeapArena							heapArena;
	final MemoryRegionCache<byte[]>[]		tinySubPagesHeapCaches;
	final MemoryRegionCache<byte[]>[]		smallSubPageHeapCaches;
	final MemoryRegionCache<byte[]>[]		normalHeapCaches;
	final DirectArena						directArena;
	final MemoryRegionCache<ByteBuffer>[]	tinySubPagesDirectCaches;
	final MemoryRegionCache<ByteBuffer>[]	smallSubPagesDirectCaches;
	final MemoryRegionCache<ByteBuffer>[]	normalDirectCaches;
	final int								pageSizeShift;
	
	@SuppressWarnings("unchecked")
	public ThreadCache(HeapArena heapArena, DirectArena directArena, int tinyCacheSize, int smallCacheSize, int normalCacheSize, int maxCachedBufferCapacity, int pageSizeShift)
	{
		this.pageSizeShift = pageSizeShift;
		this.heapArena = heapArena;
		tinySubPagesHeapCaches = new MemoryRegionCache[heapArena.tinySubPages.length];
		for (int i = 0; i < tinySubPagesHeapCaches.length; i++)
		{
			tinySubPagesHeapCaches[i] = new MemoryRegionCache<>(tinyCacheSize);
		}
		smallSubPageHeapCaches = new MemoryRegionCache[heapArena.smallSubPages.length];
		for (int i = 0; i < smallSubPageHeapCaches.length; i++)
		{
			smallSubPageHeapCaches[i] = new MemoryRegionCache<>(smallCacheSize);
		}
		int normalizeSize = MathUtil.normalizeSize(maxCachedBufferCapacity);
		int shift = MathUtil.log2(normalizeSize);
		normalHeapCaches = new MemoryRegionCache[shift - pageSizeShift];
		for (int i = 0; i < normalHeapCaches.length; i++)
		{
			normalHeapCaches[i] = new MemoryRegionCache<>(normalCacheSize);
		}
		this.directArena = directArena;
		tinySubPagesDirectCaches = new MemoryRegionCache[directArena.tinySubPages.length];
		for (int i = 0; i < tinySubPagesDirectCaches.length; i++)
		{
			tinySubPagesDirectCaches[i] = new MemoryRegionCache<>(tinyCacheSize);
		}
		smallSubPagesDirectCaches = new MemoryRegionCache[directArena.smallSubPages.length];
		for (int i = 0; i < smallSubPagesDirectCaches.length; i++)
		{
			smallSubPagesDirectCaches[i] = new MemoryRegionCache<>(smallCacheSize);
		}
		normalDirectCaches = new MemoryRegionCache[shift - pageSizeShift];
		for (int i = 0; i < normalDirectCaches.length; i++)
		{
			normalDirectCaches[i] = new MemoryRegionCache<>(normalCacheSize);
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean allocate(PooledBuffer<?> buffer, int normalizeCapacity, SizeType sizeType, Arena<?> arena)
	{
		MemoryRegionCache<?> cache = findCache(normalizeCapacity, sizeType, arena);
		if (cache == null)
		{
			return false;
		}
		return ((MemoryRegionCache) cache).tryFindAndInitBuffer(buffer, this);
		
	}
	
	private MemoryRegionCache<?> findCache(int normalizeCapacity, SizeType sizeType, Arena<?> arena)
	{
		MemoryRegionCache<?> cache = null;
		switch (sizeType)
		{
			case TINY:
				cache = cacheForTiny(normalizeCapacity, arena);
				break;
			case SMALL:
				cache = cacheForSmall(normalizeCapacity, arena);
				break;
			case NORMAL:
				cache = cacheForNormal(normalizeCapacity, arena);
				break;
			default:
				ReflectUtil.throwException(new NullPointerException());
		}
		return cache;
	}
	
	MemoryRegionCache<?> cacheForTiny(int normalizeCapacity, Arena<?> arena)
	{
		int tinyIdx = Arena.tinyIdx(normalizeCapacity);
		if (arena.isDirect())
		{
			if (tinySubPagesDirectCaches == null || tinySubPagesDirectCaches.length <= tinyIdx)
			{
				return null;
			}
			return tinySubPagesDirectCaches[tinyIdx];
		}
		else
		{
			if (tinySubPagesHeapCaches == null || tinySubPagesHeapCaches.length <= tinyIdx)
			{
				return null;
			}
			return tinySubPagesHeapCaches[tinyIdx];
		}
	}
	
	MemoryRegionCache<?> cacheForSmall(int normalizeCapacity, Arena<?> arena)
	{
		int smallIdx = Arena.smallIdx(normalizeCapacity);
		if (arena.isDirect())
		{
			if (smallSubPagesDirectCaches == null || smallSubPageHeapCaches.length <= smallIdx)
			{
				return null;
			}
			return smallSubPagesDirectCaches[smallIdx];
		}
		else
		{
			if (smallSubPageHeapCaches == null || smallSubPageHeapCaches.length <= smallIdx)
			{
				return null;
			}
			return smallSubPageHeapCaches[smallIdx];
		}
	}
	
	MemoryRegionCache<?> cacheForNormal(int normalizeCapacity, Arena<?> arena)
	{
		int shift = MathUtil.log2(normalizeCapacity);
		int idx = shift - pageSizeShift;
		if (arena.isDirect())
		{
			if (normalDirectCaches == null || normalDirectCaches.length <= idx)
			{
				return null;
			}
			return normalDirectCaches[idx];
		}
		else
		{
			if (normalHeapCaches == null || normalHeapCaches.length <= idx)
			{
				return null;
			}
			return normalHeapCaches[idx];
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean add(int normalizeCapacity, SizeType sizeType, Arena<?> arena, Chunk<?> chunk, long handle)
	{
		MemoryRegionCache<?> cache = findCache(normalizeCapacity, sizeType, arena);
		if (cache == null)
		{
			return false;
		}
		return ((MemoryRegionCache) cache).offer(chunk, handle);
	}
}
