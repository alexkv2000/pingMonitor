package kvo.monitor;

import org.springframework.scheduling.annotation.Scheduled;
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
    private final Map<String, List<Double>> pingData = new HashMap<>();
    private final List<String> servers = Arrays.asList("sdo.gaz.ru", "doc-app.gaz.ru", "doc-app2.gaz.ru", "doc-app3.gaz.ru", "doc-app4.gaz.ru", "doc-app5.gaz.ru", "doc-app6.gaz.ru", "doc-app7.gaz.ru", "doc-app8.gaz.ru", "doc-test", "doc-send1", "doc-send2", "ya.ru"); // Список серверов для пинга
    public Map<String, List<Double>> getPingData() {
        return pingData;
    }

    @Scheduled(fixedRate = 5000)
    public void pingServers() {
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
    }

    private double ping(String host) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec("ping -n 1 " + host);

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(),  "866"));
        String line;

        Pattern pattern = Pattern.compile("(?:time|время)[=<]([0-9.]+)(?:\\s*ms|мс)?");
      //  "(?:time|время)[=<]([0-9.]+)(?:\\s*ms|мс)?"
      //  "время[=<]([0-9.]+)(?:мс)?"
        while ((line = reader.readLine()) != null) {
//            System.out.println(line);  // Выводим каждую строку вывода
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                process.waitFor();
                return Double.parseDouble(matcher.group(1));
            }
        }
        process.waitFor();
        throw new IOException("Not found time ping");
    }
}