package com.geekzhang.algorithms.niuke;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * LfuCache 单元测试
 * 覆盖：基本操作、淘汰策略、频率相同按 LRU、边界、频率增长、更新已存在 key
 */
class LfuCacheTest {

    @Nested
    @DisplayName("基本操作：put 后 get 返回正确值")
    class BasicOperations {

        @Test
        @DisplayName("put 后可以 get 到值")
        void putAndGet() {
            LfuCache cache = new LfuCache(2);
            cache.put(1, 10);
            assertEquals(10, cache.get(1));
        }

        @Test
        @DisplayName("get 不存在的 key 返回 -1")
        void getNonExistent() {
            LfuCache cache = new LfuCache(2);
            assertEquals(-1, cache.get(999));
        }

        @Test
        @DisplayName("多个 key 正常存取")
        void multipleKeys() {
            LfuCache cache = new LfuCache(3);
            cache.put(1, 100);
            cache.put(2, 200);
            cache.put(3, 300);
            assertEquals(100, cache.get(1));
            assertEquals(200, cache.get(2));
            assertEquals(300, cache.get(3));
        }
    }

    @Nested
    @DisplayName("淘汰策略：容量满时淘汰最低频率")
    class EvictionPolicy {

        @Test
        @DisplayName("淘汰使用频率最低的 key")
        void evictLowestFreq() {
            LfuCache cache = new LfuCache(2);
            cache.put(1, 10);
            cache.put(2, 20);
            // key=1 访问 2 次，key=2 访问 1 次
            cache.get(1);
            cache.get(1);
            cache.get(2);
            // key=1 freq=3, key=2 freq=2
            // 插入 key=3，应淘汰 freq 最低的 key=2
            cache.put(3, 30);
            assertEquals(-1, cache.get(2));
            assertEquals(10, cache.get(1));
            assertEquals(30, cache.get(3));
        }

        @Test
        @DisplayName("连续淘汰验证")
        void consecutiveEvictions() {
            LfuCache cache = new LfuCache(2);
            cache.put(1, 10);
            cache.put(2, 20);
            // 都只访问 1 次，插入 3 时淘汰 1（同 freq 按 LRU，1 更早）
            cache.put(3, 30);
            assertEquals(-1, cache.get(1));

            // 再插入 4，淘汰 2（freq=1 的 LRU）
            cache.put(4, 40);
            assertEquals(-1, cache.get(2));
            assertEquals(30, cache.get(3));
            assertEquals(40, cache.get(4));
        }
    }

    @Nested
    @DisplayName("频率相同按 LRU 淘汰")
    class TieBreakByLRU {

        @Test
        @DisplayName("相同频率时淘汰最久未访问的 key")
        void sameFreqEvictLRU() {
            LfuCache cache = new LfuCache(2);
            cache.put(1, 10);
            cache.put(2, 20);
            // key=1 和 key=2 频率相同（都是 1）
            // 访问 key=1，使其变为最新
            cache.get(1);
            // 插入 key=3，应淘汰同为 freq=2 的 key=2（因为 key=1 刚被访问过）
            cache.put(3, 30);
            assertEquals(-1, cache.get(2));
            assertEquals(10, cache.get(1));
        }

        @Test
        @DisplayName("多次访问后再淘汰")
        void afterMultipleAccess() {
            LfuCache cache = new LfuCache(3);
            cache.put(1, 10);
            cache.put(2, 20);
            cache.put(3, 30);
            // 所有 key freq=1，访问 1 和 2
            cache.get(1);
            cache.get(2);
            // key=3 是 freq=1 中最久未访问的
            cache.put(4, 40);
            assertEquals(-1, cache.get(3));
            assertEquals(10, cache.get(1));
            assertEquals(20, cache.get(2));
            assertEquals(40, cache.get(4));
        }
    }

    @Nested
    @DisplayName("边界情况：容量为 1")
    class CapacityOne {

