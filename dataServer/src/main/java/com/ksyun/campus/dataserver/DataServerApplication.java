package com.ksyun.campus.dataserver;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableScheduling
public class DataServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(DataServerApplication.class,args);
        log.info("DataServer启动");
    }
}
