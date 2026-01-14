package com.geekzhang.mybatisplus.dao;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import com.geekzhang.mybatisplus.entity.MeituanXiaodaiInfo;
import com.geekzhang.mybatisplus.mapper.MeituanXiaodaiInfoMapper;
import com.geekzhang.mybatisplus.service.HttpExampleService;
import com.geekzhang.mybatisplus.vo.MeituanStoreInfoRegister;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@SpringBootTest
public class MeituanMapperTest {

	@Autowired
	private MeituanXiaodaiInfoMapper meituanXiaodaiInfoMapper;
	@Autowired
	private HttpExampleService httpExampleService;


	@Test
	public void testQuery() throws Exception {
		List<MeituanXiaodaiInfo> meituanXiaodaiInfoList = meituanXiaodaiInfoMapper.selectList(new LambdaUpdateWrapper<>());

		Map<String, String> map = meituanXiaodaiInfoList.stream().map(MeituanXiaodaiInfo::getProductId).collect(Collectors.toMap(k -> k, v -> v));
		InputStream resourceAsStream2 = Thread.currentThread().getContextClassLoader().getResourceAsStream("meituanStoreInfoRegisters.json");
		JSONArray info = JSON.parseObject(resourceAsStream2 , JSONArray.class);


		List<MeituanStoreInfoRegister> meituanXiaodaiInfos = JSON.parseArray(JSON.toJSONString(info), MeituanStoreInfoRegister.class);


		List<String> productIds = meituanXiaodaiInfos.stream().map(MeituanStoreInfoRegister::getSn).collect(Collectors.toList());
		List<String> productIds2 = new ArrayList<>();
		productIds.forEach(productId -> {
			if(!map.containsKey(productId)){
				productIds2.add(productId);
			}
		});
	log.info("productIds2:{}", com.alibaba.fastjson.JSON.toJSONString(productIds2, SerializerFeature.UseSingleQuotes));
	}

	@Test
	public void testWechat() throws Exception {
		httpExampleService.decodeOpenidSimple("");
	}

}