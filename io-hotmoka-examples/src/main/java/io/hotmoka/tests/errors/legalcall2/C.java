package io.hotmoka.tests.errors.legalcall2;

import java.util.ArrayList;
import java.util.Collection;

import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.Storage;

@Exported
public class C extends Storage {
	private String s = "";

	public void test() {
		Collection<String> list = new ArrayList<>();
		list.add("hello");
		list.add("how");
		list.add("are");
		list.add("you");
		list.add("?");

		list.stream()
			.map(String::length)
			.forEachOrdered(this::process);
	}

	private void process(int length) {
		s += length;
	}

	@Override
	public String toString() {
		return s;
	}
}