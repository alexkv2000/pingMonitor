package kvo.monitor.model;


import java.util.List;

public class ClusterStats {
    private Integer broker;
    private Integer brokerCount;
    private Integer brokerStatus;
    private Integer controller;
    private Integer controllerCount;
    private Integer controllerStatus;
    private Integer zookeeper;
    private Integer zookeeperCount;
    private Integer zookeeperStatus;
    private List<DiskUsage> diskUsage;
    // Конструкторы, геттеры и сеттеры
    public ClusterStats() {}

    public ClusterStats(Integer broker, Integer brokerCount, Integer brokerStatus,
                        Integer controller, Integer controllerCount, Integer controllerStatus,
                        Integer zookeeper, Integer zookeeperCount, Integer zookeeperStatus,
                        List<DiskUsage> diskUsage) {
        this.broker = broker;
        this.brokerCount = brokerCount;
        this.brokerStatus = brokerStatus;
        this.controller = controller;
        this.controllerCount = controllerCount;
        this.controllerStatus = controllerStatus;
        this.zookeeper = zookeeper;
        this.zookeeperCount = zookeeperCount;
        this.zookeeperStatus = zookeeperStatus;
        this.diskUsage = diskUsage;
    }
    public List<DiskUsage> getDiskUsage() {
        return diskUsage;
    }

    public void setDiskUsage(List<DiskUsage> diskUsage) {
        this.diskUsage = diskUsage;
    }
    public Integer getBroker() {
        return broker;
    }

    public void setBroker(Integer broker) {
        this.broker = broker;
    }

    public Integer getBrokerCount() {
        return brokerCount;
    }

    public void setBrokerCount(Integer brokerCount) {
        this.brokerCount = brokerCount;
    }

    public Integer getBrokerStatus() {
        return brokerStatus;
    }

    public void setBrokerStatus(Integer brokerStatus) {
        this.brokerStatus = brokerStatus;
    }

    public Integer getController() {
        return controller;
    }

    public void setController(Integer controller) {
        this.controller = controller;
    }

    public Integer getControllerCount() {
        return controllerCount;
    }

    public void setControllerCount(Integer controllerCount) {
        this.controllerCount = controllerCount;
    }

    public Integer getControllerStatus() {
        return controllerStatus;
    }

    public void setControllerStatus(Integer controllerStatus) {
        this.controllerStatus = controllerStatus;
    }

    public Integer getZookeeper() {
        return zookeeper;
    }

    public void setZookeeper(Integer zookeeper) {
        this.zookeeper = zookeeper;
    }

    public Integer getZookeeperCount() {
        return zookeeperCount;
    }

    public void setZookeeperCount(Integer zookeeperCount) {
        this.zookeeperCount = zookeeperCount;
    }

    public Integer getZookeeperStatus() {
        return zookeeperStatus;
    }

    public void setZookeeperStatus(Integer zookeeperStatus) {
        this.zookeeperStatus = zookeeperStatus;
    }
}
