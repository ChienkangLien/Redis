package org.tutorial;

import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.Jedis;

public class JedisDemo {

	public static void main(String[] args) {
		try (Jedis jedis = new Jedis("192.168.191.136", 6379)) {
			jedis.auth("password");
			System.out.println(jedis.ping());

			// Key
			Set<String> keys = jedis.keys("*");
			for (String key : keys) {
				System.out.println("key: " + key);
			}

			// String
			// 設置鍵值對
			jedis.set("name", "lucy");

			// 獲取值
			String name = jedis.get("name");
			System.out.println(name); // 應該返回 "lucy"

			// 設置鍵值對及過期時間（秒）
			jedis.setex("temporary_key", 10, "temporary_value");

			// 獲取多個值
			jedis.mset("key1", "value1", "key2", "value2");
			List<String> values = jedis.mget("key1", "key2");
			System.out.println(values); // 應該返回 [value1, value2]

			// List
			// 推入列表
			jedis.lpush("mylist", "value1");
			jedis.lpush("mylist", "value2");

			// 獲取列表長度
			long length = jedis.llen("mylist");
			System.out.println(length); // 應該返回 2

			// 獲取列表的所有元素
			List<String> list = jedis.lrange("mylist", 0, -1);
			System.out.println(list); // 應該返回 [value2, value1]

			// Set
			// 添加元素到集合
			jedis.sadd("myset", "member1");
			jedis.sadd("myset", "member2");

			// 獲取集合的所有成員
			Set<String> set = jedis.smembers("myset");
			System.out.println(set); // 應該返回 [member1, member2]

			// 檢查成員是否在集合中
			boolean isMember = jedis.sismember("myset", "member1");
			System.out.println(isMember); // 應該返回 true

			// Hash
			// 設置散列表字段
			jedis.hset("myhash", "field1", "value1");
			jedis.hset("myhash", "field2", "value2");

			// 獲取散列表字段的值
			String value = jedis.hget("myhash", "field1");
			System.out.println(value); // 應該返回 "value1"

			// 獲取散列表的所有字段和值
			Map<String, String> hash = jedis.hgetAll("myhash");
			System.out.println(hash); // 應該返回 {field1=value1, field2=value2}

			// SortedSet
			// 添加元素到有序集合
			jedis.zadd("myzset", 1, "member1");
			jedis.zadd("myzset", 2, "member2");

			// 獲取有序集合的成員
			Set<String> zset = jedis.zrange("myzset", 0, -1);
			System.out.println(zset); // 應該返回 [member1, member2]

			// 獲取成員的分數
			Double score = jedis.zscore("myzset", "member1");
			System.out.println(score); // 應該返回 1.0
		}

	}
}
