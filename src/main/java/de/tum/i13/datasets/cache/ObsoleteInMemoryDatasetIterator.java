package de.tum.i13.datasets.cache;

import org.tinylog.Logger;

import java.io.IOException;
import java.time.Instant;
import java.util.Iterator;

public class ObsoleteInMemoryDatasetIterator<T> implements CloseableSource<T> {

    private final Iterator<T> iter;
    private final Instant stopTime;

    public ObsoleteInMemoryDatasetIterator(Iterator<T> iter, Instant stopTime) {

        this.iter = iter;
        this.stopTime = stopTime;
    }

    @Override
    public boolean hasMoreElements() {
        boolean timeOut = Instant.now().isBefore(this.stopTime);
        if(!timeOut) {
            Logger.info("Timeout of datasource reached");
        }
        return timeOut && this.iter.hasNext();
    }

    @Override
    public T nextElement() {

        return this.iter.next();
    }

    @Override
    public void close() throws IOException {
        //do nothing
    }
}
