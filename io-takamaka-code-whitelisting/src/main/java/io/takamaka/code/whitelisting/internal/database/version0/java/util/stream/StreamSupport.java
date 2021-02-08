package io.takamaka.code.whitelisting.internal.database.version0.java.util.stream;

import io.takamaka.code.whitelisting.MustBeFalse;

public interface StreamSupport {
	<T> java.util.stream.Stream<T> stream(java.util.Spliterator<T> spliterator, @MustBeFalse boolean parallel);
}