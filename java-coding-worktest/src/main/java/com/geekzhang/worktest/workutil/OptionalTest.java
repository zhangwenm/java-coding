package com.geekzhang.worktest.workutil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * @author zwm
 * @desc OptionalTest
 * @date 2024年08月15日 15:37
 */
public class OptionalTest {
    public static void main(String[] args) {
        Optional<Object> object = Optional.empty();//Optional<>
        object = object.ofNullable("123");
        if(object.isPresent()){
            System.out.println(object.get());
        }
        LocalDateTime localDate = LocalDate.now().atStartOfDay();
        System.out.println(localDate);
    }
}
