package com.tutorial.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

@Service
public class SecKillService {

	@Autowired
	RedisTemplate<String, String> redisTemplate;

	/*
	 * 秒殺過程，高併發安全
	 */
	public boolean doSecKillSafe(String userId, String prodId) {

		// 1.非空判斷
		if (userId == null || prodId == null) {
			return false;
		}

		// 2.拼接key
		String qtKey = "sk:" + prodId + ":qt";
		String usrKey = "sk:" + prodId + ":usr";

		return redisTemplate.execute(new SessionCallback<Boolean>() {
			@Override
			public Boolean execute(RedisOperations operations) {
				// 監視庫存
				operations.watch(qtKey);

				// 3.獲取庫存，如果庫存為空，秒殺尚未開始
				String qt = (String) operations.opsForValue().get(qtKey);
				if (qt == null) {
					System.out.println("秒殺尚未開始");
					operations.unwatch();
					return false;
				}

				// 4.判斷用戶是否重複秒殺
				if (operations.opsForSet().isMember(usrKey, userId)) {
					System.out.println("已經秒殺成功，不得重複");
					operations.unwatch();
					return false;
				}

				// 5.判斷庫存，若小於1則結束
				if (Integer.valueOf(qt) < 1) {
					System.out.println("秒殺結束");
					operations.unwatch();
					return false;
				}

				// 6.秒殺過程
				operations.multi();
				// 6.1庫存-1
				operations.opsForValue().decrement(qtKey);
				// 6.2將秒殺成功用戶加入清單
				operations.opsForSet().add(usrKey, userId);

				List<Object> results = operations.exec();
				if (results == null || results.isEmpty()) {
					System.out.println("秒殺失敗");
					return false;
				}

				System.out.println("秒殺成功");
				return true;
			}
		});

	}

	/*
	 * 秒殺過程
	 */
	public boolean doSecKill(String userId, String prodId) {

		// 1.非空判斷
		if (userId == null || prodId == null) {
			return false;
		}

		// 2.拼接key
		String qtKey = "sk:" + prodId + ":qt";
		String usrKey = "sk:" + prodId + ":usr";

		// 3.獲取庫存，如果庫存為空，秒殺尚未開始
		String qt = redisTemplate.opsForValue().get(qtKey);
		if (qt == null) {
			System.out.println("秒殺尚未開始");
			return false;
		}

		// 4.判斷用戶是否重複秒殺
		if (redisTemplate.opsForSet().isMember(usrKey, userId)) {
			System.out.println("已經秒殺成功，不得重複");
			return false;
		}

		// 5.判斷庫存，若小於1則結束
		if (Integer.valueOf(qt) < 1) {
			System.out.println("秒殺結束");
			return false;
		}

		// 6.秒殺過程
		// 6.1庫存-1
		redisTemplate.opsForValue().decrement(qtKey);
		// 6.2將秒殺成功用戶加入清單
		redisTemplate.opsForSet().add(usrKey, userId);

		System.out.println("秒殺成功");
		return true;
	}
}
