package takamaka.tests.basic;

import takamaka.lang.Storage;
import takamaka.util.StorageList;

public class WithList extends Storage {
	private final StorageList<Object> list = new StorageList<>();

	public WithList() {
		list.add("hello");
		list.add("how");
		list.add("are");
		list.add("you");
	}

	@Override
	public String toString() {
		return list.toString();
	}

	public void illegal() {
		// we add a non-Storage object: this is illegal
		list.add(new Object());
	}
}