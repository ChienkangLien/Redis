package org.tutorial;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.Transaction;

public class SecKillTask implements Runnable {

	public static void main(String[] args) {
		for (int i = 0; i < 3000; i++) {
			Thread thread = new Thread(new SecKillTask());
			thread.start();
		}
	}

	@Override
	public void run() {
		// userId隨機給定、prodId則固定
		String prodId = "prod01";
		String userId = new Random().nextInt(50000) + "";
//		doSecKill(userId, prodId);
		
		// ### 通過lua腳本，解決樂觀鎖更新衝突問題(庫存遺留)
		doSecKillByLua(userId, prodId);
	}

	public static boolean doSecKill(String userId, String prodId) {
		// 1.非空判斷
		if (userId == null || prodId == null) {
			System.out.println("uid和prodid為空");
			return false;
		}
		// 2.連接redis
//		Jedis jedis = new Jedis("192.168.191.138", 6379);
//		jedis.auth("password");
		// ### 通過連接池，解決連線超時問題
		JedisPool jedisPool = JedisPoolUtil.getJedisPool();
		Jedis jedis = jedisPool.getResource();

		// 3.拼接key
		// 3.1. 庫存key
		String skKey = "sk:" + prodId + ":qt";
		// 3.2. 秒殺成功用戶key
		String userKey = "sk:" + prodId + ":usr";

		// ### 監視庫存，解決併發安全問題(超賣)
		jedis.watch(skKey);

		// 4.獲取庫存，如果庫存null，秒殺還沒有開始
		String skValue = jedis.get(skKey);
		if (skValue == null) {
			System.out.println("秒殺還沒開始，請等待");
			jedis.close();
			return false;
		}

		// 5.判斷用戶是否重複操作
		Boolean sismember = jedis.sismember(userKey, userId);
		if (sismember) {
			System.out.println("該用戶已經搶過一次，請勿重複搶購");
			jedis.close();
			return false;
		}

		// 6. 判斷如果商品數量，庫存數量小於1，秒殺結束
		if (Integer.parseInt(skValue) <= 0) {
			System.out.println("該商品的庫存不足，秒殺失敗");
			jedis.close();
			return false;
		}

		// 7. 秒殺過程
//		// 7.1. 庫存-1
//		jedis.decr(skKey);
//		// 7.2. 用戶加入到set集合中
//		jedis.sadd(userKey, userId);

		// ### 使用事務，解決併發安全問題(超賣)
		Transaction multi = jedis.multi();
		// ### 組隊操作，解決併發安全問題(超賣)
		multi.decr(skKey);
		multi.sadd(userKey, userId);

		List<Object> results = multi.exec();
		if (results == null || results.size() == 0) {
			System.out.println("秒殺失敗");
			jedis.close();
			return false;
		}

		System.out.println("秒殺成功");
		jedis.close();
		return true;
	}

	public static boolean doSecKillByLua(String userId, String prodId) {
//		JedisPool jedisPool = JedisPoolUtil.getJedisPool();

		// 若有啟用redis-sentinel
		JedisSentinelPool jedisPool = JedisPoolUtil.getJedisSentinelPool();
		
		Jedis jedis = jedisPool.getResource();

		String sha = jedis.scriptLoad(SCRIPT);
		Object result =  jedis.evalsha(sha, 2, userId, prodId);

		String reString = String.valueOf(result);
		if ("0".equals(reString)) {
			System.out.println("已搶空");
		} else if ("1".equals(reString)) {
			System.out.println("搶購成功");
			jedis.close();
			return true;
		} else if ("2".equals(reString)) {
			System.out.println("該用戶已搶過");
		} else {
			System.out.println("搶購異常");
		}
		
		jedis.close();
		return false;
	}

	static final String SCRIPT = "local userId=KEYS[1];"
			+ "local prodId=KEYS[2];"
			+ "local qtKey=\"sk:\"..prodId..\":qt\";"
			+ "local userKey=\"sk:\"..prodId..\":usr\";"
			+ "local userExists=redis.call(\"sismember\",userKey,userId);"
			+ "if tonumber(userExists)==1 then"
			+ "  return 2;"
			+ "end;"
			+ "local num=redis.call(\"get\",qtKey);"
			+ "if tonumber(num)<=0 then"
			+ "  return 0;"
			+ "else"
			+ "  redis.call(\"decr\",qtKey);"
			+ "  redis.call(\"sadd\",userKey,userId);"
			+ "end;"
			+ "return 1;";

}

class JedisPoolUtil {
	private static JedisPool jedisPool;

	// 通過單例模式來定義jedisPool連接池
	public static JedisPool getJedisPool() {
		if (jedisPool == null) {
			synchronized (JedisPoolUtil.class) {
				if (jedisPool == null) {
					JedisPoolConfig poolConfig = new JedisPoolConfig();
					poolConfig.setMaxTotal(200);
					poolConfig.setMaxIdle(32);
					poolConfig.setMaxWait(Duration.ofSeconds(100));
					poolConfig.setBlockWhenExhausted(true);
					poolConfig.setTestOnBorrow(true);
					// 指定連接池的poolConfig，redis的IP地址，端口號，已經超時時間。
					jedisPool = new JedisPool(poolConfig, "192.168.191.138", 6379, 60000, "password");
				}
			}
		}
		return jedisPool;
	}

	// 釋放jedis連接
	public static void release(JedisPool jedisPool, Jedis jedis) {
		if (jedis != null) {
//			jedisPool.returnResource(jedis);
			jedis.close();
		}
	}
	
	// 若有啟用redis-sentinel
	private static JedisSentinelPool jedisSentinelPool;
	
	// 通過單例模式來定義jedisSentinelPool連接池
	public static JedisSentinelPool getJedisSentinelPool() {
		if (jedisSentinelPool == null) {
			synchronized (JedisSentinelPool.class) {
				if (jedisSentinelPool == null) {
					JedisPoolConfig poolConfig = new JedisPoolConfig();
					poolConfig.setMaxTotal(200);
					poolConfig.setMaxIdle(32);
					poolConfig.setMaxWait(Duration.ofSeconds(100));
					poolConfig.setBlockWhenExhausted(true);
					poolConfig.setTestOnBorrow(true);
					// 指定連接池的poolConfig，redisSentinel的IP地址，端口號。
					Set<String> sentinels = new HashSet<>();
				    sentinels.add("192.168.191.138:26379"); // Sentinel節點
				    String masterName = "mymaster"; // sentinel.conf中的主節點名稱
				    jedisSentinelPool = new JedisSentinelPool(masterName, sentinels, poolConfig, "password");
				}
			}
		}
		return jedisSentinelPool;
	}
	
	// 釋放jedis連接
	public static void release(JedisSentinelPool jedisSentinelPool, Jedis jedis) {
		if (jedis != null) {
//			jedisSentinelPool.returnResource(jedis);
			jedis.close();
		}
	}
}