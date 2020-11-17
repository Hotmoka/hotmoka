package io.takamaka.tests.errors.encapsulation;

import java.util.Iterator;
import java.util.function.IntFunction;
import java.util.stream.Stream;

import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.ModifiableStorageList;
import io.takamaka.code.util.StorageLinkedList;
import io.takamaka.code.util.StorageList;

/**
 * A test that private fields are encapsulated, that is, cannot be
 * modified even though the storage reference of the object held in the field is known.
 */
public class Encapsulated extends Storage {

	/**
	 * Calls from a wallet attempting to modify this list
	 * will succeed, since it is exported.
	 */
	private final ModifiableStorageList<String> list1;

	/**
	 * Calls from a wallet attempting to modify this list
	 * will fail, since it is not exported.
	 */
	private final ModifiableStorageList<String> list2;

	@Exported
	private static class ExportedModifiableStorageList<T> extends Storage implements ModifiableStorageList<T> {

		private final ModifiableStorageList<T> backing = new StorageLinkedList<>();

		@Override
		public boolean contains(Object e) {
			return backing.contains(e);
		}

		@Override
		public T first() {
			return backing.first();
		}

		@Override
		public T last() {
			return backing.last();
		}

		@Override
		public T get(int index) {
			return backing.get(index);
		}

		@Override
		public int size() {
			return backing.size();
		}

		@Override
		public Stream<T> stream() {
			return backing.stream();
		}

		@Override
		public <A> A[] toArray(IntFunction<A[]> generator) {
			return backing.toArray(generator);
		}

		@Override
		public Iterator<T> iterator() {
			return backing.iterator();
		}

		@Override
		public void addFirst(T element) {
			backing.addFirst(element);
		}

		@Override
		public void addLast(T element) {
			backing.addLast(element);
		}

		@Override
		public void add(T element) {
			backing.add(element);
		}

		@Override
		public void clear() {
			backing.clear();
		}

		@Override
		public T removeFirst() {
			return backing.removeFirst();
		}

		@Override
		public boolean remove(Object e) {
			return backing.remove(e);
		}

		@Override
		public StorageList<T> view() {
			return backing.view();
		}

		@Override
		public StorageList<T> snapshot() {
			return backing.snapshot();
		}
	}

	public Encapsulated() {
		list1 = new ExportedModifiableStorageList<>();
		list1.add("42");
		list2 = new StorageLinkedList<>();
		list2.add("69");
	}

	public @View int size1() {
		return list1.size();
	}

	public @View int size2() {
		return list2.size();
	}
}