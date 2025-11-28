package kvo.monitor.sizetable;

import kvo.monitor.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
@Service
public class SizeTableService {
    private static final Logger logger = LoggerFactory.getLogger(SizeTableService.class);
    protected static String mySql_Url = null;
    protected static String mySql_User = null;
    protected static String mySql_Password = null;
    protected static String mySql_TableSizes = null;

    static ConfigLoader configLoader;

    static class Record {
        String tableName;
        String fileGroup;
        double usedSize;
        LocalDate date;

        Record(String tableName, String fileGroup, double usedSize, LocalDate date) {
            this.tableName = tableName;
            this.fileGroup = fileGroup;
            this.usedSize = usedSize;
            this.date = date;
        }
    }

    public static Connection getConnectionMySql() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(mySql_Url, mySql_User, mySql_Password);
    }
    public static List<Map<String, Object>> getSizeTableMSSQL() throws SQLException, IOException {
        String currentDir = System.getProperty("user.dir");
        String configPath = currentDir + "\\config\\settings.txt"; //Paths.get(currentDir, "src\\config", "setting.txt").toString();
//        logger.info(configPath);
        if (currentDir.isEmpty()) {
            configPath = "C:\\Users\\KvochkinAY\\IdeaProjects\\Spring\\Project\\pingMonitor\\src\\config\\settings.txt";
        }
        configLoader = new ConfigLoader(configPath);
        mySql_Url = configLoader.getProperty("MYSQL.DB_PATH");
        mySql_User = configLoader.getProperty("MYSQL.USER");
        mySql_Password = configLoader.getProperty("MYSQL.PASSWORD");
        mySql_TableSizes = configLoader.getProperty("MYSQL.T_TABLE_SIZES");

        String getTableSQL = "select TableName, FileGroupName, UsedSizeMB, DataSize from " + mySql_TableSizes + " NOLOCK order by UsedSizeMB DESC";

        List<Record> records = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        try (Connection connection = getConnectionMySql();
             PreparedStatement preparedStatement = connection.prepareStatement(getTableSQL);
             ResultSet resultSetSizeTable = preparedStatement.executeQuery()) {

            while (resultSetSizeTable.next()) {
                String tableName = resultSetSizeTable.getString("TableName");
                String fileGroup = resultSetSizeTable.getString("FileGroupName");
                double usedSize = resultSetSizeTable.getDouble("UsedSizeMB");
                String dateStr = resultSetSizeTable.getString("DataSize");
                LocalDate date = LocalDate.parse(dateStr, formatter);
                records.add(new Record(tableName, fileGroup, usedSize, date));
            }

            // Группировка по tableName
            Map<String, List<Record>> grouped = records.stream()
                    .collect(Collectors.groupingBy(r -> r.tableName));

            // Список для результатов: [tableName, fileGroup, column3, lastDate, lastUsedSize]
            List<Object[]> results = new ArrayList<>();

            for (Map.Entry<String, List<Record>> entry : grouped.entrySet()) {
                String tableName = entry.getKey();
                List<Record> tableRecords = entry.getValue();

                // Сортировка по дате для нахождения последней
                tableRecords.sort(Comparator.comparing(r -> r.date));

                // Последняя дата и соответствующее usedSize
                LocalDate lastDate = tableRecords.get(tableRecords.size() - 1).date;
                double lastUsedSize = tableRecords.get(tableRecords.size() - 1).usedSize;


                double average = tableRecords.get(tableRecords.size() - 2).usedSize; // сравнение с предыдущей записью
//*
                if (lastUsedSize==0) {continue;}
//                // Сбор usedSize за даты, кроме последней
//                List<Double> previousUsedSizes = tableRecords.stream()
//                        .filter(r -> !r.date.equals(lastDate))
//                        .map(r -> r.usedSize)
//                        .collect(Collectors.toList());
//
//                // Вычисление среднего из предыдущих (если нет предыдущих, среднее = 0)
//                double average = previousUsedSizes.isEmpty() ? 0.0 : previousUsedSizes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
//*
                // Вычисление столбца3: lastUsedSize - average
                double difference = lastUsedSize - average;
                if (difference<20) {continue;}
                // FileGroup
                String fileGroup = tableRecords.get(0).fileGroup;

                // Max usedSize для сортировки
//                double maxUsedSize = tableRecords.stream().mapToDouble(r -> r.usedSize).max().orElse(0.0);

                results.add(new Object[]{tableName, fileGroup, difference, lastDate, lastUsedSize});
            }

            // Сортировка по убыванию difference
            results.sort((a, b) -> Double.compare((Double) b[2], (Double) a[2]));

            // Вывод заголовка
//            logger.info("TableName\tFileGroupName\tlastUsedSize\tUsedSizeMB\tlastDate");

            // Вывод результатов
            List<Map<String, Object>> results1 = new ArrayList<>();
            for (Object[] result : results) {  // удаляем значение usedSizeMB < 0
                if ((Double) result[2] > 0) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("tableName", result[0]);
                    item.put("fileGroup", result[1]);
                    item.put("usedSizeMB", result[2]);
                    item.put("lastDate", ((LocalDate) result[3]).format(formatter));
                    item.put("lastUsedSize", result[4]);
                    results1.add(item);
//                    logger.info(item.get("tableName").toString() + "\t" + item.get("fileGroup").toString() + "\t"  + item.get("lastUsedSize").toString() + "\t"  + item.get("usedSizeMB").toString() + "\t"  + item.get("lastDate").toString());
                }
            }
            return results1;
        } catch (SQLException e) {
            logger.error("Error query table : " + mySql_TableSizes, e);
            throw e;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
