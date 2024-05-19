package org.tutorial;

import java.util.Random;

import redis.clients.jedis.Jedis;

public class PhoneCode {
	
	// 案例：驗證碼
	// 輸入手機號，點擊發送後隨機生成6位數字，兩分鐘有效
	// 輸入驗證碼，點擊驗證，返回成功或失敗
	// 每個手機號每天只能輸入三次

	public static void main(String[] args) {
		verifyCode("0912345678");
		getRedisCode("0912345678", "123456");
	}

	/*
	 * 驗證碼校驗
	 */
	public static void getRedisCode(String phone, String code) {
		try (Jedis jedis = new Jedis("192.168.191.136", 6379)) {
			jedis.auth("password");

			// 驗證碼key
			String codeKey = "VerifyCode:" + phone + ":code";
			String redisCode = jedis.get(codeKey);
			System.out.println("jedis get " + codeKey + " : " + redisCode);

			// 判斷
			if (redisCode != null && redisCode.equals(code)) {
				System.out.println("驗證成功");
			} else {
				System.out.println("驗證失敗");
			}
		}
	}

	/*
	 * 驗證碼放到redis並設置兩分鐘有效
	 */
	public static void verifyCode(String phone) {
		try (Jedis jedis = new Jedis("192.168.191.136", 6379)) {
			jedis.auth("password");

			// 拼接key
			// 手機發送次數key
			String countKey = "VerifyCode:" + phone + ":count";
			// 驗證碼key
			String codeKey = "VerifyCode:" + phone + ":code";

			// 每個手機每天只能發送三次
			String count = jedis.get(countKey);
			System.out.println("jedis get " + countKey + " : " + count);
			if (count == null) {
				// 沒有發送次數，接著第一次發送
				jedis.setex(countKey, 24 * 60 * 60, "1");
				System.out.println("jedis set " + countKey + " : " + "1");
			} else if (Integer.parseInt(count) <= 2) {
				// 發送次數加一
				jedis.incr(countKey);
				System.out.println("jedis incr " + countKey);

			} else {
				// 發送三次
				System.out.println("今天已發送三次驗證碼");
				return;
			}

			// 發送的驗證碼放到redis
			String vcode = getRandomCode();
			jedis.setex(codeKey, 1, vcode);
			System.out.println("jedis set " + codeKey + " : " + vcode);
		}
	}

	/*
	 * 生成6位數字驗證碼
	 */
	public static String getRandomCode() {
		Random random = new Random();
		String code = "";
		for (int i = 0; i < 6; i++) {
			int rand = random.nextInt(10);
			code += rand;
		}
		System.out.println("生成驗證碼: " + code);
		return code;
	}
}
