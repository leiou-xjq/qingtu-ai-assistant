package com.qingtu.agent.util;

import com.alibaba.excel.EasyExcel;
import com.qingtu.agent.common.Constants;
import com.qingtu.agent.common.ResultCode;
import com.qingtu.agent.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Excel处理工具类
 */
@Slf4j
public class ExcelUtil {

    public static <T> List<T> readExcel(String filePath, Class<T> clazz) {
        if (!isValidExcel(filePath)) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_SUPPORT, "不支持的文件格式");
        }
        return EasyExcel.read(filePath).head(clazz).sheet().doReadSync();
    }

    public static <T> List<T> readExcel(InputStream inputStream, Class<T> clazz) {
        return EasyExcel.read(inputStream).head(clazz).sheet().doReadSync();
    }

    public static <T> void writeExcel(String filePath, Class<T> clazz, List<T> data) {
        if (!isValidExcel(filePath)) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_SUPPORT, "不支持的文件格式");
        }
        EasyExcel.write(filePath, clazz).sheet("Sheet1").doWrite(data);
    }

    public static <T> void downloadTemplate(HttpServletResponse response, Class<T> clazz, String fileName) {
        try {
            setExcelResponseHeader(response, fileName);
            EasyExcel.write(response.getOutputStream(), clazz).sheet("模板").doWrite(List.of());
        } catch (IOException e) {
            log.error("下载模板异常", e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR, "模板下载失败");
        }
    }

    public static <T> void exportExcel(HttpServletResponse response, Class<T> clazz, List<T> data, String fileName) {
        try {
            setExcelResponseHeader(response, fileName);
            EasyExcel.write(response.getOutputStream(), clazz).sheet("数据").doWrite(data);
        } catch (IOException e) {
            log.error("导出Excel异常", e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR, "导出失败");
        }
    }

    private static void setExcelResponseHeader(HttpServletResponse response, String fileName) throws UnsupportedEncodingException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
    }

    public static boolean isValidExcel(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        String suffix = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
        for (String allowed : Constants.ALLOWED_EXCEL_SUFFIX) {
            if (allowed.equals(suffix)) {
                return true;
            }
        }
        return false;
    }
}