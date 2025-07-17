
package com.hewei.hzyjy.xunzhi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 讯智后管应用
 */
@SpringBootApplication
@MapperScan("com.hewei.hzyjy.xunzhi.dao.mapper")
@EnableScheduling
public class XunZhiAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(XunZhiAdminApplication.class, args);
    }
}
