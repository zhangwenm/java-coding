package com.geekzhang.worktest.workutil;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zwm
 * @desc MonitorUrlUtil
 * @date 2023年12月21日 15:50
 */
public class MonitorUrlUtil {
    private static final Pattern PATTERN = Pattern.compile("172\\.(\\d+)\\.(\\d+)\\.(\\d+)");
    private static final Pattern OLD_HOST_PATTERN = Pattern.compile("[nw]cus(\\d+)-(\\d+)\\.monitor\\.\\.com\\.cn.*");
    private static final Pattern HK_HOST_PATTERN = Pattern.compile("[nw]cus(\\d+)-(\\d+)\\.monitor-hk\\.\\.com\\.cn.*");
    private static final Pattern NEW_HOST_PATTERN = Pattern.compile("ncus(\\d+)-(\\d+)-(\\d+)\\.monitor-t\\.\\.cn.*");

    public MonitorUrlUtil() {
    }

    public static String buildUrl(String robotIp) {
        return buildUrl(robotIp, "NVWA");
    }

    public static String buildUrl(String robotIp, String type) {
        if (StringUtils.isBlank(robotIp)) {
            return "";
        } else {
            try {
                Matcher matcher = PATTERN.matcher(robotIp);
                if (!matcher.find()) {
                    return "";
                } else {
                    int num0 = Integer.parseInt(matcher.group(1));
                    int num1 = Integer.parseInt(matcher.group(2));
                    int num2 = Integer.parseInt(matcher.group(3));
                    String vpnUrl = "";
                    switch (num0) {
                        case 16:
                            vpnUrl = String.format("http://%s%d-%d.monitor%s.com.cn", buildPrefix(type), num1 * 313 + 133, num2 * 593 + 3243289, "");
                            break;
                        case 17:
                            vpnUrl = String.format("http://%s%d-%d.monitor%s.com.cn", buildPrefix(type), num1 * 313 + 133, num2 * 897 + 31423689, "-hk");
                            break;
                        case 18:
                        case 20:
                        case 21:
                        case 22:
                        case 24:
                        case 25:
                            vpnUrl = String.format("http://ncus%d-%d-%d.monitor%s.cn", num0 + 314, num1 * 313 + 133, num2 * 593 + 3243289, "-t");
                        case 19:
                        case 23:
                    }

                    return vpnUrl;
                }
            } catch (Exception var7) {
                return "";
            }
        }
    }

    private static String buildPrefix(String type) {
        return "NVWA".equals(type) ? "ncus" : "wcus";
    }

    public static String hostToIp(String host) {
        Matcher matcher = OLD_HOST_PATTERN.matcher(host);
        if (matcher.find()) {
            return String.format("172.16.%d.%d", (Integer.parseInt(matcher.group(1)) - 133) / 313, (Integer.parseInt(matcher.group(2)) - 3243289) / 593);
        } else {
            matcher = NEW_HOST_PATTERN.matcher(host);
            if (matcher.find()) {
                return String.format("172.%d.%d.%d", Integer.parseInt(matcher.group(1)) - 314, (Integer.parseInt(matcher.group(2)) - 133) / 313, (Integer.parseInt(matcher.group(3)) - 3243289) / 593);
            } else {
                matcher = HK_HOST_PATTERN.matcher(host);
                return matcher.find() ? String.format("172.17.%d.%d", (Integer.parseInt(matcher.group(1)) - 133) / 313, (Integer.parseInt(matcher.group(2)) - 31423689) / 897) : "";
            }
        }
    }

    public static void main(String[] args) {
        String url = buildUrl("172.22.225.8");
        System.out.println(url);
    }
}
