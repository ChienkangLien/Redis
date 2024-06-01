package com.tutorial.controller;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LockController {
	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	// 先在redis中執行set num 0
	// 問題：線程a執行業務時鎖過期，線程b搶到鎖後執行業務，線程a業務處理完欲釋放鎖時鎖卻在b身上
	// 解決如下
	// 將值設定成唯一的UUID值，透過呼叫delete方法刪除鎖
	// 刪除先前首先需要判斷被刪除的鎖的值和目前的UUID值是否相等
	// 這樣就可以防止鎖被其他線程誤刪的情況
	@RequestMapping("/lock/test")
	public String testLock() throws InterruptedException {
		String uuid = UUID.randomUUID().toString();
		Integer num = null;
		// 取得鎖，成功則返回true
		Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 10, TimeUnit.SECONDS);
		if (lock) {
			// 取得num後加一
			String value = redisTemplate.opsForValue().get("num");
			if (!StringUtils.hasLength(value)) {
				return "num 為空";
			}
			num = Integer.parseInt(value);
			redisTemplate.opsForValue().set("num", ++num + "");
			// 釋放鎖
			// 判斷UUID是否一樣
			String lockUUID = redisTemplate.opsForValue().get("lock");
			if (uuid.equalsIgnoreCase(lockUUID)) {
				redisTemplate.delete("lock");
			}
		} else {
			// 取得失敗，每隔0.1秒重試
			Thread.sleep(100);
			testLock();
		}
		return String.valueOf(num);
	}

	// 以上述問題延伸：刪除操作缺乏原子性
	// 線程a業務處理完也比較完UUID、欲釋放鎖的當下剛好過期，這個當下鎖被線程b拿到
	// 解決如下
	// 透過lua實現原子性
	@RequestMapping("/lock/testLua")
	public String testLockLua() throws InterruptedException {
		String uuid = UUID.randomUUID().toString();
		Integer num = null;
		// 取得鎖，成功則返回true
		Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 10, TimeUnit.SECONDS);
		if (lock) {
			// 取得num後加一
			String value = redisTemplate.opsForValue().get("num");
			if (!StringUtils.hasLength(value)) {
				return "num 為空";
			}
			num = Integer.parseInt(value);
			redisTemplate.opsForValue().set("num", ++num + "");
			// 釋放鎖
			// 判斷UUID是否一樣
			// 透過lua腳本
			DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
			redisScript.setScriptText(SCRIPT);
			redisScript.setResultType(Long.class);
			List<String> keys = Collections.singletonList("lock");
			Long result = redisTemplate.execute(redisScript, keys, uuid);
			if (result == 1) {
				System.out.println("釋放成功");
			} else {
				System.out.println("釋放失敗");
			}
		} else {
			// 取得失敗，每隔0.1秒重試
			Thread.sleep(100);
			testLock();
		}
		return String.valueOf(num);
	}

	static final String SCRIPT ="if redis.call(\"get\", KEYS[1]) == ARGV[1]"
			+ "	then return redis.call(\"del\", KEYS[1]);"
			+ "else"
			+ "	return 0;"
			+ "end;";
}
