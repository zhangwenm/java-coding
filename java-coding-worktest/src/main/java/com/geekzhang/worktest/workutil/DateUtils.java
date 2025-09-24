package com.geekzhang.worktest.workutil;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author zwm
 * @desc DateUtils
 * @date 2023年10月11日 13:39
 */
public class DateUtils {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("mm");
    private static final DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter formatter3 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static String timestampToString(Long date) {
        Instant instant = Instant.ofEpochMilli(date);

        // 使用 ZoneId 定义时区，这里使用默认时区
        ZoneId zoneId = ZoneId.systemDefault();

        // 将 Instant 转换为 LocalDateTime
        LocalDateTime localDateTime = instant.atZone(zoneId).toLocalDateTime();

        // 使用 DateTimeFormatter 格式化日期时间
        String res  = localDateTime.format(formatter3);


        return res;
    }
    public static String HHmm(Long date) {
        date = 1706803036000l;
        Instant instant = Instant.ofEpochMilli(date);

        // 使用 ZoneId 定义时区，这里使用默认时区
        ZoneId zoneId = ZoneId.systemDefault();

        // 将 Instant 转换为 LocalDateTime
        LocalDateTime localDateTime = instant.atZone(zoneId).toLocalDateTime();

        // 使用 DateTimeFormatter 格式化日期时间
        String res  = localDateTime.format(formatter);
        if(Integer.valueOf(res)%5!=0){

            Integer mo = 5- Integer.valueOf(res)%5;
            date =mo*1000*60+date;
        }

        Instant instant1 = Instant.ofEpochMilli(date);


        // 将 Instant 转换为 LocalDateTime
        LocalDateTime localDateTime1 = instant1.atZone(zoneId).toLocalDateTime();

        System.out.printf("yyyy-MM-dd HH:mm:%s",localDateTime1.format(formatter2));
        String hhmm  = localDateTime1.format(formatter1);
        return hhmm;
    }
    private static final Map<String, List<String>> types = new HashMap(16) {{
        put("SCQS", Lists.newArrayList("router", "mcu_fw"));
        put("SC", Lists.newArrayList("deb_package", "sys_package"));
        put("WT", Lists.newArrayList("deb_package", "sys_package"));
    }};
    public static void main(String[] args) {

        // 使用 ZoneId 来指定时区
        ZoneId zoneId = ZoneId.systemDefault();

        // 将 Date 转换为 LocalDate
        LocalDate startDate = new Date().toInstant().atZone(zoneId).toLocalDate();

        System.out.println("LocalDate day:"+startDate);
        System.out.println("Date:"+new Date().toString()+"--Date year: "+new Date().getYear()+"--Date month: "+new Date().getMonth()+"--Date day: "+new Date().getDate());
        String[] arr = "yyyy-MM-dd HH:mm:ss".split(" ");
        System.out.printf("mo:%s",HHmm(new Date().getTime()));
        LocalDate startDay = LocalDate.of(2023, 9, 1);
        LocalDate endDay = LocalDate.of(2024, 10, 9);
        List<String> modules = daysBetweenDates(new Date(), new Date());

//        List<String> modules = getModules("SC", "QS");

        System.out.println("LocalDate.now():"+LocalDate.now().toString());

getMillion();

        List<Date> dates = new ArrayList<>();

        dates.add(new Date(new Date().getTime()-100));
        dates.add(new Date());
        dates.add(new Date(new Date().getTime()-200));
        dates.sort(Comparator.comparing(Date::getTime).reversed());
        System.out.println("paixu :"+JSON.toJSONString(dates));

        Date date1 = new Date();
        Date date2 = new Date(new Date().getTime()-200);
        System.out.println("after:"+date1.after(date2));
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
    public static List<String> daysBetweenDates(Date start, Date end) {
        // 使用 ZoneId 来指定时区
        ZoneId zoneId = ZoneId.systemDefault();

        // 将 Date 转换为 LocalDate
        LocalDate startDate = start.toInstant().atZone(zoneId).toLocalDate();

        LocalDate endDate = end.toInstant().atZone(zoneId).toLocalDate();


//        LocalDate startDay = LocalDate.of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth());
//        LocalDate endDay = LocalDate.of(endDate.getYear(), endDate.getMonth(), endDate.getDayOfMonth());

//        LocalDate currentDay = startDay;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentDay = LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), 0, 0, 0);
        LocalDateTime endDay = currentDay.plusDays(1);
        List<String> res = Lists.newArrayList();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd");
        DateTimeFormatter formatterMon = DateTimeFormatter.ofPattern("MM");
        DateTimeFormatter formatterYea = DateTimeFormatter.ofPattern("HH:mm");
        while (currentDay.isBefore(endDay)) {
            res.add(currentDay.format(formatterYea));
            currentDay = currentDay.plusMinutes(5);
        }
        return res;
    }
    public static void getMillion() {
        // 使用 ZoneId 来指定时区

        System.out.printf("秒："+ new Date().getTime()/ 1000);
    }
}
