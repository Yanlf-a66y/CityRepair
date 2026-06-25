package com.cityrepair.dto;

/**
 * 维修人员排行数据项
 */
public class WorkerRankItem {

    private Long workerId;
    private String workerName;
    private Long completedCount;

    public WorkerRankItem() {}

    public WorkerRankItem(Long workerId, String workerName, Long completedCount) {
        this.workerId = workerId;
        this.workerName = workerName;
        this.completedCount = completedCount;
    }

    public Long getWorkerId() {
        return workerId;
    }

    public void setWorkerId(Long workerId) {
        this.workerId = workerId;
    }

    public String getWorkerName() {
        return workerName;
    }

    public void setWorkerName(String workerName) {
        this.workerName = workerName;
    }

    public Long getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(Long completedCount) {
        this.completedCount = completedCount;
    }
}
