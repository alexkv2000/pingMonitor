package kvo.monitor.serviceCheck;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kvo.monitor.model.ClusterStats;
import kvo.monitor.model.DiskUsage;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.springframework.web.client.RestTemplate;

@Service
public class ServiceCheck extends TimerTask {
    private static final String CONNECT = "CONNECT";
    private static final String STATUS = "STATUS";
    private static final String CLUSTER = "CLUSTER";
    private static Map<String, Integer> checkResult = new HashMap<>();
    private static Map<String, Integer> checkResultCluster = new HashMap<>();
    private boolean statistic;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public ServiceCheck(boolean statistic) {
        this.statistic = statistic;
    }

    @Override
    public void run() {
// Загружаем URL из application.properties
        HashMap<String, String> checkCodeImage = loadServiceUrls();
        Map<String, Integer> itog = checkServiceImage(checkCodeImage);

        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(itog.entrySet());
        sortedEntries.sort(Map.Entry.comparingByKey());

        for (Map.Entry<String, Integer> entry : sortedEntries) {
            String key = entry.getKey();
            Integer value = entry.getValue();

            if (value != 1) {
                System.out.printf("Ключ: %-36s, Значение: %-3s\n", key, value);
                continue;
            } else if (statistic) {
                System.out.printf("Ключ: %-36s, Значение: %-3s\n", key, value);
            }
        }
        if (statistic) {
            System.out.printf("++++++++++++++++++++++++++++++++++++++++++++ %-59s\n", new Date());
        }
        Map<String, Integer> itogCluster = checkCluster(checkCodeImage);
        List<Map.Entry<String, Integer>> sortedEntriesCluster = new ArrayList<>(itogCluster.entrySet());
        sortedEntriesCluster.sort(Map.Entry.comparingByKey());
        for (Map.Entry<String, Integer> entry : sortedEntriesCluster) {
            String key = entry.getKey();
            Integer value = entry.getValue();


            if ((key.equals("Broker.count") && value == 3) || (key.equals("Broker.status") || value == 200) || (key.endsWith("Cluster") && value == 1)) {
                if (statistic) {
                    System.out.printf("Ключ: %-36s, Значение: %-3s, Время: %-16s\n", key, value, new Date());
                }
            } else {
                System.out.printf("Ключ: %-36s, Значение: %-3s, Время: %-16s\n", key, value, new Date());
            }
        }
        if (statistic) {
            System.out.printf("++++++++++++++++++++++++++++++++++++++++++++ Cluster - %s\n", new Date());
        }
    }

