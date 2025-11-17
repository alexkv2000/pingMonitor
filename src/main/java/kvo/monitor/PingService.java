package kvo.monitor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PingService {
    private Map<String, List<Double>> pingData = new HashMap<>();
    //    private List<String> servers = new ArrayList<>(Arrays.asList("sdo.gaz.ru", "doc-app.gaz.ru", "doc-app2.gaz.ru", "doc-app3.gaz.ru", "doc-app4.gaz.ru", "doc-app5.gaz.ru", "doc-app6.gaz.ru", "doc-app7.gaz.ru", "doc-app8.gaz.ru", "doc-test", "doc-send1", "doc-send2", "ya.ru")); // Список серверов для пинга
//    private List<String> servers = new ArrayList<>(Arrays.asList("sdo.gaz.ru")); // Список серверов для пинг
    @Value("${app.servers}")
    private List<String> servers = new ArrayList<>(); // Список серверов для пинг

    public Map<String, List<Double>> getPingData() {
        return pingData;
    }

    public void addServer(String serverName) {
        if (serverName == null || serverName.trim().isEmpty()) {
            throw new IllegalArgumentException("Name PC is NULL");
        }
        if (!servers.contains(serverName)) {
            servers.add(serverName);
        }
    }

    @Scheduled(fixedRate = 5000)
    public void pingServers() {
        if (!servers.isEmpty()) {
            for (String server : servers) {
                try {
                    double pingTime = ping(server);
                    pingData.computeIfAbsent(server, k -> new ArrayList<>()).add(pingTime);
                    // Ограничиваем историю до 100 записей
                    if (pingData.get(server).size() > 100) {
                        pingData.get(server).remove(0);
                    }
                } catch (Exception e) {
                    System.err.println("Error ping " + server + ": " + e.getMessage());
                    pingData.computeIfAbsent(server, k -> new ArrayList<>()).add(-1.0); // -1 для ошибок
                }
            }
        } else System.out.println("Список серверов пуст.");
    }

    private double ping(String host) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec("ping -n 1 " + host);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "866"));
        String line;
        Pattern pattern = Pattern.compile("(?:time|время)[=<]([0-9.]+)(?:\\s*ms|мс)?");
        Pattern timeoutPattern = Pattern.compile("(Превышен интервал ожидания для запроса|Request timed out|Time out)", Pattern.CASE_INSENSITIVE);
        Pattern destinationHostUnreachablePattern = Pattern.compile("(Заданный узел недоступен|Destination host unrechable)", Pattern.CASE_INSENSITIVE);

        boolean timeoutDetected = false;
        boolean hostUnreachable = false;

        while ((line = reader.readLine()) != null) {
            if (timeoutPattern.matcher(line).find()) {
                timeoutDetected = true;
            }
            if (destinationHostUnreachablePattern.matcher(line).find()) {
                hostUnreachable = true;
            }
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                process.waitFor();
                return Double.parseDouble(matcher.group(1));
            }
        }
        process.waitFor();
        int exitCode = process.exitValue();
        if (timeoutDetected || hostUnreachable || exitCode != 0) {
            return -1.0;
        }
        throw new IOException("Not found time ping");
    }

    public void removeServer(String computerName) {
        if (computerName == null || computerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Name PC is NULL");
        }
        String trimNane = computerName.trim();
        if (computerName.contains(trimNane)) {
            servers.remove(trimNane);
            pingData.remove(trimNane);
        }
    }

    public void clearAllPingData() {
        pingData.clear();
        for (String server : servers) {
            pingData.put(server, new ArrayList<>());
        }
    }
}