package kvo.monitor.ping;

import kvo.monitor.serviceCheck.ServiceCheck;
import kvo.monitor.sizetable.SizeTableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Timer;

@SpringBootApplication
@EnableScheduling
public class PingMonitorApplication {
    private static final Logger logger = LoggerFactory.getLogger(PingMonitorApplication.class);
    @Value("${period.request.services}")
    private long periodRequest;
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(PingMonitorApplication.class, args);
        PingMonitorApplication app = context.getBean(PingMonitorApplication.class);
        long period = app.periodRequest;

        Timer timer = new Timer();
        timer.schedule(new ServiceCheck(), 0, period);
        logger.info("Starting monitor services. Check every " + period/1000 + " sec.");
//        System.out.println("Мониторинг сервиса запущен. Проверка каждые " + period/1000 + " секунд...");
    }
}