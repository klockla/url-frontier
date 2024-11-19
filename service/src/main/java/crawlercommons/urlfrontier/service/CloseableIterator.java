package crawlercommons.urlfrontier.service;

import java.util.Iterator;

public interface CloseableIterator<T> extends AutoCloseable, Iterator<T> {}
