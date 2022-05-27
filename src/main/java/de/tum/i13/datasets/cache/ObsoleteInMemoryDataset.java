package de.tum.i13.datasets.cache;

import java.time.Instant;
import java.util.ArrayList;

public class ObsoleteInMemoryDataset<T> {
    private final ArrayList<T> inMemoryBatches;

    public ObsoleteInMemoryDataset(ArrayList<T> inMemoryBatches) {

        this.inMemoryBatches = inMemoryBatches;
    }

    synchronized public CloseableSource<T> getIterator(Instant stopTime) {
        ObsoleteInMemoryDatasetIterator<T> imdi = new ObsoleteInMemoryDatasetIterator<T>(this.inMemoryBatches.iterator(), stopTime);
        return imdi;
    }

}
