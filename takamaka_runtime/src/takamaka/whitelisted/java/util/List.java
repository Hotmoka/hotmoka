package takamaka.whitelisted.java.util;

public interface List<E> {
	public E get(int index);
	public boolean add(E e);
	E remove(int index);
	java.util.stream.Stream<E> stream(); // the result is ordered on lists
}