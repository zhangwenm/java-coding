package com.geekzhang.algorithms.niuke;

import java.util.Random;

/**
 * 快速排序（随机化 pivot 版本）
 *
 * 【核心思路】
 * 每次随机选一个元素作为基准值（pivot），通过 partition 将数组分为两部分：
 * - 左边 ≤ pivot
 * - 右边 ≥ pivot
 * 然后递归排序左右两部分。
 *
 * 【随机化的意义】
 * 固定选第一个元素作为 pivot，在已排序/接近排序的数组上会退化为 O(n²)。
 * 随机化 pivot 使得期望时间复杂度稳定在 O(n log n)，不受输入分布影响。
 *
 * 【复杂度】
 * - 时间：平均 O(n log n)，最坏 O(n²)（极端情况概率极低）
 * - 空间：O(log n) 递归栈深度
 *
 * @author zwm
 */
public class QuickSort2 {

    private final Random random = new Random();

    public void sort(int[] arr) {
        if (arr == null || arr.length <= 1) {
            return;
        }
        quickSort(arr, 0, arr.length - 1);
    }

    private void quickSort(int[] arr, int left, int right) {
        if (left >= right) {
            return;
        }
        int pivotIndex = partition(arr, left, right);
        quickSort(arr, left, pivotIndex - 1);
        quickSort(arr, pivotIndex + 1, right);
    }

    /**
     * 随机选 pivot，放到 left 位置，然后执行经典 partition（挖坑填数法）
     * 返回 pivot 最终所在的下标
     */
    private int partition(int[] arr, int left, int right) {
        int randomIndex = left + random.nextInt(right - left + 1);
        swap(arr, left, randomIndex);

        int pivot = arr[left];
        int i = left, j = right;

        while (i < j) {
            while (i < j && arr[j] >= pivot) {
                j--;
            }
            if (i < j) {
                arr[i] = arr[j];
                i++;
            }
            while (i < j && arr[i] <= pivot) {
                i++;
            }
            if (i < j) {
                arr[j] = arr[i];
                j--;
            }
        }
        arr[i] = pivot;
        return i;
    }

    private void swap(int[] arr, int a, int b) {
        if (a != b) {
            int tmp = arr[a];
            arr[a] = arr[b];
            arr[b] = tmp;
        }
    }
}
