package de.tum.i13.challenger;

import de.tum.i13.bandency.Batch;
import de.tum.i13.bandency.ResultQ1;
import de.tum.i13.bandency.ResultQ2;
import de.tum.i13.dal.BenchmarkDuration;
import de.tum.i13.dal.ToVerify;
import de.tum.i13.datasets.cache.CloseableSource;

import org.HdrHistogram.Histogram;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

public class BenchmarkState {
    private final ArrayBlockingQueue<ToVerify> dbInserter;
    private String token;
    private int batchSize;
    private boolean isStarted;
    private HashMap<Long, Long> pingCorrelation;
    private ArrayList<Long> measurements;

    private HashMap<Long, LatencyMeasurement> latencyCorrelation;
    private ArrayList<Long> q1measurements;

    private Histogram q1Histogram;
    private Histogram q2Histogram;

    private double averageLatency;
    private long startNanoTime;
    private CloseableSource<Batch> datasource;
    private boolean q1Active;
    private boolean q2Active;
    private long benchmarkId;
    private long endNanoTime;
    private BenchmarkType benchmarkType;
    private String benchmarkName;

    public BenchmarkState(ArrayBlockingQueue<ToVerify> dbInserter) {
        this.dbInserter = dbInserter;
        this.averageLatency = 0.0;
        this.batchSize = -1;
        this.isStarted = false;

        this.pingCorrelation = new HashMap<>();
        this.measurements = new ArrayList<>();

        this.latencyCorrelation = new HashMap<>();
        this.q1measurements = new ArrayList<>();

        averageLatency = 0.0;
        startNanoTime = -1;
        endNanoTime = -1;
        datasource = null;

        this.q1Active = false;
        this.q2Active = false;

        this.q1Histogram = new Histogram( 3);
        this.q2Histogram = new Histogram( 3);

        this.benchmarkId = -1;

        this.benchmarkType = BenchmarkType.Test;
    }

    public void setQ1(boolean contains) {
        this.q1Active = contains;
    }

    public void setQ2(boolean contains) {
        this.q2Active = contains;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setIsStarted(boolean istarted) {
        this.isStarted = istarted;
    }

    public boolean getIsStarted() {
        return this.isStarted;
    }

    public String getToken() {
        return token;
    }

    public void setBenchmarkId(long random_id) {
        this.benchmarkId = random_id;
    }

    public long getBenchmarkId() {
        return benchmarkId;
    }

    public long getEndNanoTime() {
        return endNanoTime;
    }

    public void setEndNanoTime(long endNanoTime) {
        this.endNanoTime = endNanoTime;
    }

    public BenchmarkType getBenchmarkType() {
        return benchmarkType;
    }

    public void setBenchmarkType(BenchmarkType benchmarkType) {
        this.benchmarkType = benchmarkType;
    }



    //Methods for latency measurement
    public void addLatencyTimeStamp(long random_id, long nanoTime) {
        pingCorrelation.put(random_id, nanoTime);
    }

    public void correlatePing(long correlation_id, long nanoTime) {
        if(pingCorrelation.containsKey(correlation_id)) {
            Long sentTime = pingCorrelation.get(correlation_id);
            pingCorrelation.remove(correlation_id);
            long duration = nanoTime - sentTime;
            this.measurements.add(duration);
        }
    }

    public double calcAverageTransportLatency() {
        if(this.measurements.size() > 0) {
            this.averageLatency = this.measurements.stream().mapToLong(a -> a).average().getAsDouble();
        }

        return this.averageLatency;
    }

    //Starting the benchmark - timestamp
    public void startBenchmark(long startNanoTime) {
        this.startNanoTime = startNanoTime;
    }

    public void setDatasource(CloseableSource<Batch> newDataSource) {
        this.datasource = newDataSource;
    }

    public CloseableSource<Batch> getDatasource() {
        return this.datasource;
    }

    public Batch getNextBatch(long benchmarkId) {
        if(this.datasource == null) { //when participants ignore the last flag
            return Batch.newBuilder().setLast(true).build();
        }
        if(this.datasource.hasMoreElements()) {
            Batch batch = this.datasource.nextElement();
            LatencyMeasurement lm = new LatencyMeasurement(benchmarkId, batch.getSeqId(), System.nanoTime());
            this.latencyCorrelation.put(batch.getSeqId(), lm);
            return batch;
        } else {
            try {
                this.datasource.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.datasource = null;
            return Batch.newBuilder().setLast(true).build();
        }
    }

    public void resultsQ1(ResultQ1 request, long nanoTime) {
        if (latencyCorrelation.containsKey(request.getBatchSeqId())) {
            LatencyMeasurement lm = latencyCorrelation.get(request.getBatchSeqId());
            lm.setQ1Results(nanoTime, request);
            q1Histogram.recordValue(nanoTime - lm.getStartTime());
            if (isfinished(lm)) {
                this.dbInserter.add(new ToVerify(lm));
                latencyCorrelation.remove(request.getBatchSeqId());
            }
        }
    }

    public void resultsQ2(ResultQ2 request, long nanoTime) {
        if (latencyCorrelation.containsKey(request.getBatchSeqId())) {
            LatencyMeasurement lm = latencyCorrelation.get(request.getBatchSeqId());
            lm.setQ2Results(nanoTime, request);
            q2Histogram.recordValue(nanoTime - lm.getStartTime());
            if (isfinished(lm)) {
                this.dbInserter.add(new ToVerify(lm));
                latencyCorrelation.remove(request.getBatchSeqId());
            }
        }
    }

    private boolean isfinished(LatencyMeasurement lm) {
        if((this.q1Active == lm.hasQ1Results()) && (this.q2Active == lm.hasQ2Results())) {
            return true;
        }
        return false;
    }

    public void endBenchmark(long benchmarkId, long endTime) {
        this.endNanoTime = endTime;

        BenchmarkDuration bd = new BenchmarkDuration(
                benchmarkId,
                this.startNanoTime,
                endTime,
                this.averageLatency,
                q1Histogram,
                q2Histogram,
                this.q1Active,
                this.q2Active);
        this.dbInserter.add(new ToVerify(bd));
    }

    @Override
    public String toString() {
        return "BenchmarkState{" +
                "dbInserter=" + dbInserter.size() +
                ", token='" + token + '\'' +
                ", batchSize=" + batchSize +
                ", pingCorrelation=" + pingCorrelation.size() +
                ", measurements=" + measurements.size() +
                ", latencyCorrelation=" + latencyCorrelation.size() +
                ", q1measurements=" + q1measurements.size() +
                ", averageLatency=" + averageLatency +
                ", startNanoTime=" + startNanoTime +
                ", q1Active=" + q1Active +
                ", q2Active=" + q2Active +
                ", benchmarkId=" + benchmarkId +
                ", endNanoTime=" + endNanoTime +
                ", benchmarkType=" + benchmarkType +
                ", benchmarkName=" + benchmarkName +
                '}';
    }

    public void setBenchmarkName(String benchmarkName) {
        this.benchmarkName = benchmarkName;
    }
}
