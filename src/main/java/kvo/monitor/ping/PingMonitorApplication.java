package kvo.monitor.ping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PingMonitorApplication {
    public static void main(String[] args) {
        SpringApplication.run(PingMonitorApplication.class, args);
    }
}