package org.tutorial;

import java.util.Arrays;
import java.util.UUID;

import redis.clients.jedis.JedisCluster;

public class Lock {

	public static void main(String[] args) {
//		doLock();
		doLockLua();
	}
	
	// 先在redis中執行set num 0
	// 問題：線程a執行業務時鎖過期，線程b搶到鎖後執行業務，線程a業務處理完欲釋放鎖時鎖卻在b身上
	// 解決如下
	// 將值設定成唯一的UUID值，透過呼叫delete方法刪除鎖
	// 刪除先前首先需要判斷被刪除的鎖的值和目前的UUID值是否相等
	// 這樣就可以防止鎖被其他線程誤刪的情況
	public static void doLock() {
		JedisCluster jedis = JedisPoolUtil.getJedisCluster();
		String uuid = UUID.randomUUID().toString();
		Integer num = null;
		try {
			// 取得鎖，成功則返回true
			String lockResult = jedis.set("lock", uuid, "NX", "EX", 10);
			if ("OK".equals(lockResult)) {
				// 取得num後加一
				String value = jedis.get("num");
				if (value == null || value.isEmpty()) {
					System.out.println("num 為空");
				}
				num = Integer.parseInt(value);
				jedis.set("num", String.valueOf(++num));
				// 釋放鎖
				// 判斷UUID是否一樣
				String lockUUID = jedis.get("lock");
				if (uuid.equalsIgnoreCase(lockUUID)) {
					jedis.del("lock");
				}
			} else {
				// 取得失敗，每隔0.1秒重試
				Thread.sleep(100);
				doLock();
			}
			System.out.println(num);
			jedis.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// 以上述問題延伸：刪除操作缺乏原子性
	// 線程a業務處理完也比較完UUID、欲釋放鎖的當下剛好過期，這個當下鎖被線程b拿到
	// 解決如下
	// 透過lua實現原子性
	public static void doLockLua() {
		JedisCluster jedis = JedisPoolUtil.getJedisCluster();
		String uuid = UUID.randomUUID().toString();
		Integer num = null;
		try {
			// 取得鎖，成功則返回true
			String lockResult = jedis.set("lock", uuid, "NX", "EX", 10);
			if ("OK".equals(lockResult)) {
				// 取得num後加一
				String value = jedis.get("num");
				if (value == null || value.isEmpty()) {
					System.out.println("num 為空");
				}
				num = Integer.parseInt(value);
				jedis.set("num", String.valueOf(++num));
				// 釋放鎖
				// 判斷UUID是否一樣
				String sha = jedis.scriptLoad(SCRIPT, "lock");
				Long result = (Long) jedis.evalsha(sha, Arrays.asList("lock"), Arrays.asList(uuid));
				if (result == 1) {
					System.out.println("釋放成功");
				} else {
					System.out.println("釋放失敗");
				}
			} else {
				// 取得失敗，每隔0.1秒重試
				Thread.sleep(100);
				doLockLua();
			}
			System.out.println(num);
			jedis.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static final String SCRIPT ="if redis.call(\"get\", KEYS[1]) == ARGV[1]"
			+ "	then return redis.call(\"del\", KEYS[1]);"
			+ "else"
			+ "	return 0;"
			+ "end;";
}
