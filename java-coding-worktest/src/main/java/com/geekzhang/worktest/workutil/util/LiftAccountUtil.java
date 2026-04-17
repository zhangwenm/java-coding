package com.geekzhang.worktest.workutil.util;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

/**
 * @author zwm
 * @desc liftAccountUtil
 * @date 2025年10月13日 09:54
 */
public class LiftAccountUtil {
    public static void main(String[] args) {
//        String password = "D3AD0ADC372B36521E483C6736521E48";
        //String productId = "2000dbe120ec49a4";
        Integer id = 63271;
        String proListStr ="440050000A0000344351534E";
        String[] proList  = proListStr.split(",");
        for (int i = 0; i < proList.length; i++) {

            id = id+i;
            String productId = proList[i];
            String salt = UUID.randomUUID().toString().replaceAll("-", "");

            if(StringUtils.isNotBlank(productId)){

                String password = "90AD70DDF32344341351594E44341351";


                String encryptedPassword = Hashing.sha256()
                        .newHasher()
                        .putString(salt + password, Charsets.UTF_8)
                        .hash()
                        .toString();
//                System.out.println("salt: " + salt);
//                System.out.println("password: " + password);
//                System.out.println("encryptedPassword: " + encryptedPassword);

                String res =
                        "INSERT INTO `mqtt_user` (`id`, `username`, `password`, `salt`, `is_superuser`, `created`) VALUES(%s, '%s', '%s', '%s', 0, '2026-04-03 14:47:50');";

                String sql = String.format(res,id,productId,encryptedPassword,salt);
                System.out.println( sql+";");

            }
        }


        //INSERT INTO `mqtt_user` (`id`, `username`, `password`, `salt`, `is_superuser`, `created`)
        //VALUES (63229, '2D003B000A30315151334A53', 'd54a4bf90bf06c8c3c30df48715d9cf2d18f1b3000087226a7855b521e79caac', 'd1705fb5057042a3a615bfb1a9248b2d', 0, '2026-02-09 14:12:19');
       // INSERT INTO `mqtt_user` (`id`, `username`, `password`, `salt`, `is_superuser`, `created`) VALUES(59099, '2000dcca20ec3f00', '15b9239f255b137936c7144d54c7d355e7f1749ec536731c969d5a7efbd206ad', '12f7304b61fb4fd38786e15d0b648a57', 0, '2026-02-25 14:47:50');

    }
}
