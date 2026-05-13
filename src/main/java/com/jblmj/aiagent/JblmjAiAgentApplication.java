package com.jblmj.aiagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JblmjAiAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(JblmjAiAgentApplication.class, args);
    }

}
