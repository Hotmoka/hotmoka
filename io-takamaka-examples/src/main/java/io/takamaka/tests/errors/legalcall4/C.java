package io.takamaka.tests.errors.legalcall4;

import java.util.HashSet;
import java.util.Set;

import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.Storage;

@Exported
public class C extends Storage {
	private String s = "";

	public void test() {
		Set<String> set = new HashSet<>();
		set.add("hello");
		set.add("how");
		set.add("are");
		set.add("you");
		set.add("?");
		
		set.stream()
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