    private static HashMap<String, String> loadServiceUrls() {
        HashMap<String, String> urls = new HashMap<>();
        Properties properties = new Properties();
        try (InputStream input = ServiceCheck.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                System.err.println("Файл application.properties не найден в classpath!");
                return urls;
            }
            properties.load(input);
            // Загружаем все ключи, начинающиеся с "service."
            for (String key : properties.stringPropertyNames()) {
                if (key.startsWith("service.")) {
                    urls.put(key, properties.getProperty(key));
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка при загрузке application.properties: " + e.getMessage());
        }
        return urls;
    }

    //Основной модуль
    private static Map<String, Integer> checkServiceImage(HashMap<String, String> SERVICE_URL) {
        checkResult.clear();

        for (Map.Entry<String, String> entry : SERVICE_URL.entrySet()) {
            String key = entry.getKey();
            String url = entry.getValue();

            try {
                URL urlObj = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000); // Таймаут 5 секунд
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    // String keyConnect = key.split("\.")[2].toUpperCase();
                    boolean keyConnect = key.toUpperCase().contains(CONNECT);
                    boolean keyStatus = key.toUpperCase().contains(STATUS);
                    if (keyStatus) {
                        //но выдает {"status":"offline","errorDetails":"Ошибка подключения: Unknown error 0x80040770","timestamp":639000036961272237}
                        // offline - checkResult.put(key, -1);
                        // online - checkResult.put(key, 1);
                        // Парсим JSON тело ответа
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                            StringBuilder response = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                response.append(line);
                            }
                            JsonNode jsonNode = objectMapper.readTree(response.toString());
                            String status = jsonNode.get("status").asText();
                            if ("online".equals(status)) {
                                checkResult.put(key, 1);
                            } else if ("offline".equals(status)) {
                                checkResult.put(key, -1);
                            } else {
                                checkResult.put(key, -2); // Неизвестный статус, считаем offline
                            }
                        } catch (Exception e) {
                            // Ошибка парсинга JSON
                            checkResult.put(key, -1);
                        }
                        continue;
                    }
// Специальная логика для "Connect": только проверка кода 200 возвращает ошибку "status": "offline",
                    if (keyConnect) {
//работает корректно: HTTP 200");
                        String contentType = connection.getContentType();
                        if (contentType != null && contentType.startsWith("image/")) {
//работает корректно: получена иконка (" + contentType + ")");
                            checkResult.put(key, 1);
                            continue;
                        }

                        checkResult.put(key, 1);
                        continue;
                    } else { // в остальных случаях проверка на получения "image/"
                        String contentType = connection.getContentType();
                        if (contentType != null && contentType.startsWith("image/")) {
                            //работает корректно: получена иконка (" + contentType + ")");
                            checkResult.put(key, 1);
                            continue;
                        }
                    }
                    if (key.toUpperCase().contains(CLUSTER)) {
                        //работает корректно: получена иконка (" + contentType + ")");
                        checkResult.put(key, 1);
                        continue;
                    } else {
                        //получен некорректный ответ Нет иконки (" + contentType + ")");
                        checkResult.put(key, 0);
                        continue;
                    }
                } else {
                    //Ошибка HTTP
                    checkResult.put(key, -1);
                }
                connection.disconnect();
            } catch (IOException e) {
                //Ошибка подключения
                checkResult.put(key, -2); // Или -1, в зависимости от логики
            }
        }
        return checkResult;
    }

    private Map<String, Integer> checkCluster(HashMap<String, String> SERVICE_URL) {
        for (Map.Entry<String, String> entry : SERVICE_URL.entrySet()) {
            String key = entry.getKey();
            String url = entry.getValue();
            boolean keyCluster = key.toUpperCase().contains(CLUSTER);
            if (keyCluster) {
                try {
                    URL urlObj = new URL(url);
                    HttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
                    CloseableHttpClient httpClient = HttpClients.custom()
                            .setConnectionManager(connectionManager)
                            .build();

                    HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
                    requestFactory.setConnectTimeout(5000); // 5 сек таймаут подключения
                    requestFactory.setReadTimeout(5000);    // 5 сек таймаут чтения
                    requestFactory.setConnectionRequestTimeout(5000); // Таймаут из пула

                    RestTemplate restTemplate = new RestTemplate(requestFactory);

                    String clusterUrl = String.valueOf(urlObj);
                    try {
                        ResponseEntity<ClusterStats> response = restTemplate.getForEntity(clusterUrl, ClusterStats.class);
                        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                            //                     System.out.println("Статус : " + response.getStatusCode().toString().trim().substring(0, 3));
                            checkResultCluster.put("Broker.status", response.getStatusCode().value());
                            ClusterStats stats = response.getBody();
                            //Кластер stats: brokerCount = " + stats.getBrokerCount());
                            checkResultCluster.put("Broker.count", stats.getBrokerCount());

                            if (stats.getBrokerCount() != 3) {
                                List<DiskUsage> diskUsages = stats.getDiskUsage();
                                if (diskUsages != null && !diskUsages.isEmpty()) {
                                    //ВНИМАНИЕ: brokerCount != 3
                                    for (DiskUsage usage : diskUsages) {
                                        System.out.println("  - Broker ID: " + usage.getBrokerId() +
                                                ", segmentSize: " + usage.getSegmentSize() +
                                                ", segmentCount: " + usage.getSegmentCount());
                                    }
                                    checkResultCluster.put(key, 0);
                                } else {
                                    //ВНИМАНИЕ: brokerCount != 3
                                    checkResultCluster.put(key, -1);
                                }
                            } else {
                                //Кластер в норме: brokerCount = 3
                                checkResultCluster.put(key, 1);
                            }
                        } else {
                            //Ошибка HTTP-запроса к кластеру
                            checkResultCluster.put(key, -2);
                        }
                    } catch (Exception e) {
                        //Ошибка проверки кластера
                        checkResultCluster.put(key, -2);
                    }
                } catch (MalformedURLException e) {
                    checkResultCluster.put(key, -2);
// throw new RuntimeException(e);
                }
            }
        }
        return checkResultCluster;
    }
}