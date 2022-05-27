package de.tum.i13;

import de.tum.i13.dal.DB;
import de.tum.i13.dal.ResultsVerifier;
import io.grpc.Server;

import java.sql.SQLException;

import org.tinylog.Logger;

public class ShutDown extends Thread {
    private final ResultsVerifier rv;
    private final Server server;
    private final DB db;

    public ShutDown(ResultsVerifier rv, Server server, DB db) {
        this.rv = rv;
        this.server = server;
        this.db = db;
    }

    @Override
    public void run() {
        this.server.shutdown();
        Logger.info("Server shutdown");
        rv.shutdown();
        Logger.info("ResultsVerifier shutdown");
        try {
            db.getConnection().close();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Logger.info("Disconnect DB");
    }
}
