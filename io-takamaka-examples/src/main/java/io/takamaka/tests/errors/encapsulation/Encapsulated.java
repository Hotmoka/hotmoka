package io.takamaka.tests.errors.encapsulation;

import io.takamaka.code.lang.Storage;
import io.takamaka.code.util.StorageList;

/**
 * A test that private fields are encapsulated, that is, cannot be
 * modified even though the storage reference of the object held in the field is known.
 */
public class Encapsulated extends Storage {
	private final StorageList<String> list1;
	private final StorageList<String> list2;

	public Encapsulated() {
		list1 = new StorageList<>();
		list1.add("42");
		list2 = new StorageList<>();
		list2.add("69");
	}

	public int size1() {
		return list1.size();
	}

	public int size2() {
		return list2.size();
	}
}