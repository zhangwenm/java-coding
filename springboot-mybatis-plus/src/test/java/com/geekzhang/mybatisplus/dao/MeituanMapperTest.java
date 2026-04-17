package com.geekzhang.mybatisplus.dao;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.geekzhang.mybatisplus.entity.MeituanXiaodaiInfo;
import com.geekzhang.mybatisplus.mapper.MeituanXiaodaiInfoMapper;
import com.geekzhang.mybatisplus.util.HttpClientUtil;
import com.geekzhang.mybatisplus.vo.RobotDto;
import com.geekzhang.mybatisplus.vo.RobotDtoQuery;
import com.geekzhang.mybatisplus.vo.TaskQueryDto;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@SpringBootTest
@Transactional
public class MeituanMapperTest {

	@Autowired
	private MeituanXiaodaiInfoMapper meituanXiaodaiInfoMapper;
	
	@Autowired
	private HttpClientUtil httpClientUtil;
	
	private static final String ROBOT_ONLINE_API = "https://openapi.yunjichina.com.cn/openapi/v1/robot/online";
	private static final int BATCH_SIZE = 100;


	@Test
	@Rollback(false)
	public void testQuery() throws Exception {
        MeituanXiaodaiInfo meituanXiaodaiInfo = new MeituanXiaodaiInfo();

        LambdaUpdateWrapper<MeituanXiaodaiInfo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(MeituanXiaodaiInfo::getId, 626117)
                .set(MeituanXiaodaiInfo::getOnline, 1);

        meituanXiaodaiInfoMapper.update(null, updateWrapper);
	}

	@Test
	public void testSelectList() {
		List<MeituanXiaodaiInfo> list = meituanXiaodaiInfoMapper.selectList(null);
		log.info("查询结果数量: {}", list.size());
		list.forEach(info -> log.info("ID: {}, productId: {}, hotelName: {}, state: {}",
				info.getId(), info.getProductId(), info.getHotelName(), info.getState()));
		assert list != null : "查询结果为空";
	}

	@Test
    @Rollback(false)
	public void testSelectListWithOnlineStatus() {
		List<MeituanXiaodaiInfo> allRecords = meituanXiaodaiInfoMapper.selectList(
				new LambdaQueryWrapper<MeituanXiaodaiInfo>()
						.isNotNull(MeituanXiaodaiInfo::getProductId)
        );
		
		log.info("需要更新在线状态的设备总数: {}", allRecords.size());
		
		if (CollectionUtils.isEmpty(allRecords)) {
			log.info("没有需要处理的设备");
			return;
		}
		
		List<List<MeituanXiaodaiInfo>> batches = Lists.partition(allRecords, BATCH_SIZE);
		int totalUpdated = 0;
		
		for (int i = 0; i < batches.size(); i++) {
			List<MeituanXiaodaiInfo> batch = batches.get(i);
			log.info("处理第 {}/{} 批, 数量: {}", i + 1, batches.size(), batch.size());
			
			List<String> productIdList = batch.stream()
					.map(MeituanXiaodaiInfo::getProductId)
					.collect(Collectors.toList());
			
			Map<String, Boolean> onlineStatusMap = queryRobotOnlineStatus(productIdList);
			
			if (!onlineStatusMap.isEmpty()) {
				for (MeituanXiaodaiInfo info : batch) {
					Boolean online = onlineStatusMap.get(info.getProductId());
					if (online != null) {
						LambdaUpdateWrapper<MeituanXiaodaiInfo> updateWrapper = new LambdaUpdateWrapper<>();
						updateWrapper.eq(MeituanXiaodaiInfo::getId, info.getId())
								.set(MeituanXiaodaiInfo::getOnline, online ? 1 : 0)
                                .set(MeituanXiaodaiInfo::getUpdateTime, info.getUpdateTime());

                        if(!info.getProductId().startsWith("Y")){
                            updateWrapper.set(MeituanXiaodaiInfo::getYuncang, 1);
                        }
                        if(StringUtils.isNotBlank(info.getUserId())){
                            String[] mobile = info.getUserId().split(":");
                            if(mobile.length>1){
                                updateWrapper.set(MeituanXiaodaiInfo::getMobile, mobile[1]);
                            }
                        }
						meituanXiaodaiInfoMapper.update(null, updateWrapper);
                        if(online){
                            log.info("更新设备 {} 在线状态: {}", info.getProductId(), online);
                        }else {
                            log.info("更新设备 {} 在线状态: {}", info.getProductId(), online);
                        }
						totalUpdated++;

					}
				}
			}
			
			if (i < batches.size() - 1) {
				try {
					log.info("批次处理完成，等待3秒后继续...");
					TimeUnit.SECONDS.sleep(3);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					log.warn("睡眠被中断");
				}
			}
		}
		
		log.info("在线状态更新完成，共更新 {} 条记录", totalUpdated);
	}
	
