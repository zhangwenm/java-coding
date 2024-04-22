package com.geekzhang.worktest.workutil.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author zwm
 * @desc DateUtils
 * @date 2023年10月11日 09:22
 */
public abstract class DateUtils {
    private static final DateTimeFormatter formatterMillionEnd = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter mmEndFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter year_Formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter yearFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter hh_MMFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter mmFormatter = DateTimeFormatter.ofPattern("mm");
    private static final DateTimeFormatter formatterMonthEnd = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static volatile List<String> timeSlots = Lists.newArrayList();

    private static final Map<String ,DateTimeFormatter> formatterMap = new HashMap() {{
        put("yyyy-MM-dd HH:mm:ss", formatterMillionEnd);
        put("yyyy-MM-dd",formatterMonthEnd);
        put("yyyyMMdd", yearFormatter);
        put("HH:mm", hh_MMFormatter);
        put("mm", mmFormatter);
        put("yyyyMM", monthFormatter);
    }};

    public static String timestampToString(Long date) {
        Instant instant = Instant.ofEpochMilli(date);

        // 使用 ZoneId 定义时区，这里使用默认时区
        ZoneId zoneId = ZoneId.systemDefault();

        // 将 Instant 转换为 LocalDateTime
        LocalDateTime localDateTime = instant.atZone(zoneId).toLocalDateTime();
        // 使用 DateTimeFormatter 格式化日期时间
        String res  = localDateTime.format(formatterMillionEnd);
        return res;
    }

    public static LocalDate timestampToLocalDate(Long date) {
        Instant instant = Instant.ofEpochMilli(date);

        // 使用 ZoneId 定义时区，这里使用默认时区
        ZoneId zoneId = ZoneId.systemDefault();

        // 将 Instant 转换为 LocalDateTime
        LocalDate localDate = instant.atZone(zoneId).toLocalDate();
        // 使用 DateTimeFormatter 格式化日期时间
        return localDate;
    }
    public static LocalDateTime timestampToLocalDateTime(Long date) {
        Instant instant = Instant.ofEpochMilli(date);

        // 使用 ZoneId 定义时区，这里使用默认时区
        ZoneId zoneId = ZoneId.systemDefault();

        // 将 Instant 转换为 LocalDateTime
        LocalDateTime localDateTime = instant.atZone(zoneId).toLocalDateTime();
        // 使用 DateTimeFormatter 格式化日期时间
        return localDateTime;
    }

    public static String timestampToString(Long date,String formatStr) {
        Instant instant = Instant.ofEpochMilli(date);

        // 使用 ZoneId 定义时区，这里使用默认时区
        ZoneId zoneId = ZoneId.systemDefault();

        // 将 Instant 转换为 LocalDateTime
        LocalDateTime localDateTime = instant.atZone(zoneId).toLocalDateTime();
        DateTimeFormatter format = formatterMap.getOrDefault(formatStr, formatterMillionEnd);
        // 使用 DateTimeFormatter 格式化日期时间
        String res  = localDateTime.format(format);
        return res;
    }
    /**
     * @desc 两个日期之间间隔的月份（202306 202307 ……）
     * @author zwm
     * @date 2023/10/11 11:58
     * @param start
     * @param end
     * @return java.lang.String
     */
    public static Set<String> monthBetweenDates(Date start, Date end) {
        // 使用 ZoneId 来指定时区
        ZoneId zoneId = ZoneId.systemDefault();

        // 将 Date 转换为 LocalDate
        LocalDate startDate = start.toInstant().atZone(zoneId).toLocalDate();

        LocalDate endDate = end.toInstant().atZone(zoneId).toLocalDate();


        LocalDate startDay = LocalDate.of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth());
        LocalDate endDay = LocalDate.of(endDate.getYear(), endDate.getMonth(), endDate.getDayOfMonth());

        LocalDate currentDay = startDay;

        Set<String> res = Sets.newHashSet();
        DateTimeFormatter formatterYea = DateTimeFormatter.ofPattern("yyyyMM");
        while (!currentDay.isAfter(endDay)) {
            res.add(currentDay.format(formatterYea));
            currentDay = currentDay.plusDays(1);
        }
        return res;
    }
    /**
     * @desc 两个日期之间间隔的天（20230601 20230701 ……）
     * @author zwm
     * @date 2023/10/11 11:58
     * @param start
     * @param end
     * @return java.lang.String
     */
    public static Set<String> daysBetweenDates(Date start, Date end) {
        // 使用 ZoneId 来指定时区
        ZoneId zoneId = ZoneId.systemDefault();

        // 将 Date 转换为 LocalDate
        LocalDate startDate = start.toInstant().atZone(zoneId).toLocalDate();

        LocalDate endDate = end.toInstant().atZone(zoneId).toLocalDate();


        LocalDate startDay = LocalDate.of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth());
        LocalDate endDay = LocalDate.of(endDate.getYear(), endDate.getMonth(), endDate.getDayOfMonth());

        LocalDate currentDay = startDay;

        Set<String> res = Sets.newHashSet();
        DateTimeFormatter formatterYea = DateTimeFormatter.ofPattern("yyyyMMdd");
        while (!currentDay.isAfter(endDay)) {
            res.add(currentDay.format(formatterYea));
            currentDay = currentDay.plusDays(1);
        }
        return res;
    }
    /**
     * @desc desc
     * @author zwm
     * @date 2024/2/2 11:36
     * @param time
     * @return java.lang.String yyyy-MM-dd HH:mm
     */
    public static String getTimeSlice(Long time) {

        Instant instant = Instant.ofEpochMilli(time);
        // 使用 ZoneId 定义时区，这里使用默认时区
        ZoneId zoneId = ZoneId.systemDefault();
        // 将 Instant 转换为 LocalDateTime
        LocalDateTime localDateTime = instant.atZone(zoneId).toLocalDateTime();

        // 使用 DateTimeFormatter 格式化日期时间
        String res  = localDateTime.format(mmFormatter);
        if(Integer.valueOf(res)%5!=0){

            Integer mo = 5- Integer.valueOf(res)%5;
            time =mo*1000*60+time;
        }

        Instant instant1 = Instant.ofEpochMilli(time);


        // 将 Instant 转换为 LocalDateTime
        LocalDateTime localDateTime1 = instant1.atZone(zoneId).toLocalDateTime();

        String hhmm  = localDateTime1.format(mmEndFormatter);
        return hhmm;
    }
    /**
     * @desc 整天5分钟间隔时间段综合（00:00 00:05 00:10……）
     * @author zwm
     * @date 2023/10/11 11:58
     * @param time
     * @return java.lang.String
     */
    public static List<String> hhMMDay(Long time) {
        if(timeSlots.size()!=288){
            synchronized (DateUtils.class){
                if(timeSlots.size()!=288){
                    Instant instant = Instant.ofEpochMilli(time);
                    // 使用 ZoneId 定义时区，这里使用默认时区
                    ZoneId zoneId = ZoneId.systemDefault();
                    // 将 Instant 转换为 LocalDateTime
                    LocalDateTime start = instant.atZone(zoneId).toLocalDateTime();

                    LocalDateTime currentDay = LocalDateTime.of(start.getYear(), start.getMonth(), start.getDayOfMonth(), 0, 0, 0);
                    LocalDateTime endDay = currentDay.plusDays(1);

                    DateTimeFormatter formatterYea = DateTimeFormatter.ofPattern("HH:mm");
                    while (currentDay.isBefore(endDay)) {
                        timeSlots.add(currentDay.format(formatterYea));
                        currentDay = currentDay.plusMinutes(5);
                    }
                }
            }
        }
        return timeSlots;
    }
}