        @Test
        @DisplayName("容量为 1 时每次 put 都淘汰旧 key")
        void capacityOne() {
            LfuCache cache = new LfuCache(1);
            cache.put(1, 10);
            assertEquals(10, cache.get(1));
            cache.put(2, 20);
            assertEquals(-1, cache.get(1));
            assertEquals(20, cache.get(2));
        }

        @Test
        @DisplayName("容量为 1 时反复更新同一个 key")
        void capacityOneUpdateSameKey() {
            LfuCache cache = new LfuCache(1);
            cache.put(1, 10);
            cache.put(1, 20);
            assertEquals(20, cache.get(1));
        }
    }

    @Nested
    @DisplayName("get 后频率增加")
    class FreqIncreaseOnGet {

        @Test
        @DisplayName("频繁 get 的 key 不容易被淘汰")
        void frequentGetSurvives() {
            LfuCache cache = new LfuCache(3);
            cache.put(1, 10);
            cache.put(2, 20);
            cache.put(3, 30);
            // 大量访问 key=1
            cache.get(1);
            cache.get(1);
            cache.get(1);
            cache.get(2);
            // key=3 freq=1（最低），key=2 freq=2，key=1 freq=4
            cache.put(4, 40);
            assertEquals(-1, cache.get(3));
            assertEquals(10, cache.get(1));
        }
    }

    @Nested
    @DisplayName("put 已存在的 key 更新值且频率增加")
    class UpdateExistingKey {

        @Test
        @DisplayName("更新已存在 key 的值")
        void updateValue() {
            LfuCache cache = new LfuCache(2);
            cache.put(1, 10);
            cache.put(2, 20);
            // 更新 key=1 的值，同时其频率增加
            cache.put(1, 100);
            assertEquals(100, cache.get(1));
            // key=2 freq=1, key=1 freq=2（因为 put 更新也算一次访问）
            cache.put(3, 30);
            assertEquals(-1, cache.get(2));
        }

        @Test
        @DisplayName("先 get 再 put 更新，频率正确累加")
        void getThenUpdate() {
            LfuCache cache = new LfuCache(2);
            cache.put(1, 10);
            cache.put(2, 20);
            cache.get(1);       // key=1 freq=2
            cache.put(1, 100);  // key=1 freq=3
            cache.put(3, 30);   // 淘汰 key=2 (freq=1)
            assertEquals(-1, cache.get(2));
            assertEquals(100, cache.get(1));
            assertEquals(30, cache.get(3));
        }
    }

    @Nested
    @DisplayName("容量为 0 的特殊边界")
    class ZeroCapacity {

        @Test
        @DisplayName("容量为 0 时所有操作无效")
        void zeroCapacity() {
            LfuCache cache = new LfuCache(0);
            cache.put(1, 10);
            assertEquals(-1, cache.get(1));
        }
    }

    @Nested
    @DisplayName("LeetCode 官方示例验证")
    class LeetCodeExample {

        @Test
        @DisplayName("LeetCode 460 示例 1")
        void leetCodeExample1() {
            LfuCache cache = new LfuCache(2);
            cache.put(1, 1);   // cache={1=1}
            cache.put(2, 2);   // cache={1=1, 2=2}
            assertEquals(1, cache.get(1));  // 返回 1，key=1 freq=2
            cache.put(3, 3);   // 淘汰 key=2（freq=1 < freq=2），cache={1=1, 3=3}
            assertEquals(-1, cache.get(2)); // 返回 -1
            assertEquals(3, cache.get(3));  // 返回 3
            cache.put(4, 4);   // 淘汰 key=1（freq=2 vs key=3 freq=2，按 LRU 淘汰 1）
            assertEquals(-1, cache.get(1)); // 返回 -1
            assertEquals(3, cache.get(3));  // 返回 3
            assertEquals(4, cache.get(4));  // 返回 4
        }
    }
}
