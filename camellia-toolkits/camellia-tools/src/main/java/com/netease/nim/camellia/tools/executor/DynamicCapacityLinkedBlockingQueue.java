package com.netease.nim.camellia.tools.executor;

import com.netease.nim.camellia.tools.base.DynamicValueGetter;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2023/1/3
 */
public class DynamicCapacityLinkedBlockingQueue<T> implements BlockingQueue<T> {

    private final LinkedBlockingQueue<T> queue;
    private final DynamicValueGetter<Integer> capacity;

    public DynamicCapacityLinkedBlockingQueue(DynamicValueGetter<Integer> capacity) {
        this.capacity = capacity;
        this.queue = new LinkedBlockingQueue<>();
    }

    @Override
    public boolean add(T t) {
        if (capacity.get() == Integer.MAX_VALUE) {
            return queue.add(t);
        }
        if (queue.size() >= capacity.get()) {
            throw new IllegalStateException("Queue full");
        }
        return queue.add(t);
    }

    @Override
    public boolean offer(T t) {
        if (capacity.get() == Integer.MAX_VALUE) {
            return queue.offer(t);
        }
        if (queue.size() >= capacity.get()) {
            return false;
        }
        return queue.offer(t);
    }

    @Override
    public T remove() {
        return queue.remove();
    }

    @Override
    public T poll() {
        return queue.poll();
    }

    @Override
    public T element() {
        return queue.element();
    }

    @Override
    public T peek() {
        return queue.peek();
    }

    @Override
    public void put(T t) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean offer(T t, long timeout, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public T take() throws InterruptedException {
        return queue.take();
    }

    @Override
    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        return queue.poll(timeout, unit);
    }

    @Override
    public int remainingCapacity() {
        return queue.remainingCapacity();
    }

    @Override
    public boolean remove(Object o) {
        return queue.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return queue.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        if (capacity.get() == Integer.MAX_VALUE) {
            return queue.addAll(c);
        }
        if (queue.size() + c.size() >= capacity.get()) {
            return false;
        }
        return queue.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return queue.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return queue.retainAll(c);
    }

    @Override
    public void clear() {
        queue.clear();
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return queue.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return queue.iterator();
    }

    @Override
    public Object[] toArray() {
        return queue.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return queue.toArray(a);
    }

    @Override
    public int drainTo(Collection<? super T> c) {
        return queue.drainTo(c);
    }

    @Override
    public int drainTo(Collection<? super T> c, int maxElements) {
        return queue.drainTo(c, maxElements);
    }
}
