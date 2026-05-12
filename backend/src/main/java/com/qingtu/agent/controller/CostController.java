package com.qingtu.agent.controller;

import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.service.CostService;
import com.qingtu.agent.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 消费记录控制器
 *
 * 提供消费记录管理、账单导入、消费统计等功能
 *
 * @author 青途智伴技术团队
 */
@RestController
@RequestMapping("/cost")
@RequiredArgsConstructor
public class CostController {

    private final CostService costService;
    private final JwtUtil jwtUtil;

    /**
     * 分页查询消费记录
     */
    @GetMapping("/list")
    public CommonResult<?> listCostRecords(HttpServletRequest request,
                                            @RequestParam(required = false) String category,
                                            @RequestParam(required = false) String startDate,
                                            @RequestParam(required = false) String endDate,
                                            @RequestParam(required = false) Integer year,
                                            @RequestParam(required = false) Integer month,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        Long userId = getUserIdFromRequest(request);
        return costService.listCostRecords(userId, category, startDate, endDate, year, month, page, size);
    }

    /**
     * 添加消费记录
     */
    @PostMapping("/add")
    public CommonResult<?> addCostRecord(HttpServletRequest request,
                                          @RequestBody Map<String, Object> params) {
        Long userId = getUserIdFromRequest(request);
        BigDecimal amount = new BigDecimal(params.get("amount").toString());
        String category = params.get("category").toString();
        String remark = params.getOrDefault("remark", "").toString();
        String merchantName = params.getOrDefault("merchantName", "").toString();
        String tradeTime = params.getOrDefault("tradeTime", "").toString();
        return costService.addCostRecord(userId, amount, category, remark, merchantName, tradeTime);
    }

    /**
     * 删除消费记录
     */
    @DeleteMapping("/{id}")
    public CommonResult<?> deleteCostRecord(HttpServletRequest request, @PathVariable Long id) {
        Long userId = getUserIdFromRequest(request);
        return costService.deleteCostRecord(userId, id);
    }

    /**
     * 导入账单
     */
    @PostMapping("/import")
    public CommonResult<?> importBill(HttpServletRequest request,
                                       @RequestParam MultipartFile file,
                                       @RequestParam String source) {
        Long userId = getUserIdFromRequest(request);
        return costService.importBill(userId, file, source);
    }

    /**
     * OCR识别账单
     */
    @PostMapping("/ocr")
    public CommonResult<?> ocrBill(HttpServletRequest request,
                                    @RequestParam MultipartFile file) {
        Long userId = getUserIdFromRequest(request);
        return costService.ocrBill(userId, file);
    }

    /**
     * 获取月度消费统计
     */
    @GetMapping("/monthly")
    public CommonResult<?> getMonthlyStatistics(HttpServletRequest request,
                                                @RequestParam int year,
                                                @RequestParam int month) {
        Long userId = getUserIdFromRequest(request);
        return costService.getMonthlyStatistics(userId, year, month);
    }

    /**
     * 获取消费分类统计
     */
    @GetMapping("/category")
    public CommonResult<?> getCategoryStatistics(HttpServletRequest request,
                                                 @RequestParam(required = false) String startDate,
                                                 @RequestParam(required = false) String endDate) {
        Long userId = getUserIdFromRequest(request);
        return costService.getCategoryStatistics(userId, startDate, endDate);
    }

    /**
     * AI月度消费报告
     */
    @GetMapping("/report")
    public CommonResult<?> getMonthlyReport(HttpServletRequest request,
                                             @RequestParam int year,
                                             @RequestParam int month) {
        Long userId = getUserIdFromRequest(request);
        return costService.getMonthlyReport(userId, year, month);
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = jwtUtil.extractToken(authHeader);
        return jwtUtil.getUserId(token);
    }
}