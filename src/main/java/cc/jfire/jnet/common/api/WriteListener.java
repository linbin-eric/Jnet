package cc.jfire.jnet.common.api;

public interface WriteListener
{
    WriteListener INSTANCE = new WriteListener()
    {
        @Override
        public void partWriteFinish(long currentSend)
        {
        }

        @Override
        public void queuedWrite(long size)
        {
        }

        @Override
        public void writeFailed(Throwable e)
        {
        }
    };

    /**
     * @param currentSend 本次已经写出的容量
     */
     void partWriteFinish(long currentSend) ;

    /**
     * 当前入队的待写出大小
     *
     * @param size
     */
     void queuedWrite(long size) ;

     void writeFailed(Throwable e);
}
