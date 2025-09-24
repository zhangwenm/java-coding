package com.geekzhang.mybatisplus.vo;

import lombok.Data;

import java.util.UUID;

/**
 * @author zwm
 * @desc MeituanStoreInfo
 * @date 2025年09月23日 11:06
 */
@Data
public class MeituanStoreInfoRegister {
    private String requestId = UUID.randomUUID().toString();
     private String robotId;

    private String robotName;
    private String model;
    private String modelName;
     private String storeId;

    private String province;

    private String city;

    private String district;

    private String address;
        private Integer cabNum;
    private String servicePhone;
    private String keyword;
    private String latitude;
    private String longitude;

    private String storeName;

    private String placeId;
    private String type;

    private String sn;

}
