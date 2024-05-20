package com.tutorial.controller;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tutorial.service.SecKillService;

@RestController
@RequestMapping("/secKill")
public class SecKillController {

	// 兩個表
	// 一、商品庫存，key sk:prodId:qt / string 剩餘個數
	// 二、秒殺成功者清單，key sk:prodId:usr / set 成功者的userId

	@Autowired
	SecKillService secKillService;

	@GetMapping
	public String entry() {

		// userId隨機給定、prodId則固定
		String userId = new Random().nextInt(50000) + "";
		String prodId = "prod01";

		boolean isSuccess = secKillService.doSecKillSafe(userId, prodId);
		return isSuccess ? "成功" : "失敗";
	}
}
