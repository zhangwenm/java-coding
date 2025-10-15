package com.geekzhang.worktest.workutil.util;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

import java.util.UUID;

/**
 * @author zwm
 * @desc liftAccountUtil
 * @date 2025年10月13日 09:54
 */
public class liftAccountUtil {
    public static void main(String[] args) {
        String salt = UUID.randomUUID().toString().replaceAll("-", "");
        String password = "8DE70DDEB78B7B1A7F7952727B1A7F79";
        String encryptedPassword = Hashing.sha256()
                .newHasher()
                .putString(salt + password, Charsets.UTF_8)
                .hash()
                .toString();
        System.out.println("salt: " + salt);
        System.out.println("password: " + password);
        System.out.println("encryptedPassword: " + encryptedPassword);
        //INSERT INTO `mqtt_user` (`id`, `username`, `password`, `salt`, `is_superuser`, `created`)
        //VALUES (58027, '594A2D595273225052200001', 'd6755f805c3ca4bdc715a589466e2b4e6efb75863aff672ece37464e39f41be6', '2e68606d822b4ae39847b7671d8b5b28', 0, '2025-10-13 09:50:19');
    }
}
