package com.geekzhang.worktest.workutil.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zwm
 * @desc Device
 * @date 2023年09月04日 11:17
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MethodDto {


    @ColumnWidth(20)
    @ExcelProperty("api")
    private String url;
    @ColumnWidth(20)
    @ExcelProperty("permission")
    private String permission;
    @ColumnWidth(20)
    @ExcelProperty("apiV4")
    private String crowUrl;
    @ColumnWidth(20)
    @ExcelProperty("per")
    private String newPermission;

}
