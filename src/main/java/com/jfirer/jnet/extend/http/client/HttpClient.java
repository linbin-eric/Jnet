package com.jfirer.jnet.extend.http.client;

import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator;

public interface HttpClient
{
    BufferAllocator ALLOCATOR = new PooledBufferAllocator("HttpClient");

    HttpReceiveResponse newCall(HttpSendRequest request) throws Exception;

}
