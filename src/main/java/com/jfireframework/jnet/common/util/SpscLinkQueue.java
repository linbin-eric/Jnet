package com.jfireframework.jnet.common.util;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.Queue;

public class SpscLinkQueue<E> extends AbstractQueue<E> implements Queue<E>
{
    Node producer;
    Node consumer;

    public SpscLinkQueue()
    {
        producer = consumer = new Node();
    }

    class Node
    {
        volatile Node next;
        E value;
    }

    @Override
    public Iterator iterator()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty()
    {
        return consumer.next == null;
    }

    @Override
    public int size()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean offer(Object o)
    {
        Node node = new Node();
        node.value = (E) o;
        producer.next = node;
        producer = node;
        return true;
    }

    @Override
    public E poll()
    {
        Node next = consumer.next;
        if (next == null)
        {
            return null;
        }
        else
        {
            E result = next.value;
            consumer = next;
            return result;
        }
    }

    @Override
    public E peek()
    {
        Node next = consumer.next;
        return next == null ? null : next.value;
    }
}
