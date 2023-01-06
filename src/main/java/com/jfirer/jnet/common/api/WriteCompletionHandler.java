package com.jfirer.jnet.common.api;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

public interface WriteCompletionHandler extends CompletionHandler<Integer, ByteBuffer>, WriteProcessor
{}
