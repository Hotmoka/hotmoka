package io.takamaka.tests.javacollections;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import io.takamaka.code.lang.View;

/**
 * This class defines methods that test the use of the Java HashMap class.
 */
public class HashMapTests {

	public static @View String testToString1() {
		Map<String, BigInteger> map = new HashMap<>();
		String[] keys = { "hello", "how", "are", "you", "?" };
		for (String key: keys)
			map.put(key, BigInteger.valueOf(key.length()));

		return map.keySet().toString();
	}

	public static @View String testToString2() {
		Map<Object, String> map = new HashMap<>();
		Object[] keys = { "hello", "how", "are", "you", "?" };
		for (Object key: keys)
			map.put(key, key.toString());

		return map.keySet().toString();
	}

	public static @View String testToString3() { // non-deterministic
		Map<Object, String> map = new HashMap<>();
		Object[] keys = { "hello", new Object(), "are", "you", "?" };
		for (Object key: keys)
			map.put(key, key.toString());

		return map.keySet().toString();
	}

	public static @View String testToString4() {
		Map<Object, String> map = new HashMap<>();
		Object[] keys = { "hello", new C(), "are", "you", "?" };
		for (Object key: keys)
			map.put(key, key.toString());

		return map.keySet().toString();
	}
}