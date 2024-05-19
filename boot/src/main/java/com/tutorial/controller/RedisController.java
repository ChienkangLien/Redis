package com.tutorial.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/redis")
public class RedisController {

	@Autowired
	RedisTemplate<String, String> redisTemplate;

	@GetMapping("/test")
	public void test() {
		System.out.println(redisTemplate.getConnectionFactory().getConnection().ping());

		// Key
		Set<String> keys = redisTemplate.keys("*");
		for (String key : keys) {
			System.out.println("key: " + key);
		}

		// String
		// 設置鍵值對
		redisTemplate.opsForValue().set("name", "lucy");

		// 獲取值
		String name = redisTemplate.opsForValue().get("name");
		System.out.println(name); // 應該返回 "lucy"

		// 設置鍵值對及過期時間（秒）
		redisTemplate.opsForValue().set("temporary_key", "temporary_value", 10, TimeUnit.SECONDS);

		// 獲取多個值
		redisTemplate.opsForValue().multiSet(Map.of("key1", "value1", "key2", "value2"));
		List<String> values = redisTemplate.opsForValue().multiGet(Arrays.asList("key1", "key2"));
		System.out.println(values); // 應該返回 [value1, value2]

		// List
		// 推入列表
		redisTemplate.opsForList().leftPush("mylist", "value1");
		redisTemplate.opsForList().leftPush("mylist", "value2");

		// 獲取列表長度
		long length = redisTemplate.opsForList().size("mylist");
		System.out.println(length); // 應該返回 2

		// 獲取列表的所有元素
		List<String> list = redisTemplate.opsForList().range("mylist", 0, -1);
		System.out.println(list); // 應該返回 [value2, value1]

		// Set
		// 添加元素到集合
		redisTemplate.opsForSet().add("myset", "member1");
		redisTemplate.opsForSet().add("myset", "member2");

		// 獲取集合的所有成員
		Set<String> set = redisTemplate.opsForSet().members("myset");
		System.out.println(set); // 應該返回 [member1, member2]

		// 檢查成員是否在集合中
		boolean isMember = redisTemplate.opsForSet().isMember("myset", "member1");
		System.out.println(isMember); // 應該返回 true

		// Hash
		// 設置散列表字段
		redisTemplate.opsForHash().put("myhash", "field1", "value1");
		redisTemplate.opsForHash().put("myhash", "field2", "value2");

		// 獲取散列表字段的值
		String value = (String) redisTemplate.opsForHash().get("myhash", "field1");
		System.out.println(value); // 應該返回 "value1"

		// 獲取散列表的所有字段和值
		Map<Object, Object> hash = redisTemplate.opsForHash().entries("myhash");
		System.out.println(hash); // 應該返回 {field1=value1, field2=value2}

		// SortedSet
		// 添加元素到有序集合
		redisTemplate.opsForZSet().add("myzset", "member1", 1);
		redisTemplate.opsForZSet().add("myzset", "member2", 2);

		// 獲取有序集合的成員
		Set<String> zset = redisTemplate.opsForZSet().range("myzset", 0, -1);
		System.out.println(zset); // 應該返回 [member1, member2]

		// 獲取成員的分數
		Double score = redisTemplate.opsForZSet().score("myzset", "member1");
		System.out.println(score); // 應該返回 1.0
	}
}
