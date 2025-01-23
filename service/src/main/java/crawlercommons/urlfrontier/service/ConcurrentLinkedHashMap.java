// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Concurrent version of LinkedHashMap Design goal is the same as for ConcurrentHashMap: Maintain
 * concurrent readability (typically method get(), but also iterators and related methods) while
 * minimizing update contention
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class ConcurrentLinkedHashMap<K, V> extends AbstractMap<K, V>
        implements ConcurrentMap<K, V> {

    private final ConcurrentHashMap<K, V> map;
    private final ConcurrentLinkedQueue<K> order;
    private final ReentrantLock lock; // To maintain consistency between map and order
    private final AtomicInteger size;

    public ConcurrentLinkedHashMap() {
        this.map = new ConcurrentHashMap<>();
        this.order = new ConcurrentLinkedQueue<>();
        this.lock = new ReentrantLock();
        this.size = new AtomicInteger(0);
    }

    @Override
    public int size() {
        return size.get();
    }

    @Override
    public boolean isEmpty() {
        return size.get() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return map.get(key);
    }

    /**
     * Associates the specified value with the specified key in this map. Maintains insertion order
     * if the key is new.
     */
    @Override
    public V put(K key, V value) {
        V previous;
        lock.lock();
        try {
            previous = map.put(key, value);
            if (previous == null) {
                order.add(key);
                size.incrementAndGet();
            }
        } finally {
            lock.unlock();
        }
        return previous;
    }

    /**
     * Removes the mapping for a key from this map if it is present. Also removes the key from the
     * insertion order queue.
     */
    @Override
    public V remove(Object key) {
        V removedValue;
        lock.lock();
        try {
            removedValue = map.remove(key);
            if (removedValue != null) {
                order.remove(key);
                size.decrementAndGet();
            }
        } finally {
            lock.unlock();
        }
        return removedValue;
    }

    /**
     * Copies all of the mappings from the specified map to this map. Maintains insertion order
     * based on the iteration order of the specified map.
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        lock.lock();
        try {
            for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
                K key = entry.getKey();
                V value = entry.getValue();
                if (map.put(key, value) == null) {
                    order.add(key);
                    size.incrementAndGet();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /** Removes all of the mappings from this map and clears the insertion order. */
    @Override
    public void clear() {
        lock.lock();
        try {
            map.clear();
            order.clear();
            size.set(0);
        } finally {
            lock.unlock();
        }
    }

    /** Returns a Set view of the keys contained in this map in insertion order. */
    @Override
    public Set<K> keySet() {
        return new LinkedHashSet<>(order);
    }

    /** Returns a Collection view of the values contained in this map in insertion order. */
    @Override
    public Collection<V> values() {
        List<V> values = new ArrayList<>();
        for (K key : order) {
            V value = map.get(key);
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    /** Returns a Set view of the mappings contained in this map in insertion order. */
    @Override
    public Set<Entry<K, V>> entrySet() {
        LinkedHashSet<Entry<K, V>> entries = new LinkedHashSet<>();
        for (K key : order) {
            V value = map.get(key);
            if (value != null) {
                entries.add(new AbstractMap.SimpleImmutableEntry<>(key, value));
            }
        }
        return entries;
    }

    @Override
    public V putIfAbsent(K key, V value) {

        lock.lock();
        try {
            if (!map.containsKey(key)) return put(key, value);
            else return map.get(key);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean remove(Object key, Object value) {

        lock.lock();
        try {
            if (map.containsKey(key) && Objects.equals(map.get(key), value)) {
                remove(key);
                return true;
            } else {
                return false;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {

        lock.lock();
        try {
            if (map.containsKey(key) && Objects.equals(map.get(key), oldValue)) {
                put(key, newValue);
                return true;
            } else {
                return false;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public V replace(K key, V value) {

        lock.lock();
        try {
            if (map.containsKey(key)) return put(key, value);
            else return null;
        } finally {
            lock.unlock();
        }
    }
}
