package com.datalink;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * DataLink 数据开发平台启动类
 */
@SpringBootApplication
@MapperScan("com.datalink.mapper")
@EnableScheduling
@EnableAsync
public class DataLinkApplication {

    /**
     * 应用启动入口
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(DataLinkApplication.class, args);
    }
}
