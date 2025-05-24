package com.example.logging;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HighPerfLoggingApp implements CommandLineRunner {
    private final MessageProcessor processor;

    public HighPerfLoggingApp(MessageProcessor processor) {
        this.processor = processor;
    }

    public static void main(String[] args) {
        // Enable async logging and garbage-free factory
        System.setProperty("log4j2.contextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");
        System.setProperty("log4j2.messageFactory", "org.apache.logging.log4j.message.ReusableMessageFactory");
        SpringApplication.run(HighPerfLoggingApp.class, args);
    }

    @Override
    public void run(String... args) {
        for (int i = 0; i < 10; i++) {
            processor.processMessage("msg-" + i, i % 2 == 0);
        }
    }
}