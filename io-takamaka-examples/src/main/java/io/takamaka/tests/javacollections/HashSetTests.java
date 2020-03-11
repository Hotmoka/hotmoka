package io.takamaka.tests.javacollections;

import java.util.HashSet;
import java.util.Set;

import io.takamaka.code.lang.View;

/**
 * This class defines methods that test the use of the Java HashSet class.
 */
public class HashSetTests {

	public static @View String testToString1() {
		Set<Object> set = new HashSet<>();
		Object[] keys = { "hello", "how", "are", "you", "?", "are", "how" };
		for (Object key: keys)
			set.add(key);

		Set<Object> copy = new HashSet<>(set);
		return copy.toString();
	}

	public static @View String testToString2() {
		Set<Object> set = new HashSet<>();
		Object[] keys = { "hello", "how", new Object(), "are", "you", "?", "are", "how" };
		for (Object key: keys)
			set.add(key);

		Set<Object> copy = new HashSet<>(set);
		return copy.toString();
	}

	public static @View String testToString3() {
		Set<Object> set = new HashSet<>();
		Object[] keys = { "hello", "how", new C(), "are", "you", "?", "are", "how" };
		for (Object key: keys)
			set.add(key);

		Set<Object> copy = new HashSet<>(set);
		return copy.toString();
	}
}