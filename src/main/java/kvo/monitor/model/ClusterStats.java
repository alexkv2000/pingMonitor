package kvo.monitor.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ClusterStats {
    @JsonProperty("brokerCount")
    private int brokerCount;

    @JsonProperty("diskUsage")
    private List<DiskUsage> diskUsage;

    // Геттеры и сеттеры
    public int getBrokerCount() {
        return brokerCount;
    }

    public void setBrokerCount(int brokerCount) {
        this.brokerCount = brokerCount;
    }

    public List<DiskUsage> getDiskUsage() {
        return diskUsage;
    }

    public void setDiskUsage(List<DiskUsage> diskUsage) {
        this.diskUsage = diskUsage;
    }
}