	private Map<String, Boolean> queryRobotOnlineStatus(List<String> productIdList) {
		Map<String, Boolean> result = new HashMap<>();
		
		if (CollectionUtils.isEmpty(productIdList)) {
			return result;
		}
		
		try {
			RobotDtoQuery query = new RobotDtoQuery();
			query.setProductIdList(productIdList);
			
			log.info("调用在线状态接口, productId数量: {}", productIdList.size());
			ResponseEntity<String> response = httpClientUtil.postJson(ROBOT_ONLINE_API, query, String.class);
			
			if (response != null && response.getBody() != null) {
				JSONObject json = JSON.parseObject(response.getBody());
				Object data = json.get("data");
				
				if (data instanceof JSONArray) {
					JSONArray dataArray = (JSONArray) data;
					for (int i = 0; i < dataArray.size(); i++) {
						JSONObject item = dataArray.getJSONObject(i);
						String productId = item.getString("productId");
						Long ts = item.getLong("ts");
						Boolean online = ts != null && System.currentTimeMillis() - ts < 1800000L;
						result.put(productId, online);
					}
				}
				log.info("在线状态查询成功, 返回 {} 条记录", result.size());
			}
		} catch (Exception e) {
			log.error("查询在线状态失败: {}", e.getMessage(), e);
		}
		
		return result;
	}

	@Test
	public void testSelectByProductId() {
		String productId = "test_product_001";
		MeituanXiaodaiInfo query = new MeituanXiaodaiInfo();
		query.setProductId(productId);
		List<MeituanXiaodaiInfo> list = meituanXiaodaiInfoMapper.selectList(
				new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MeituanXiaodaiInfo>()
						.eq(MeituanXiaodaiInfo::getProductId, productId));
		log.info("根据productId查询结果数量: {}", list.size());
		list.forEach(info -> log.info("ID: {}, productId: {}, amount: {}",
				info.getId(), info.getProductId(), info.getAmount()));
		assert list != null : "查询结果为空";
	}

	/**
	 * state=0 且 yuncang=1 的设备：
	 *   online=1：全部改为 state=2
	 *   online=0：超过300台时，第301台起改为 state=2，前300台保留
	 */
	@Test
	@Rollback(false)
	public void testUpdateStateForYuncang() {
		Random random = new Random();
		int[] days = {26, 27, 28};
		LocalDateTime now = LocalDateTime.now();




		// --- 2. online=0：超过300台才更新第301台起 ---
		List<MeituanXiaodaiInfo> offlineList = meituanXiaodaiInfoMapper.selectList(
				new LambdaQueryWrapper<MeituanXiaodaiInfo>()
						.eq(MeituanXiaodaiInfo::getState, 0)
						.eq(MeituanXiaodaiInfo::getYuncang, 1)
						.eq(MeituanXiaodaiInfo::getOnline, 0)
						.orderByAsc(MeituanXiaodaiInfo::getId)
		);
		log.info("online=0 满足条件设备数: {}", offlineList.size());

		if (offlineList.size() <= 300) {
			log.info("online=0 总数不超过300，无需修改");
			return;
		}

		List<MeituanXiaodaiInfo> toUpdate = offlineList.subList(300, offlineList.size());
		int offlineRows = 0;
		for (MeituanXiaodaiInfo info : toUpdate) {
			LocalDateTime offlineUpdateTime = randomUpdateTime(now, days, random);
			LambdaUpdateWrapper<MeituanXiaodaiInfo> offlineWrapper = new LambdaUpdateWrapper<>();
			offlineWrapper.eq(MeituanXiaodaiInfo::getId, info.getId())
					.set(MeituanXiaodaiInfo::getState, 2)
					.set(MeituanXiaodaiInfo::getUpdateTime, offlineUpdateTime);
			meituanXiaodaiInfoMapper.update(null, offlineWrapper);
			offlineRows++;
		}
		log.info("online=0 共更新 {} 条为 state=2（保留前300条，每条update_time独立随机）", offlineRows);
	}

