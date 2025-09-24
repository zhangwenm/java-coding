package com.geekzhang.worktest.workutil;

import com.google.common.base.Stopwatch;

import java.util.concurrent.TimeUnit;

/**
 * @author zwm
 * @desc TimeUtils
 * @date 2024年08月21日 10:33
 */
public class TimeUtils {
    public static void main(String[] args) {
        Stopwatch stopwatch = Stopwatch.createStarted();

        // 要测量的代码块
        executeTask();


        System.out.println("Execution time: " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
        // 要测量的代码块
        executeTask();
        System.out.println("Execution time: " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
        // 要测量的代码块
        executeTask();
        System.out.println("Execution time: " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
        // 要测量的代码块
        executeTask();
        stopwatch.stop();
    }

    public static void executeTask() {
        // 模拟任务执行
        try {
            Thread.sleep(200); // 休眠200毫秒
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
