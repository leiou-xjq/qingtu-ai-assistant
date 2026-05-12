package com.qingtu.agent.util;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class AliyunOssUtil {

    @Value("${aliyun.oss.endpoint:}")
    private String endpoint;

    @Value("${aliyun.oss.access-key-id:}")
    private String accessKeyId;

    @Value("${aliyun.oss.access-key-secret:}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucket-name:}")
    private String bucketName;

    private OSS ossClient;

    private OSS getOssClient() {
        if (ossClient == null) {
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        }
        return ossClient;
    }

    public String uploadAvatar(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFilename = file.getOriginalFilename();
        String suffix = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String key = "avatar/" + userId + "/" + UUID.randomUUID() + suffix;

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());

            getOssClient().putObject(bucketName, key, file.getInputStream(), metadata);

            String url = "https://" + bucketName + "." + endpoint + "/" + key;
            log.info("头像上传成功: userId={}, url={}", userId, url);
            return url;
        } catch (IOException e) {
            log.error("头像上传失败: userId={}, error={}", userId, e.getMessage());
            return null;
        }
    }

    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFilename = file.getOriginalFilename();
        String suffix = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String key = "documents/" + java.time.LocalDate.now() + "/" + UUID.randomUUID() + suffix;

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());

            getOssClient().putObject(bucketName, key, file.getInputStream(), metadata);

            String url = "https://" + bucketName + "." + endpoint + "/" + key;
            log.info("文件上传成功: fileName={}, url={}", originalFilename, url);
            return url;
        } catch (IOException e) {
            log.error("文件上传失败: fileName={}, error={}", originalFilename, e.getMessage());
            return null;
        }
    }

    public boolean deleteFile(String key) {
        try {
            getOssClient().deleteObject(bucketName, key);
            log.info("文件删除成功: key={}", key);
            return true;
        } catch (Exception e) {
            log.error("文件删除失败: key={}, error={}", key, e.getMessage());
            return false;
        }
    }

    public String getUrl(String key) {
        return "https://" + bucketName + "." + endpoint + "/" + key;
    }

    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
            ossClient = null;
        }
    }
}