	/**
	 * 从离线 + 非货柜(yuncang=1) + 初始化(state=0) 中随机抽400条改为同意(state=1)
	 * update_time 按 3:2:1 衰减分布在2/3/4号（2号200条、3号133条、4号67条），时分秒随机
	 */
	@Test
	@Rollback(false)
	public void testApprove400OfflineYuncang() {
		Random random = new Random();
		LocalDateTime now = LocalDateTime.now();

		// 查询满足条件的所有设备
		List<MeituanXiaodaiInfo> list = meituanXiaodaiInfoMapper.selectList(
				new LambdaQueryWrapper<MeituanXiaodaiInfo>()
						.eq(MeituanXiaodaiInfo::getState, 0)
						.eq(MeituanXiaodaiInfo::getYuncang, 1)
						.eq(MeituanXiaodaiInfo::getOnline, 1)
		);
		log.info("满足条件的设备总数: {}", list.size());

		if (list.size() < 400) {
			log.warn("满足条件的设备不足400台，实际: {}，全部更新", list.size());
		}

		// 随机打乱，取前400条
		Collections.shuffle(list, random);
		List<MeituanXiaodaiInfo> selected = list.subList(0, Math.min(800, list.size()));

		// 按 3:2:1 分配到 2/3/4 号
		int total = selected.size();
		int day2Count = (int) Math.round(total * 3.0 / 6); // ~200
		int day3Count = (int) Math.round(total * 2.0 / 6); // ~133
		int day4Count = total - day2Count - day3Count;     // 剩余 ~67

		int[] assignedDays = new int[total];
		for (int i = 0; i < day2Count; i++) assignedDays[i] = 2;
		for (int i = day2Count; i < day2Count + day3Count; i++) assignedDays[i] = 3;
		for (int i = day2Count + day3Count; i < total; i++) assignedDays[i] = 4;

		log.info("日期分配：2号{}条, 3号{}条, 4号{}条", day2Count, day3Count, day4Count);

		// 逐条更新
		for (int i = 0; i < selected.size(); i++) {
			int day = assignedDays[i];
			LocalDateTime updateTime = LocalDateTime.of(
					now.getYear(), now.getMonth(), day,
					random.nextInt(24), random.nextInt(60), random.nextInt(60));
			LambdaUpdateWrapper<MeituanXiaodaiInfo> wrapper = new LambdaUpdateWrapper<>();
			wrapper.eq(MeituanXiaodaiInfo::getId, selected.get(i).getId())
					.set(MeituanXiaodaiInfo::getState, 1)
					.set(MeituanXiaodaiInfo::getUpdateTime, updateTime);
			meituanXiaodaiInfoMapper.update(null, wrapper);
		}
		log.info("完成，共更新 {} 条为 state=1(同意)", selected.size());
	}

	/**
	 * state=0, yuncang=1, user_id NOT LIKE '%null%'：
	 *   随机抽274条 → state=1，随机抽280条 → state=2，更新时间保持不变
	 */
	@Test
	@Rollback(false)
	public void testUpdateStateKeepTime() {
		Random random = new Random();

		// 查询满足条件的所有设备
		List<MeituanXiaodaiInfo> list = meituanXiaodaiInfoMapper.selectList(
				new LambdaQueryWrapper<MeituanXiaodaiInfo>()
						.eq(MeituanXiaodaiInfo::getState, 0)
						.eq(MeituanXiaodaiInfo::getYuncang, 1)
						.notLike(MeituanXiaodaiInfo::getUserId, "null")
		);
		log.info("满足条件的设备总数: {}", list.size());

		int need = 274 + 280;
		if (list.size() < need) {
			log.warn("数据不足{}条，实际: {}，请确认", need, list.size());
			return;
		}

		// 随机打乱，前274条→state=1，接着280条→state=2
		Collections.shuffle(list, random);
		List<MeituanXiaodaiInfo> state1List = list.subList(0, 274);
		List<MeituanXiaodaiInfo> state2List = list.subList(274, 554);

		// 更新 state=1，显式写回原 update_time 防止 DB 自动刷新
		for (MeituanXiaodaiInfo info : state1List) {
			LambdaUpdateWrapper<MeituanXiaodaiInfo> wrapper = new LambdaUpdateWrapper<>();
			wrapper.eq(MeituanXiaodaiInfo::getId, info.getId())
					.set(MeituanXiaodaiInfo::getState, 1)
					.set(MeituanXiaodaiInfo::getUpdateTime, info.getUpdateTime());
			meituanXiaodaiInfoMapper.update(null, wrapper);
		}
		log.info("state=1(同意) 更新完成，共{}条", state1List.size());

		// 更新 state=2，显式写回原 update_time
		for (MeituanXiaodaiInfo info : state2List) {
			LambdaUpdateWrapper<MeituanXiaodaiInfo> wrapper = new LambdaUpdateWrapper<>();
			wrapper.eq(MeituanXiaodaiInfo::getId, info.getId())
					.set(MeituanXiaodaiInfo::getState, 2)
					.set(MeituanXiaodaiInfo::getUpdateTime, info.getUpdateTime());
			meituanXiaodaiInfoMapper.update(null, wrapper);
		}
		log.info("state=2(不同意) 更新完成，共{}条", state2List.size());
	}

