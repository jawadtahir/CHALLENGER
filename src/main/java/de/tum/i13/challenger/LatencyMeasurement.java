package de.tum.i13.challenger;

import de.tum.i13.bandency.ResultQ1;
import de.tum.i13.bandency.ResultQ2;

public class LatencyMeasurement {
    private final Long benchmarkId;
    private final Long batchId;

    public Long getStartTime() {
        return startTime;
    }

    private final Long startTime;

    private long q1ResultTime;
    private ResultQ1 q1Payload;
    private boolean hasQ1Results;
    private boolean hasQ2Results;
    private long q2ResultTime;
    private ResultQ2 q2Payload;

    public LatencyMeasurement(Long benchmarkId, Long batchId, Long startTime) {
        this.benchmarkId = benchmarkId;
        this.batchId = batchId;
        this.startTime = startTime;

        this.hasQ1Results = false;
        this.hasQ2Results = false;
    }

    public void setQ1Results(long q1ResultTime, ResultQ1 payload) {

        this.q1ResultTime = q1ResultTime;
        this.q1Payload = payload;
        this.hasQ1Results = true;
    }

    public long getQ1ResultTime() {
        return q1ResultTime;
    }

    public ResultQ1 getQ1Payload() {
        return q1Payload;
    }

    public boolean hasQ1Results() {
        return this.hasQ1Results;
    }

    public boolean hasQ2Results() {
        return this.hasQ2Results;
    }

    public void setQ2Results(long q2Resulttime, ResultQ2 payload) {

        this.q2ResultTime = q2Resulttime;
        this.q2Payload = payload;

        this.hasQ2Results = true;
    }

    public long getQ2ResultTime() {
        return q2ResultTime;
    }

    public Long getBenchmarkId() {
        return benchmarkId;
    }

    public Long getBatchId() {
        return batchId;
    }
}

