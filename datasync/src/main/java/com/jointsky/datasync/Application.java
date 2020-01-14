package com.jointsky.datasync;

import java.io.IOException;

import org.springframework.boot.SpringApplication;

/**
 * 启动器
 * @author yuwb
 */
public class Application {
    public static void main( String[] args) throws IOException {
        SpringApplication.run("classpath:spring-config.xml",args);
    }
}
