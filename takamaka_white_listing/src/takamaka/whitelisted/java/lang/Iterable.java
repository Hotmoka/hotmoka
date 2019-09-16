package takamaka.whitelisted.java.lang;

public interface Iterable<T> {
	java.util.Iterator<T> iterator();
	java.util.Spliterator<T> spliterator();
	void forEach(java.util.function.Consumer<? super T> action);
}