	private LocalDateTime randomUpdateTime(LocalDateTime base, int[] days, Random random) {
		int day = days[random.nextInt(days.length)];
		return LocalDateTime.of(base.getYear(), base.getMonth(), day,
				random.nextInt(24), random.nextInt(60), random.nextInt(60));
	}

	private static final String TASK_DETAIL_API = "https://openapi.yunjichina.com.cn/openapi/v3/task/detail_new";
	private static final String TASK_TOKEN = "89eb8f8ae1284175a1fe0451d457101d";

	/**
	 * 遍历所有设备，分批调用任务接口，获取每个设备最新任务的 startTime 写入 last_task_time
	 */
	@Test
	@Rollback(false)
	public void testFetchLastTaskTime() throws InterruptedException {
		// 查询非货柜（yuncang=1）且有 productId 的设备
		List<MeituanXiaodaiInfo> allDevices = meituanXiaodaiInfoMapper.selectList(
				new LambdaQueryWrapper<MeituanXiaodaiInfo>()
						.isNotNull(MeituanXiaodaiInfo::getProductId)
						.eq(MeituanXiaodaiInfo::getYuncang, 1)
		);
		log.info("设备总数: {}", allDevices.size());

		String endTime = LocalDateTime.now().toLocalDate().toString();
		int updated = 0;

		// 逐台查询，count=1 只取最新一条
		for (int i = 0; i < allDevices.size(); i++) {
			MeituanXiaodaiInfo device = allDevices.get(i);
			String productId = device.getProductId();

			MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
			params.add("token", TASK_TOKEN);
			params.add("appname", "test");
			params.add("productId", productId);
			params.add("startTime", "2026-02-05");
			params.add("endTime", endTime);
			params.add("robotType", "NVWA,GEGE,QL,SC,WT,UP");
			params.add("start", 0);
			params.add("count", 1);

			try {
				ResponseEntity<String> response = httpClientUtil.postForm(TASK_DETAIL_API, params, null, String.class);
				if (response == null || response.getBody() == null) {
					log.warn("[{}/{}] {} 接口返回为空", i + 1, allDevices.size(), productId);
					continue;
				}

				JSONObject json = JSON.parseObject(response.getBody());
				if (json.getIntValue("errcode") != 0) {
					log.warn("[{}/{}] {} 接口错误: {}", i + 1, allDevices.size(), productId, json.getString("errmsg"));
					continue;
				}

				JSONArray data = json.getJSONObject("result").getJSONArray("data");
				if (data == null || data.isEmpty()) {
					log.info("[{}/{}] {} 无任务数据", i + 1, allDevices.size(), productId);
					continue;
				}

				String startTimeStr = data.getJSONObject(0).getString("startTime");
				if (startTimeStr == null) continue;

				// 格式如 "2026-03-02T14:33:13.000+0800"
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
				LocalDateTime lastTaskTime = OffsetDateTime.parse(startTimeStr, formatter).toLocalDateTime();
				LambdaUpdateWrapper<MeituanXiaodaiInfo> wrapper = new LambdaUpdateWrapper<>();
				wrapper.eq(MeituanXiaodaiInfo::getId, device.getId())
						.set(MeituanXiaodaiInfo::getLastTaskTime, lastTaskTime)
						.set(MeituanXiaodaiInfo::getUpdateTime, device.getUpdateTime());
				meituanXiaodaiInfoMapper.update(null, wrapper);
				updated++;
				log.info("[{}/{}] {} 最新任务时间: {}", i + 1, allDevices.size(), productId, lastTaskTime);
			} catch (Exception e) {
				log.error("[{}/{}] {} 请求失败: {}", i + 1, allDevices.size(), productId, e.getMessage());
			}

			// 每500台休眠10秒，避免请求过快
			if ((i + 1) % 500 == 0) {
				TimeUnit.SECONDS.sleep(10);
			}
		}

		log.info("last_task_time 写入完成，共更新 {} 条", updated);
	}

	@Test
	public void testSelectByState() {
		Integer state = 0;
		List<MeituanXiaodaiInfo> list = meituanXiaodaiInfoMapper.selectList(
				new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MeituanXiaodaiInfo>()
						.eq(MeituanXiaodaiInfo::getState, state)
						.orderByDesc(MeituanXiaodaiInfo::getCreateTime));
		log.info("根据state查询结果数量: {}", list.size());
		list.forEach(info -> log.info("ID: {}, state: {}, createTime: {}",
				info.getId(), info.getState(), info.getCreateTime()));
		assert list != null : "查询结果为空";
	}
}