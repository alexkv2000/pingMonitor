package kvo.monitor.pcMonitorService;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

@Service
public class PCMonitorService {
    private static final TimeSeries cpuSeries = new TimeSeries("CPU Load (%)");
    private static final TimeSeries memorySeries = new TimeSeries("Memory Usage (MB)");
    private static final TimeSeriesCollection dataset = new TimeSeriesCollection();
    private static final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private static final SystemInfo systemInfo = new SystemInfo();
    private static final OperatingSystem os = systemInfo.getOperatingSystem();
    private static List<String[]> topProcesses = new ArrayList<>(); // [имя, CPU%]

    static {
        dataset.addSeries(cpuSeries);
        dataset.addSeries(memorySeries);
    }

    // Периодический сбор данных каждые 5 секунд
    @Scheduled(fixedRate = 5000)
    public void collectData() {
        // Загрузка CPU
        double cpuLoad = osBean.getSystemLoadAverage();
        if (cpuLoad < 0) {
            cpuLoad = Math.random() * 100; // Заглушка
        }
        cpuSeries.add(new Millisecond(), cpuLoad);

        // Использование памяти
        long totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);
        long freeMemory = Runtime.getRuntime().freeMemory() / (1024 * 1024);
        long usedMemory = totalMemory - freeMemory;
        memorySeries.add(new Millisecond(), usedMemory);

//        System.out.println("CPU series item count: " + cpuSeries.getItemCount());
//        for (int i = 0; i < cpuSeries.getItemCount(); i++) {
//            System.out.println("CPU point " + i + " time: " + cpuSeries.getTimePeriod(i).getFirstMillisecond());
//        }

        // Ограничение точек
        if (cpuSeries.getItemCount() > 96) {//60 кол-во точек на графике (8минут)
            //cpuSeries.removeAgedItems(30000, true); //Удалить все точки более 1 минуты true - обновить график
            cpuSeries.delete(0,0);
            memorySeries.delete(0,0);
        }
        // Топ-5 процессов по CPU
        List<OSProcess> processes = os.getProcesses();
        topProcesses = processes.stream()
                .filter(p -> {
                    double load = p.getProcessCpuLoadCumulative();
                    return !p.getName().equals("Idle") && !Double.isNaN(load) && load > 0;
                })
                .sorted((p1, p2) -> Double.compare(p2.getProcessCpuLoadCumulative() * 100, p1.getProcessCpuLoadCumulative() * 100))
                .limit(5)
                .map(p -> {
                    double cpuLoadPercent = p.getProcessCpuLoadCumulative() * 100;
                    if (Double.isNaN(cpuLoadPercent)) {
                        cpuLoadPercent = 0.0;
                    }
                    return new String[]{p.getName(), String.format(Locale.US, "%.2f", cpuLoadPercent)};
                })
                .collect(Collectors.toList());
    }

    // Генерация основного графика (временной ряд)
    public String generateMainGraph() {
        try {
            JFreeChart chart = ChartFactory.createTimeSeriesChart(
                    "PC Monitor - CPU & Memory",
                    "Time",
                    "Value",
                    dataset,
                    true,
                    true,
                    false
            );
            chart.setBackgroundPaint(Color.white);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ChartUtils.writeChartAsPNG(baos, chart, 400, 300);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            return "";
        }
    }

    // Генерация графика топ-процессов
    public String generateTopProcessesGraph() {
        try {
            DefaultCategoryDataset barDataset = new DefaultCategoryDataset();
            for (String[] proc : topProcesses) {
                double cpuValue;
                try {
                    cpuValue = Double.parseDouble(proc[1]);
                } catch (NumberFormatException e) {
                    cpuValue = 0.0;
                }
                barDataset.addValue(cpuValue, "CPU Load (%)", proc[0]);
            }

            JFreeChart chart = ChartFactory.createBarChart(
                    "TOP 5 Processes by CPU Load",
                    "Process Name",
                    "CPU Load (%)",
                    barDataset,
                    PlotOrientation.VERTICAL,
                    true,
                    true,
                    false
            );
            chart.setBackgroundPaint(Color.white);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ChartUtils.writeChartAsPNG(baos, chart, 400, 300);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            return "";
        }
    }
}
