package com.qingtu.agent.entity.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * 课程导入DTO
 * 对应Excel表格列（仅用于EasyExcel导入）
 * AI导入使用JSON格式，包含weekStart/weekEnd字段
 */
@Data
public class CourseImportDTO {

    @ExcelProperty("课程名称")
    @ColumnWidth(20)
    private String name;

    @ExcelProperty("上课地点")
    @ColumnWidth(15)
    private String location;

    @ExcelProperty("星期")
    @ColumnWidth(8)
    private Integer weekday;

    @ExcelProperty("开始时间")
    @ColumnWidth(10)
    private String startTime;

    @ExcelProperty("结束时间")
    @ColumnWidth(10)
    private String endTime;

    @ExcelProperty("教师")
    @ColumnWidth(10)
    private String teacher;

    private Integer weekStart;

    private Integer weekEnd;
}