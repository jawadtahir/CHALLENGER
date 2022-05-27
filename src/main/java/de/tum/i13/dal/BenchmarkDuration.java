package de.tum.i13.dal;

import org.HdrHistogram.Histogram;

public class BenchmarkDuration {
    private final long benchmarkId;
    private final long startTime;
    private final long endTime;
    private final double averageLatency;
    private final Histogram q1Histogram;
    private final Histogram q2Histogram;
    private final boolean q1Active;
    private final boolean q2Active;

    public BenchmarkDuration(long benchmarkId,
                             long startTime,
                             long endTime,
                             double averageLatency,
                             Histogram q1Histogram,
                             Histogram q2Histogram,
                             boolean q1Active,
                             boolean q2Active) {

        this.benchmarkId = benchmarkId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.averageLatency = averageLatency;
        this.q1Histogram = q1Histogram;
        this.q2Histogram = q2Histogram;
        this.q1Active = q1Active;
        this.q2Active = q2Active;
    }

    public long getBenchmarkId() {
        return benchmarkId;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public double getAverageLatency() {
        return averageLatency;
    }

    public Histogram getQ1Histogram() {
        return q1Histogram;
    }

    public Histogram getQ2Histogram() {
        return q2Histogram;
    }

    public boolean isQ1Active() {
        return q1Active;
    }

    public boolean isQ2Active() {
        return q2Active;
    }
}
