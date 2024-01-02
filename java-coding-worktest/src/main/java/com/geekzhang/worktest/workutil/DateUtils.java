package com.geekzhang.worktest.workutil;

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
 * @date 2023年10月11日 13:39
 */
public class DateUtils {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String timestampToString(Long date) {
        Instant instant = Instant.ofEpochMilli(date);

        // 使用 ZoneId 定义时区，这里使用默认时区
        ZoneId zoneId = ZoneId.systemDefault();

        // 将 Instant 转换为 LocalDateTime
        LocalDateTime localDateTime = instant.atZone(zoneId).toLocalDateTime();

        // 使用 DateTimeFormatter 格式化日期时间
        String res  = localDateTime.format(formatter);
        return res;
    }
    private static final Map<String, List<String>> types = new HashMap(16) {{
        put("SCQS", Lists.newArrayList("router", "mcu_fw"));
        put("SC", Lists.newArrayList("deb_package", "sys_package"));
        put("WT", Lists.newArrayList("deb_package", "sys_package"));
    }};
    public static void main(String[] args) {
        LocalDate startDay = LocalDate.of(2023, 9, 1);
        LocalDate endDay = LocalDate.of(2024, 10, 9);
        Set<String> modules = daysBetweenDates(new Date(), new Date());

//        List<String> modules = getModules("SC", "QS");

        System.out.println(modules);

getMillion();
    }


    public static List<String> getModules(String type, String subtype) {
        // 使用 ZoneId 来指定时区
        return types.getOrDefault(type+subtype,types.get(type));
    }

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
        DateTimeFormatter formatterMonth = DateTimeFormatter.ofPattern("MM");
        DateTimeFormatter formatterYea = DateTimeFormatter.ofPattern("yyyy");
        while (!currentDay.isAfter(endDay)) {
            res.add(currentDay.format(formatterYea) + currentDay.format(formatterMonth));
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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd");
        DateTimeFormatter formatterMon = DateTimeFormatter.ofPattern("MM");
        DateTimeFormatter formatterYea = DateTimeFormatter.ofPattern("yyyyMMdd");
        while (!currentDay.isAfter(endDay)) {
            res.add(currentDay.format(formatterYea) + currentDay.format(formatterMon) + currentDay.format(formatter));
            currentDay = currentDay.plusDays(1);
        }
        return res;
    }
    public static void getMillion() {
        // 使用 ZoneId 来指定时区

        System.out.printf("秒："+ new Date().getTime()/ 1000);
    }
}
