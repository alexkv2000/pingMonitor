package kvo.monitor.ping;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class PingService {
    private Map<String, List<Double>> pingData = new HashMap<>();
    @Value("${app.servers}")
    private List<String> servers = new ArrayList<>(); // Список серверов для пинг
    @Value("${app.servers}")
    private List<String> servers_default_list = new ArrayList<>();

    public Map<String, List<Double>> getPingData() {
        return pingData;
    }

    static Calendar DateClean;

    public PingService() {
        DateEmpty(); //Устанавливаем дату старта DateClean
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
        Calendar currentCal = Calendar.getInstance();
        if (DateClean.get(Calendar.DAY_OF_YEAR) != currentCal.get(Calendar.DAY_OF_YEAR)) {
            DateClean = currentCal;
            clearServerList();
        }

        if (!servers.isEmpty()) {
            List<String> serversCopy = new ArrayList<>(servers);
            for (String server : serversCopy) {
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

    private void clearServerList() {
//        List<String> serversCopy = new ArrayList<>(servers);
        //TODO заменить на  servers = new ArrayList<>(); а может быть просто servers = new ArrayList<>(servers_default_list);
        servers = new ArrayList<>(servers_default_list);
//        for (String server : serversCopy) {
//            removeServer(server);
//        }
//        servers = new ArrayList<>(servers_default_list);
        System.out.println("Список серверов пуст." + new Date());
    }

    private void DateEmpty() {
        DateClean = Calendar.getInstance();
        DateClean.setTime(new Date()); // Берем текущую дату и время
        DateClean.set(Calendar.HOUR_OF_DAY, 0); // Часы = 0
        DateClean.set(Calendar.MINUTE, 0);      // Минуты = 0
        DateClean.set(Calendar.SECOND, 0);      // Секунды = 0
        DateClean.set(Calendar.MILLISECOND, 0); // Миллисекунды = 0
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
        List<String> serversCopy = new ArrayList<>(servers);
        //TODO можно заменить pingData = serversCopy.stream().collect(Collectors.toMap(server -> server, server -> new ArrayList<>()));
        //TODO или так Map<String, List<Double>> tempMap = serversCopy.stream().collect(Collectors.toMap(server -> server, server -> new ArrayList<>()));
        //TODO pingData.putAll(tempMap);
        Map<String, List<Double>> tempMap = serversCopy.stream().collect(Collectors.toMap(server -> server, server -> new ArrayList<>()));
        pingData.clear();
        pingData.putAll(tempMap);
//        for (String server : serversCopy) {
//            pingData.put(server, new ArrayList<>());
//        }
    }
}