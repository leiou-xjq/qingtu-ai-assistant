package com.qingtu.agent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching
@EnableAsync
@MapperScan({"com.qingtu.agent.mapper", "com.qingtu.agent.mcp.server.audit"})
public class QingTuApplication {

    public static void main(String[] args) {
        SpringApplication.run(QingTuApplication.class, args);
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                            ║");
        System.out.println("║   青途智伴·大学生AI生活助手  已成功启动！                      ║");
        System.out.println("║                                                            ║");
        System.out.println("║   访问地址：http://localhost:8080/api                       ║");
        System.out.println("║                                                            ║");
        System.out.println("║   Swagger文档：http://localhost:8080/api/swagger-ui.html    ║");
        System.out.println("║                                                            ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
    }
}