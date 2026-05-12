package com.qingtu.agent.controller;

import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.config.LocationConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/location")
@RequiredArgsConstructor
@Slf4j
public class LocationController {

    private final LocationConfig locationConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/reverse")
    public CommonResult<?> reverseGeocode(@RequestParam Double lat, @RequestParam Double lng) {
        try {
            String url = locationConfig.getApiUrl() + 
                "?location=" + lng + "," + lat + 
                "&key=" + locationConfig.getQqKey() + 
                "&get_poi=0";
            
            String response = restTemplate.getForObject(url, String.class);
            
            if (response != null && response.contains("\"status\":0")) {
                com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSONObject.parseObject(response);
                com.alibaba.fastjson2.JSONObject result = json.getJSONObject("result");
                if (result != null && result.containsKey("ad_info")) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("city", result.getJSONObject("ad_info").getString("city"));
                    data.put("province", result.getJSONObject("ad_info").getString("province"));
                    data.put("district", result.getJSONObject("ad_info").getString("district"));
                    data.put("address", result.getString("address"));
                    return CommonResult.success(data);
                }
            }
            return CommonResult.fail("解析失败");
        } catch (Exception e) {
            log.error("逆地理编码失败: {}", e.getMessage());
            return CommonResult.fail("定位服务异常");
        }
    }

    @GetMapping("/geocode")
    public CommonResult<?> geocode(@RequestParam String address) {
        try {
            String url = locationConfig.getApiUrl() + 
                "?address=" + address + 
                "&key=" + locationConfig.getQqKey();
            
            String response = restTemplate.getForObject(url, String.class);
            
            if (response != null && response.contains("\"status\":0")) {
                com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSONObject.parseObject(response);
                com.alibaba.fastjson2.JSONObject result = json.getJSONObject("result");
                if (result != null && result.containsKey("location")) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("lat", result.getJSONObject("location").getDouble("lat"));
                    data.put("lng", result.getJSONObject("location").getDouble("lng"));
                    data.put("city", result.getJSONObject("ad_info").getString("city"));
                    return CommonResult.success(data);
                }
            }
            return CommonResult.fail("解析失败");
        } catch (Exception e) {
            log.error("地理编码失败: {}", e.getMessage());
            return CommonResult.fail("服务异常");
        }
    }
}