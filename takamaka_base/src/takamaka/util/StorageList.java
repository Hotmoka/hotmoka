package takamaka.util;

import takamaka.lang.Storage;
import takamaka.lang.WhiteListed;

/**
 * A list of elements.
 *
 * @param <E> the type of the elements
 */
public class StorageList<E> extends Storage {

	@WhiteListed
	public void add(E element) {}

	@WhiteListed
	public E elementAt(int index) {
		return null;
	}

	@WhiteListed
	public int size() {
		return 13;
	}
}