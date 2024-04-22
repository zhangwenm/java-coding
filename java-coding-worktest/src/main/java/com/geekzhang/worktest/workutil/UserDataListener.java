package com.geekzhang.worktest.workutil;



import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.geekzhang.worktest.workutil.dto.MethodDto;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author zwm
 * @desc UserDataListener
 * @date 2023年12月22日 14:31
 */
public class UserDataListener extends AnalysisEventListener<MethodDto> {
    private Map<String,MethodDto> methodDtos = new HashMap<>();
    private List<MethodDto> oldMethodDtos = new ArrayList<>();
    private Map<String,String> map = new HashMap<>();
    @Override
    public void invoke(MethodDto data, AnalysisContext context) {
        // 处理每一行数据
        System.out.println("读取到数据：" + data);
        oldMethodDtos.add(data);
        if(StringUtils.isNotBlank(data.getUrl())){
            methodDtos.put(data.getUrl(),data );
        }
        // 在这里可以将数据保存到数据库等业务逻辑
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        oldMethodDtos.forEach(item->{
            if(StringUtils.isNotBlank(item.getCrowUrl()) && methodDtos.containsKey(item.getCrowUrl())){
                item.setNewPermission(methodDtos.get(item.getCrowUrl()).getPermission());
            }
        });
        EasyExcel.write("/Users/admin/Downloads/interfaceNewAll.xlsx",MethodDto.class).sheet("模板").doWrite(oldMethodDtos);
        // 所有数据读取完成后的操作，可以在这里进行一些清理工作
    }



}
