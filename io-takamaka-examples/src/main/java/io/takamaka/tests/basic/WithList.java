package io.takamaka.tests.basic;

import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.ModifiableStorageList;
import io.takamaka.code.util.StorageLinkedList;

@Exported
public class WithList extends Storage {
	private final ModifiableStorageList<Object> list = new StorageLinkedList<>();

	public WithList() {
		list.add("hello");
		list.add("how");
		list.add("are");
		list.add("you");
	}

	@Override @View
	public String toString() {
		return list.toString();
	}

	public void illegal() {
		// we add a non-Storage object: this is illegal
		list.add(new Object());
	}
}