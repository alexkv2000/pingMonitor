package kvo.monitor.ping;

import kvo.monitor.serviceCheck.ServiceCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.Timer;

@SpringBootApplication
@EnableScheduling
public class PingMonitorApplication {
    @Value("${server.sync}")
    private String SERVER_SYNC;
    private static final Logger logger = LoggerFactory.getLogger(PingMonitorApplication.class);

    @Value("${period.request.services}")
    private long periodRequest;
    @Value("${statusVisible}")
    private boolean statistic;
    public static void main(String[] args) throws UnknownHostException {
        ApplicationContext context = SpringApplication.run(PingMonitorApplication.class, args);
        PingMonitorApplication app = context.getBean(PingMonitorApplication.class);
        long period = app.periodRequest;

        InetAddress localHost = InetAddress.getLocalHost();
        String[] nameServer = localHost.getHostName().split("/");
        if (Objects.equals(nameServer[0], app.SERVER_SYNC)) {
            Timer timer = new Timer();
            timer.schedule(new ServiceCheck(app.statistic), 0, period);
            logger.info("Starting monitor services. Start every " + period / 1000 + " sec.");
        } else {
            logger.info("Monitor services is disabled.");
        }
    }


}