package com.geekzhang.worktest.workutil.dto;

import com.alibaba.fastjson.JSONArray;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;


@Data
public class StoreInfo {
    private String id;

    private String name;

    private String address;

    private String imageUrl;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private String country;

    private String province;

    private String cityCode;

    private String city;

    private String districtCode;

    private String district;

    private String townCode;

    private String townShip;

    private String operationType;

    private String groupId;

    private String brandId;

    private String hotelType;

    private String hotelLevel;

    private String brandType;

    private String customerCategory;

    private String oldStoreId;

    private String placeId;

    private String createUser;

    private String updateUser;

    private Object loc;

    private Date createTime;

    private Date updateTime;

    private String parent;

    private String adcode;
    private String principal;//门店负责人

    private Integer state;

    private Boolean isOpen;

    private Boolean isExist;

    private Object ext;
    private Date reconciliationDate;
    /**
     * ai上线时间
     */
    private Date onlineDate;
    /**
     * ai到期时间
     */

    private Date maturityDate;

    /** 收款账号主键ID */
    private String merchantId;
    private Integer floors;
    private Integer area;
    private Integer officePersons;
    private String propertyCompanyName;
    private String ownerCompanyName;
    private String operationDate;
    private JSONArray gmvTargets;

    private String iotBrandId;
}
