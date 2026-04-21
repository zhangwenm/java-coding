package com.geekzhang.algorithms.niuke;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * LFU (Least Frequently Used) Cache 实现
 * LeetCode 460: https://leetcode.cn/problems/lfu-cache/
 *
 * 【LFU vs LRU 核心差异】
 * - LRU 淘汰最久未使用的 key，只看「时间」一个维度
 * - LFU 淘汰使用频率最低的 key；频率相同时，才按 LRU 淘汰最久未访问的
 * - 因此 LFU 需要额外维护「频率」维度，数据结构比 LRU 更复杂
 *
 * 【O(1) 实现思路】
 * - keyToVal:   key → value，O(1) 查值
 * - keyToFreq:  key → freq，O(1) 查频率
 * - freqToKeys: freq → LinkedHashSet<key>，同一频率内按访问顺序排列（LRU）
 * - minFreq:    当前最小频率指针，O(1) 找到淘汰目标
 *
 * 每次 get/put 访问一个 key 时，将其频率 +1，并从旧频率集合移到新频率集合。
 * 淘汰时直接从 minFreq 对应的集合中移除第一个（最久未访问的）。
 *
 * @author zwm
 */
public class LfuCache {

    /** key → value 映射 */
    private final Map<Integer, Integer> keyToVal;

    /** key → freq 映射 */
    private final Map<Integer, Integer> keyToFreq;

    /** freq → 按访问顺序排列的 key 集合（LinkedHashSet 保证插入顺序即访问顺序） */
    private final Map<Integer, LinkedHashSet<Integer>> freqToKeys;

    /** 当前最小频率，用于 O(1) 定位淘汰候选 */
    private int minFreq;

    /** 缓存容量 */
    private final int capacity;

    public LfuCache(int capacity) {
        this.capacity = capacity;
        this.minFreq = 0;
        this.keyToVal = new HashMap<>();
        this.keyToFreq = new HashMap<>();
        this.freqToKeys = new HashMap<>();
    }

    /**
     * 获取 key 对应的值，同时增加该 key 的访问频率
     *
     * @param key 缓存键
     * @return 对应的值，不存在返回 -1
     */
    public int get(int key) {
        if (!keyToVal.containsKey(key)) {
            return -1;
        }
        // 增加频率
        increaseFreq(key);
        return keyToVal.get(key);
    }

    /**
     * 插入/更新键值对
     * - key 已存在：更新值，增加频率
     * - key 不存在：如果容量已满则先淘汰，再插入新 key（频率初始为 1）
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    public void put(int key, int value) {
        if (capacity <= 0) {
            return;
        }

        // key 已存在：更新值并增加频率
        if (keyToVal.containsKey(key)) {
            keyToVal.put(key, value);
            increaseFreq(key);
            return;
        }

        // 容量已满：淘汰最小频率中最久未使用的 key
        if (keyToVal.size() >= capacity) {
            evictMinFreqKey();
        }

        // 插入新 key
        keyToVal.put(key, value);
        keyToFreq.put(key, 1);
        freqToKeys.computeIfAbsent(1, k -> new LinkedHashSet<>()).add(key);
        // 新插入的 key 频率为 1，必然是最小频率
        this.minFreq = 1;
    }

    /**
     * 增加 key 的访问频率：
     * 1. 从旧频率集合中移除
     * 2. 加入新频率集合（freq + 1）
     * 3. 如果旧频率集合为空且恰好是 minFreq，则 minFreq++
     */
    private void increaseFreq(int key) {
        int freq = keyToFreq.get(key);
        // 更新频率映射
        keyToFreq.put(key, freq + 1);

        // 从旧频率集合中移除
        LinkedHashSet<Integer> oldSet = freqToKeys.get(freq);
        oldSet.remove(key);
        // 如果旧频率集合空了，清理并可能更新 minFreq
        if (oldSet.isEmpty()) {
            freqToKeys.remove(freq);
            // 如果移除的是最小频率，minFreq 必须递增
            // （因为同一个 key 频率+1后必然在新频率集合中）
            if (freq == this.minFreq) {
                this.minFreq++;
            }
        }

        // 加入新频率集合
        freqToKeys.computeIfAbsent(freq + 1, k -> new LinkedHashSet<>()).add(key);
    }

    /**
     * 淘汰策略：移除 minFreq 对应集合中最久未访问的 key（即第一个元素）
     */
    private void evictMinFreqKey() {
        LinkedHashSet<Integer> minFreqSet = freqToKeys.get(this.minFreq);
        // LinkedHashSet 的迭代器按插入顺序遍历，第一个就是最久未访问的
        int evictKey = minFreqSet.iterator().next();
        minFreqSet.remove(evictKey);
        if (minFreqSet.isEmpty()) {
            freqToKeys.remove(this.minFreq);
            // minFreq 会在新插入时重置为 1，此处不需要更新
        }
        keyToVal.remove(evictKey);
        keyToFreq.remove(evictKey);
    }
}
