package de.tum.i13.dal.dto;

import com.google.gson.annotations.Expose;

public class PercentileResult {
    @Expose
    private double percentile;
    @Expose
    private long q1Latency;
    @Expose
    private long q2Latency;

    public PercentileResult(double percentile) {
        this.percentile = percentile;
        this.q1Latency = -1;
        this.q2Latency = -1;
    }

    public void setQ1Latency(long q1ValueAtPercentile) {
        this.q1Latency = q1ValueAtPercentile;
    }

    public void setQ2Latency(long q2ValueAtPercentile) {
        this.q2Latency = q2ValueAtPercentile;
    }

    public long getQ1Latency() {
        return q1Latency;
    }

    public long getQ2Latency() {
        return q2Latency;
    }

    public double getPercentile() {
        return percentile;
    }
}
