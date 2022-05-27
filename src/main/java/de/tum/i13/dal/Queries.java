package de.tum.i13.dal;

import de.tum.i13.challenger.BenchmarkType;
import de.tum.i13.challenger.LatencyMeasurement;
import de.tum.i13.dal.dto.BenchmarkResult;

import java.sql.*;
import java.time.Instant;
import java.util.UUID;

public class Queries {
    private final DB conn;

    public Queries(DB connectionPool) {
        this.conn = connectionPool;
    }

    public boolean checkIfGroupExists(String token) throws SQLException, ClassNotFoundException, InterruptedException {
        try(PreparedStatement preparedStatement = this.conn
                .getConnection()
                .prepareStatement("SELECT count(*) AS rowcount FROM groups where groupapikey = ?")) {
            preparedStatement.setString(1, token);
            try(ResultSet r = preparedStatement.executeQuery()) {
                r.next();
                int count = r.getInt("rowcount");
                return count == 1;
            }
        }
    }

    public UUID getGroupIdFromToken(String token) throws SQLException, ClassNotFoundException, InterruptedException {
        try(PreparedStatement preparedStatement = this.conn
                .getConnection()
                .prepareStatement("SELECT id AS group_id FROM groups where groupapikey = ?")) {
            preparedStatement.setString(1, token);
            try(ResultSet r = preparedStatement.executeQuery()) {
                r.next();
                return r.getObject("group_id", UUID.class);
            }
        }
    }

    public void insertBenchmarkStarted(long benchmarkId, UUID groupId, String benchmarkName, int batchSize, BenchmarkType bt) throws SQLException, ClassNotFoundException, InterruptedException {
        try(PreparedStatement pStmt = this.conn
                .getConnection()
                .prepareStatement("INSERT INTO benchmarks(" +
                        "id, group_id, \"timestamp\", benchmark_name, benchmark_type, batchsize) " +
                        "VALUES (?, ?, ?, ?, ?, ?)")) {

            pStmt.setLong(1, benchmarkId);
            pStmt.setObject(2, groupId);
            pStmt.setTimestamp(3, Timestamp.from(Instant.now()));
            pStmt.setString(4, benchmarkName);
            pStmt.setString(5, bt.toString());
            pStmt.setLong(6, batchSize);

            pStmt.execute();
        }
    }

    public void insertLatencyMeasurementStats(long benchmarkId, double averageLatency) throws SQLException, ClassNotFoundException, InterruptedException {

        //delete in case there is already a measurement
        try(PreparedStatement pStmt = this.conn
                .getConnection()
                .prepareStatement("DELETE FROM latencymeasurement where benchmark_id = ?")) {
            pStmt.setLong(1, benchmarkId);
            pStmt.execute();
        }

        //insert new metrics
        try(PreparedStatement pStmt = this.conn
                .getConnection()
                .prepareStatement("INSERT INTO latencymeasurement(" +
                                "benchmark_id, \"timestamp\", avglatency) " +
                                "VALUES (?, ?, ?)")) {

            pStmt.setLong(1, benchmarkId);
            pStmt.setTimestamp(2, Timestamp.from(Instant.now()));
            pStmt.setDouble(3, averageLatency);
            pStmt.execute();
        }
    }

    public void insertLatency(LatencyMeasurement lm) throws SQLException, ClassNotFoundException, InterruptedException {

        try(PreparedStatement pStmt = this.conn
                .getConnection()
                .prepareStatement("INSERT INTO querymetrics(" +
                                "benchmark_id, batch_id, starttime, q1resulttime, q1latency, q2resulttime, q2latency) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            pStmt.setLong(1, lm.getBenchmarkId());
            pStmt.setLong(2, lm.getBatchId());

            long startTime = lm.getStartTime();
            pStmt.setLong(3, startTime);
            if(lm.hasQ1Results()) {
                long q1resultTime = lm.getQ1ResultTime();
                long q1Latency = q1resultTime - startTime;

                pStmt.setLong(4, q1resultTime);
                pStmt.setLong(5, q1Latency);
            } else {
                pStmt.setLong(4, java.sql.Types.NULL);
                pStmt.setLong(5, java.sql.Types.NULL);
            }

            if(lm.hasQ2Results()) {
                long q2resultTime = lm.getQ2ResultTime();
                long q2Latency = q2resultTime - startTime;

                pStmt.setLong(6, q2resultTime);
                pStmt.setLong(7, q2Latency);
            } else {
                pStmt.setNull(6, java.sql.Types.NULL);
                pStmt.setLong(7, java.sql.Types.NULL);
            }

            pStmt.execute();
        }
    }

    public void insertBenchmarkResult(BenchmarkResult br, String s) throws SQLException, ClassNotFoundException, InterruptedException {

        try(PreparedStatement pStmt = this.conn
                .getConnection()
                .prepareStatement("INSERT INTO public.benchmarkresults(" +
                "id, duration_sec, q1_count, q1_throughput, q1_90percentile, q2_count, q2_throughput, q2_90percentile, summary) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {

            pStmt.setLong(1, br.getBenchmarkId());
            pStmt.setDouble(2, br.getSeconds());

            pStmt.setLong(3, br.getQ1_count());
            pStmt.setDouble(4, br.getQ1Throughput());
            pStmt.setDouble(5, br.getQ1_90Percentile());

            pStmt.setLong(6, br.getQ2_count());
            pStmt.setDouble(7, br.getQ2Throughput());
            pStmt.setDouble(8, br.getQ2_90Percentile());

            pStmt.setString(9, s);

            pStmt.execute();
        }
    }
}
