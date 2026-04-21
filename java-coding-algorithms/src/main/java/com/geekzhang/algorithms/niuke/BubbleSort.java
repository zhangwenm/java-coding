package com.geekzhang.algorithms.niuke;

/**
 * 冒泡排序（提前终止优化版）
 *
 * 【核心思路】
 * 每轮遍历将相邻元素两两比较，逆序则交换，一轮下来最大元素"冒泡"到末尾。
 * 如果某轮遍历中没有发生任何交换，说明数组已经有序，提前终止。
 *
 * 【复杂度】
 * - 时间：最好 O(n)（已排序，一轮就终止），最坏/平均 O(n²)
 * - 空间：O(1)，原地排序
 *
 * @author zwm
 */
public class BubbleSort {

    public void sort(int[] arr) {
        if (arr == null || arr.length <= 1) {
            return;
        }
        int n = arr.length;
        for (int i = 0; i < n - 1; i++) {
            boolean swapped = false;
            for (int j = 0; j < n - 1 - i; j++) {
                if (arr[j] > arr[j + 1]) {
                    int tmp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = tmp;
                    swapped = true;
                }
            }
            if (!swapped) {
                break;
            }
        }
    }
}
