package cc.jfire.jnet.common.internal;

import cc.jfire.jnet.common.api.PipelineInitializer;
import cc.jfire.jnet.common.util.ChannelConfig;

import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel>
{
    protected final ChannelConfig       channelConfig;
    protected final PipelineInitializer pipelineInitializer;

    public AcceptHandler(ChannelConfig channelConfig, PipelineInitializer pipelineInitializer)
    {
        this.channelConfig       = channelConfig;
        this.pipelineInitializer = pipelineInitializer;
    }

    @Override
    public void completed(AsynchronousSocketChannel socketChannel, AsynchronousServerSocketChannel serverChannel)
    {
        serverChannel.accept(serverChannel, this);
        Thread.startVirtualThread(() -> {
            try
            {
                socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
            }
            catch (Exception e)
            {
            }
            DefaultPipeline pipeline = new DefaultPipeline(socketChannel, channelConfig);
            pipelineInitializer.onPipelineComplete(pipeline);
            pipeline.complete();
        });
    }

    @Override
    public void failed(Throwable exc, AsynchronousServerSocketChannel serverChannel)
    {
        try
        {
            serverChannel.close();
        }
        catch (Throwable e)
        {
            ;
        }
    }
}
