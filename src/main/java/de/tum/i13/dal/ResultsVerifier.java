package de.tum.i13.dal;

import com.google.gson.Gson;
import de.tum.i13.challenger.LatencyMeasurement;
import de.tum.i13.dal.dto.BenchmarkResult;
import de.tum.i13.dal.dto.PercentileResult;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import org.tinylog.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ResultsVerifier implements Runnable{
    private final ArrayBlockingQueue<ToVerify> verificationQueue;
    private final Queries q;
    private AtomicReference<Boolean> shutdown;
    private AtomicReference<Boolean> shuttingDown;

    public ResultsVerifier(ArrayBlockingQueue<ToVerify> verificationQueue, Queries q) {
        this.verificationQueue = verificationQueue;
        this.q = q;
        this.shuttingDown = new AtomicReference<Boolean>(false);
        this.shutdown = new AtomicReference<Boolean>(true);
    }

    static final Counter verifyMeasurementCounter = Counter.build()
            .name("verifyMeasurementCounter")
            .help("calls to verifyMeasurementCounter methods")
            .register();

    static final Counter durationMeasurementCounter = Counter.build()
            .name("durationMeasurementCounter")
            .help("calls to durationMeasurementCounter methods")
            .register();

    static final Histogram resultVerificationQueue = Histogram.build()
            .name("verificationQueue")
            .help("verificationQueue of Resultsverifier")
            .linearBuckets(0.0, 1_000.0, 21)
            .create()
            .register();

    static final Counter resultVerificationErrors = Counter.build()
            .name("verificationErrors")
            .help("counter of errors which currently are unhandled")
            .register();

    @Override
    public void run() {
        this.shuttingDown.set(false);
        this.shutdown.set(false);

        while(!shuttingDown.get() || verificationQueue.size() > 0) {
            try {
                ToVerify poll = verificationQueue.poll(100, TimeUnit.MILLISECONDS);
                resultVerificationQueue.observe(verificationQueue.size());
                if(poll != null) {
                    if(poll.getType() == VerificationType.Measurement) {
                        LatencyMeasurement lm = poll.getLatencyMeasurement();
                        try {
                            q.insertLatency(lm);
                        } catch (SQLException | ClassNotFoundException throwables) {
                            //We have to handle that gracefully
                            throwables.printStackTrace();

                            resultVerificationErrors.inc();
                        }

                        verifyMeasurementCounter.inc();
                    } else if(poll.getType() == VerificationType.Duration) {
                        BenchmarkDuration benchmarkDuration = poll.getBenchmarkDuration();
                        benchmarkDuration.getStartTime();

                        double[] percentiles = new double[]{50.0, 75.0, 87.5, 90, 95, 97.5, 99, 99.9};

                        var pl = new ArrayList<PercentileResult>();

                        for(double percentile : percentiles) {
                            var p = new PercentileResult(percentile);
                            if(benchmarkDuration.isQ1Active()) {
                                long valueAtPercentile = benchmarkDuration.getQ1Histogram().getValueAtPercentile(percentile);
                                p.setQ1Latency(valueAtPercentile);
                            }
                            if(benchmarkDuration.isQ2Active()) {
                                long valueAtPercentile = benchmarkDuration.getQ2Histogram().getValueAtPercentile(percentile);
                                p.setQ2Latency(valueAtPercentile);
                            }
                            pl.add(p);
                        }

                        double q1_90Percentile = benchmarkDuration.isQ1Active() ? benchmarkDuration.getQ1Histogram().getValueAtPercentile(90)/1e6 : -1.0;
                        double q2_90Percentile = benchmarkDuration.isQ2Active() ? benchmarkDuration.getQ2Histogram().getValueAtPercentile(90)/1e6 : -1.0;

                        benchmarkDuration.getStartTime();
                        benchmarkDuration.getEndTime();
                        benchmarkDuration.getQ1Histogram().getTotalCount();
                        benchmarkDuration.getQ2Histogram().getTotalCount();

                        double seconds = (benchmarkDuration.getEndTime() - benchmarkDuration.getStartTime()) / 1e9;
                        double q1Throughput = benchmarkDuration.getQ1Histogram().getTotalCount() / seconds;
                        double q2Throughput = benchmarkDuration.getQ2Histogram().getTotalCount() / seconds;

                        BenchmarkResult br = new BenchmarkResult(benchmarkDuration.getBenchmarkId(),
                                pl,
                                benchmarkDuration.getQ1Histogram().getTotalCount(),
                                benchmarkDuration.getQ2Histogram().getTotalCount(),
                                seconds,
                                q1Throughput,
                                q2Throughput,
                                q1_90Percentile,
                                q2_90Percentile);

                        Gson g = new Gson();
                        String s = g.toJson(br);

                        try {
                            q.insertBenchmarkResult(br, s);
                        } catch (SQLException | ClassNotFoundException throwables) {
                            throwables.printStackTrace();
                            Logger.error(throwables, "Insert of Benchmarkresult failed");
                            resultVerificationErrors.inc();
                        }

                        durationMeasurementCounter.inc();
                    }
                    //Here we do some database operations, verifcation of results and so on
                    //System.out.println(poll);
                }
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        this.shutdown.set(true);
        System.out.println("shutting down");
    }

    public void shutdown() {
        if(this.shutdown.get()) //is already shutdown
            return;

        //set the shutdown flag to drain the queue
        this.shuttingDown.set(true);

        while(true) { //Wait till the queue is drained
            try {
                Thread.sleep(100);
                if(this.shutdown.get())
                    return;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
