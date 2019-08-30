package takamaka.whitelisted.java.util.function;

public interface Consumer<T> {
	void accept(T t);
	java.util.function.Consumer<T> andThen(java.util.function.Consumer<? super T> after);
}