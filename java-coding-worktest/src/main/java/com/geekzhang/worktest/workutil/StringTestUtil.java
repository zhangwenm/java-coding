package com.geekzhang.worktest.workutil;

import com.alibaba.fastjson.JSONObject;
import com.geekzhang.worktest.workutil.util.DateUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author zwm
 * @desc MapUtil
 * @date 2023年09月20日 20:23
 */
public class StringTestUtil {

    public static void main(String[] args) {

        String yuncang = "{\"batch\":3,\"bitRate\":0.2,\"block2Mid\":false,\"checkPush\":true,\"compareTimes\":4,\"compareTimesBlock\":4,\"compress\":true,\"connectVPN\":true,\"crf\":40,\"dayVolumn\":30.0,\"debug\":true,\"frameRate\":30,\"hasBiaoding\":true,\"hasBiaoding31\":true,\"hight\":137,\"hight31\":121,\"hkServer\":false,\"id\":\"YCC00661220310214\",\"isTask2\":true,\"language\":\"zh\",\"map\":[{\"column\":9,\"row\":1,\"type\":1},{\"column\":9,\"row\":2,\"type\":1},{\"column\":7,\"row\":3,\"type\":1},{\"column\":4,\"row\":4,\"type\":2}],\"maxColumn\":9,\"maxRow\":4,\"nightVolumn\":0.3,\"openServer\":true,\"openWebSever\":true,\"retry\":3,\"serialName\":\"ttyS3\",\"similar\":0.3,\"similar31\":0.3,\"similar_reduce\":0.0,\"sn\":\"6ad5bbfa94cc481084f08e0db1b4b423\",\"uploadPic\":false,\"use30Similar\":true,\"useAAC\":true,\"useDuishe\":false,\"useOpenCV\":true,\"useSecretMqtt\":true,\"useTestEnv\":false,\"videoSize\":0,\"vidioStayDay\":10,\"wayRollHalf\":true,\"webServerType\":0,\"width\":149,\"width31\":113,\"wifiOpen\":true,\"x\":245,\"x31\":283,\"y\":222,\"y31\":248}";

        JSONObject jsonObject = JSONObject.parseObject(yuncang);

        System.out.println("jsonObject:"+jsonObject);



        String str = "2024-03-03";
        System.out.println("res:"+ StringUtils.substring(str,  str.length()-4, str.length()));

        System.out.println("year:"+ StringUtils.substring(str,  0, 4));

        String date = DateUtils.timestampToString(new Date().getTime()-24*60*60*1000*10,"yyyy-MM-dd");

        String queryKey ="20240307";

        if(StringUtils.isBlank(queryKey)||queryKey.compareTo(date.replaceAll("-",""))>0){
            System.out.println("date:"+date);
        }
        validateOpenSign("executor");


        System.out.println("1253022531540094976_DHTZJS：len："+"1253022531540094976_DHTZJS".length());

        String routerNumber = "35671";

        String softwareVersion = "v2";

        if(StringUtils.isNotBlank(routerNumber)){
            Integer.parseInt(routerNumber);
            softwareVersion = new BigDecimal(routerNumber).compareTo(new BigDecimal(35673)) >= 0 ? "v3" :"v2";
        }
        System.out.println("softwareVersion:"+"（Z53224）如家商旅酒店2.0（青岛）城阳黑龙江北路城中城凤岗路地铁站店（特）".length());

    }
    private static void validateOpenSign(String module) {
        String key = "";
        String version = "v1.4.12";
        switch (module){
            case "executor":
                key = "rcs-app-executor";
                break;
            case "up_tools":
                key = "up_tools";
                break;
            case "voice_package":
                key = "robot-default-voice-packet";
                break;
            case "cabin_web_app":
                key = "cabin-web-app";
                break;
            default:break;
        }
        // 对象键(Key)是对象在存储桶中的唯一标识。详情请参见 [对象键](https://cloud.tencent.com/document/product/436/13324)
        key += version + ".zip";
        System.out.println("key:"+key);
    }
}
