package com.geekzhang.mybatisplus.util;



import org.apache.commons.lang3.StringUtils;

import java.util.Random;
import java.util.regex.Pattern;

public class MobileUtil {

    public static String createSmsCode() {
        char[] codeSeq = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
        Random random = new Random();
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            String r = String.valueOf(codeSeq[random.nextInt(codeSeq.length)]);
            s.append(r);
        }
        return s.toString();
    }

    public static boolean validMobile(String mobile){
        if (StringUtils.isNotEmpty(mobile)){
            String regex = "^((13[0-9])|(14[0,1,4-9])|(15[0-3,5-9])|(16[2,5,6,7])|(17[0-8])|(18[0-9])|(19[0-3,5-9]))\\d{8}$";
            return Pattern.matches(regex,mobile);
        }else {
            return false;
        }
    }

    public static boolean validIdcard(String idcard){
        if (StringUtils.isNotEmpty(idcard)){
            String id_18="^[1-9][0-9]{5}(18|19|20)[0-9]{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)[0-9]{3}([0-9]|(X|x))";
            String id_15="^[1-9][0-9]{5}[0-9]{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)[0-9]{2}[0-9]";
            String id_valid="("+id_18+")"+"|"+"("+id_15+")";
            return Pattern.matches(id_valid,idcard);
        }else {
            return false;
        }
    }
}
