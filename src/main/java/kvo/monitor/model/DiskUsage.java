package kvo.monitor.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DiskUsage {
    @JsonProperty("brokerId")
    private int brokerId;

    @JsonProperty("segmentSize")
    private long segmentSize;

    @JsonProperty("segmentCount")
    private int segmentCount;

    // Геттеры и сеттеры
    public int getBrokerId() {
        return brokerId;
    }

    public void setBrokerId(int brokerId) {
        this.brokerId = brokerId;
    }

    public long getSegmentSize() {
        return segmentSize;
    }

    public void setSegmentSize(long segmentSize) {
        this.segmentSize = segmentSize;
    }

    public int getSegmentCount() {
        return segmentCount;
    }

    public void setSegmentCount(int segmentCount) {
        this.segmentCount = segmentCount;
    }
}
