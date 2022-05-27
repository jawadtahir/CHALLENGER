package de.tum.i13.datasets.cache;

import java.io.Closeable;
import java.util.Enumeration;

public interface CloseableSource<T> extends Enumeration<T>, Closeable {
}
