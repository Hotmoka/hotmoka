package io.takamaka.tests.errors.encapsulation;

import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageList;

/**
 * A test that private fields are encapsulated, that is, cannot be
 * modified even though the storage reference of the object held in the field is known.
 */
public class Encapsulated extends Storage {

	/**
	 * Calls from a wallet attempting to modify this list
	 * will fail, since it is not exported.
	 */
	private final StorageList<String> list1;

	@Exported
	private static class ExportedStorageList<T> extends StorageList<T> {}

	/**
	 * Calls from a wallet attempting to modify this list
	 * will succeed, since it is exported.
	 */
	private final StorageList<String> list2;

	
	public Encapsulated() {
		list1 = new ExportedStorageList<>();
		list1.add("42");
		list2 = new StorageList<>();
		list2.add("69");
	}

	public @View int size1() {
		return list1.size();
	}

	public @View int size2() {
		return list2.size();
	}
}