/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.examples.errors.encapsulation;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageLinkedList;
import io.takamaka.code.util.StorageList;
import io.takamaka.code.util.StorageListView;

/**
 * A test that private fields are encapsulated, that is, cannot be
 * modified even though the storage reference of the object held in the field is known.
 */
public class Encapsulated extends Storage {

	/**
	 * Calls from a wallet attempting to modify this list
	 * will succeed, since it is exported.
	 */
	private final StorageList<String> list1;

	/**
	 * Calls from a wallet attempting to modify this list
	 * will fail, since it is not exported.
	 */
	private final StorageList<String> list2;

	@Exported
	private static class ExportedModifiableStorageList<T> extends Storage implements StorageList<T> {

		private final StorageList<T> backing = new StorageLinkedList<>();

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
		public T[] toArray(IntFunction<T[]> generator) {
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
		public StorageListView<T> view() {
			return backing.view();
		}

		@Override
		public StorageListView<T> snapshot() {
			return backing.snapshot();
		}

		@Override
		public void forEach(Consumer<? super T> action) {
			backing.forEach(action);
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