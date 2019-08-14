package takamaka.whitelisted.java.util.stream;

import takamaka.lang.MustBeFalse;

public interface StreamSupport {
	<T> java.util.stream.Stream<T> stream(java.util.Spliterator<T> spliterator, @MustBeFalse boolean parallel);
}