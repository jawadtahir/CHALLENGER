package de.tum.i13.datasets.financial;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;

import de.tum.i13.bandency.Batch;
import de.tum.i13.datasets.cache.CloseableSource;

public class FinancialBatchIterator implements CloseableSource<Batch> {

    private ArrayList<Batch> batches;
    private int pointer;
    private Instant stopTime;

    public FinancialBatchIterator(ArrayList<Batch> batches, Instant stopTime) {
        this.batches = batches;
        this.pointer = 0;
        this.stopTime = stopTime;
    }

    @Override
    public boolean hasMoreElements() {
        return this.batches.size() > this.pointer;
    }

    @Override
    public Batch nextElement() {
        Batch b = batches.get(this.pointer);
        ++this.pointer;
        return b;
    }

    @Override
    public void close() throws IOException {
        //in memory, nothing to do
    }
    
}
