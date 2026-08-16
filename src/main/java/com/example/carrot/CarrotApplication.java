package com.example.carrot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 家庭积分系统（Carrot）入口。
 */
@SpringBootApplication
public class CarrotApplication {

    public static void main(String[] args) throws IOException {
        // 确保数据目录存在（SQLite 不会自动创建父目录）
        Path dataDir = Paths.get("data");
        Files.createDirectories(dataDir);
        // 确保日志目录存在（logback 也会自动创建，这里显式创建更稳妥）
        Path logDir = Paths.get("logs");
        Files.createDirectories(logDir);
        SpringApplication.run(CarrotApplication.class, args);
    }
}
