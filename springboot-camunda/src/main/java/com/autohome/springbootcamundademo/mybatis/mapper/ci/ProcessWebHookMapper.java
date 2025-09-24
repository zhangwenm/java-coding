package com.autohome.springbootcamundademo.mybatis.mapper.ci;

import com.autohome.springbootcamundademo.mybatis.entity.ci.ProcessWebHookEntity;

import java.util.List;

public interface ProcessWebHookMapper {
    public List<ProcessWebHookEntity> getAllCamundaProcessWebHookEntity();

    public static void main(String[] args) {
        //冒泡排序并输出排序前和排序后的数组
        int[] arr = {1, 5, 3, 2, 4};
        System.out.println("排序前：");
        for (int i = 0; i < arr.length; i++) {
            System.out.print(arr[i] + " ");
        }
        for (int i = 0; i < arr.length - 1; i++) {
            for (int j = 0; j < arr.length - 1 - i; j++) {
                if (arr[j] > arr[j + 1]) {
                    int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                }
            }
        }

        System.out.println();
        System.out.println("排序后：");
        for (int i = 0; i < arr.length; i++) {
            System.out.print(arr[i] + " ");
        }

    }
}
