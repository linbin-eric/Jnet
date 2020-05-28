package com.jfireframework.jnet.common.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

class LinkedQueueNode
{
    volatile LinkedQueueNode next;
    final    Object          value;

    LinkedQueueNode(Object value)
    {
        this.value = value;
    }

    LinkedQueueNode()
    {
        value = null;
    }
}

abstract class Pad0
{
    long p00, p01, p02, p03, p04, p05, p06, p07;
    long p10, p11, p12, p13, p14, p15, p16;
}

abstract class ProduceNode extends Pad0
{
    LinkedQueueNode producerNode;
    static final long P_OFFSET = UNSAFE.getFieldOffset("producerNode", ProduceNode.class);
}

abstract class Pad1 extends ProduceNode
{
    long p01, p02, p03, p04, p05, p06, p07;
    long p10, p11, p12, p13, p14, p15, p16, p17;
}

abstract class ConsumerNode extends Pad1
{
    LinkedQueueNode consumerNode;
    static final long C_OFFSET = UNSAFE.getFieldOffset("consumerNode", ConsumerNode.class);
}

abstract class BaseLinkQueue extends ConsumerNode
{
    long p01, p02, p03, p04, p05, p06, p07;
    long p10, p11, p12, p13, p14, p15, p16, p17;
}

public class SpscLinkedQueue<E> extends BaseLinkQueue implements Queue<E>
{
    public SpscLinkedQueue()
    {
        consumerNode = producerNode = new LinkedQueueNode();
    }

    @Override
    public int size()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty()
    {
        return consumerNode.next == null;
    }

    @Override
    public boolean contains(Object o)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<E> iterator()
    {

        throw new UnsupportedOperationException();

    }

    @Override
    public Object[] toArray()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T[] toArray(T[] a)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(E e)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends E> c)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean offer(E e)
    {
        LinkedQueueNode node = new LinkedQueueNode(e);
        producerNode.next = node;
        producerNode = node;
        return true;
    }

    @Override
    public E remove()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public E poll()
    {
        LinkedQueueNode next = consumerNode.next;
        if (next != null)
        {
            E e = (E) next.value;
            consumerNode.next = consumerNode;
            consumerNode = next;
            return e;
        }
        else
        {
            return null;
        }
    }

    @Override
    public E element()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public E peek()
    {
        LinkedQueueNode next = consumerNode.next;
        return next == null ? null : (E) next.value;
    }
}
