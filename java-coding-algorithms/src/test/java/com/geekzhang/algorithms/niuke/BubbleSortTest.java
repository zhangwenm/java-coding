package com.geekzhang.algorithms.niuke;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class BubbleSortTest {

    private final BubbleSort sorter = new BubbleSort();

    private void assertSorted(int[] input, int[] expected) {
        int[] copy = input.clone();
        sorter.sort(copy);
        assertArrayEquals(expected, copy);
    }

    @Nested
    @DisplayName("基本排序")
    class BasicSort {

        @Test
        @DisplayName("空数组不变")
        void emptyArray() {
            assertSorted(new int[]{}, new int[]{});
        }

        @Test
        @DisplayName("单元素不变")
        void singleElement() {
            assertSorted(new int[]{1}, new int[]{1});
        }

        @Test
        @DisplayName("两个元素正常排序")
        void twoElements() {
            assertSorted(new int[]{2, 1}, new int[]{1, 2});
        }

        @Test
        @DisplayName("已排序数组不变")
        void alreadySorted() {
            assertSorted(new int[]{1, 2, 3, 4, 5}, new int[]{1, 2, 3, 4, 5});
        }

        @Test
        @DisplayName("逆序数组排序")
        void reverseOrder() {
            assertSorted(new int[]{5, 4, 3, 2, 1}, new int[]{1, 2, 3, 4, 5});
        }
    }

    @Nested
    @DisplayName("含重复元素")
    class Duplicates {

        @Test
        @DisplayName("全部相同")
        void allSame() {
            assertSorted(new int[]{3, 3, 3, 3}, new int[]{3, 3, 3, 3});
        }

        @Test
        @DisplayName("部分重复")
        void someDuplicates() {
            assertSorted(new int[]{3, 1, 2, 1, 3}, new int[]{1, 1, 2, 3, 3});
        }
    }

    @Nested
    @DisplayName("边界和随机")
    class EdgeCases {

        @Test
        @DisplayName("null 不抛异常")
        void nullInput() {
            sorter.sort(null);
        }

        @Test
        @DisplayName("大数组随机排序正确")
        void largeRandomArray() {
            Random rng = new Random(42);
            int[] arr = new int[500];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = rng.nextInt(10000);
            }
            int[] expected = arr.clone();
            Arrays.sort(expected);
            assertSorted(arr, expected);
        }
    }
}
