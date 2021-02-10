package io.hotmoka.examples.errors.legalcall3;

import java.util.ArrayList;
import java.util.List;

public class C {
	public static boolean test() {
		List<String> list = new ArrayList<>();
		list.add("hello");
		list.add("how");
		list.add("are");
		list.add("you");
		list.add("?");

		// fine, although the new Object does not redefine hashCode()
		return list.contains(new Object());
	}
}