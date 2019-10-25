package io.takamaka.whitelisting.database.java.util.stream;

import io.takamaka.whitelisting.MustBeFalse;

public interface StreamSupport {
	<T> java.util.stream.Stream<T> stream(java.util.Spliterator<T> spliterator, @MustBeFalse boolean parallel);
}