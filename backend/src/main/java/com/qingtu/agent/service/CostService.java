package com.qingtu.agent.service;

import com.qingtu.agent.common.CommonResult;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

/**
 * 消费记录服务接口
 *
 * 定义消费记录相关的业务操作
 *
 * @author 青途智伴技术团队
 */
public interface CostService {

    /**
     * 分页查询消费记录
     *
     * @param userId 用户ID
     * @param category 消费分类
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param year 年份（优先使用年月筛选）
     * @param month 月份
     * @param page 页码
     * @param size 每页数量
     * @return 消费记录列表
     */
    CommonResult<?> listCostRecords(Long userId, String category, String startDate, String endDate, Integer year, Integer month, int page, int size);

    /**
     * 添加消费记录
     *
     * @param userId 用户ID
     * @param amount 金额
     * @param category 分类
     * @param remark 备注
     * @param merchantName 商户名称
     * @param tradeTime 交易时间
     * @return 添加结果
     */
    CommonResult<?> addCostRecord(Long userId, BigDecimal amount, String category, String remark, String merchantName, String tradeTime);

    /**
     * 删除消费记录
     *
     * @param userId 用户ID
     * @param recordId 记录ID
     * @return 删除结果
     */
    CommonResult<?> deleteCostRecord(Long userId, Long recordId);

    /**
     * 导入微信/支付宝账单
     *
     * @param userId 用户ID
     * @param file Excel/CSV文件
     * @param source 来源（wechat/alipay）
     * @return 导入结果
     */
    CommonResult<?> importBill(Long userId, MultipartFile file, String source);

    /**
     * OCR识别账单图片
     *
     * @param userId 用户ID
     * @param file 账单截图
     * @return 识别结果列表
     */
    CommonResult<?> ocrBill(Long userId, MultipartFile file);

    /**
     * 获取月度消费统计
     *
     * @param userId 用户ID
     * @param year 年份
     * @param month 月份
     * @return 统计数据
     */
    CommonResult<?> getMonthlyStatistics(Long userId, int year, int month);

    /**
     * 获取消费分类统计
     *
     * @param userId 用户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 分类统计
     */
    CommonResult<?> getCategoryStatistics(Long userId, String startDate, String endDate);

    /**
     * AI消费分析报告
     *
     * @param userId 用户ID
     * @param year 年份
     * @param month 月份
     * @return 分析报告
     */
    CommonResult<?> getMonthlyReport(Long userId, int year, int month);
}