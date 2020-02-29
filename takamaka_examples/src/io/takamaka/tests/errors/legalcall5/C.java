package io.takamaka.tests.errors.legalcall5;

import java.util.HashSet;
import java.util.stream.StreamSupport;

public class C {

	public static void foo() {
		StreamSupport.stream(new HashSet<String>().spliterator(), false);
	}
}