package takamaka.whitelisted.java.lang;

public interface Iterable<T> {
	void forEach(java.util.function.Consumer<? super T> action);
}