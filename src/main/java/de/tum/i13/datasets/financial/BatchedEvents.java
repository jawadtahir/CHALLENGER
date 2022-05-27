package de.tum.i13.datasets.financial;

import java.time.Instant;
import java.util.ArrayList;

import de.tum.i13.bandency.Batch;

public class BatchedEvents {

    private final ArrayList<Batch> batches;
    private SymbolsGenerator sg;
    
    public BatchedEvents(SymbolsGenerator sg) {
        this.sg = sg;
        this.batches = new ArrayList<>();
    }

    public void loadData(FinancialEventLoader fel, int batchSize) {

        long cnt = 0;
        while(fel.hasMoreElements()) {
            Batch.Builder bb = Batch.newBuilder();
            for(int i = 0; i < batchSize && fel.hasMoreElements(); ++i) {
                bb.addEvents(fel.nextElement());
            }
            bb.setSeqId(cnt);
            bb.addAllLookupSymbols(this.sg.nextElement());

            batches.add(bb.build());

            ++cnt;
        }

        var last = this.batches.get(batches.size()-1);
        var newLast = Batch.newBuilder(last)
            .setLast(true)
            .build();
        this.batches.set(batches.size()-1, newLast);
    }

    public int batchCount() {
        return this.batches.size();
    }
    
    public FinancialBatchIterator newIterator(Instant stopTime) {
        return new FinancialBatchIterator(batches, stopTime);
    }
}
