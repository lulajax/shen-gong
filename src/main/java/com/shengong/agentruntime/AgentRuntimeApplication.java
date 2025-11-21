package com.shengong.agentruntime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 神工  应用主类
 *
 * @author 神工团队
 * @since 1.0.0
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
public class AgentRuntimeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentRuntimeApplication.class, args);
    }
}
