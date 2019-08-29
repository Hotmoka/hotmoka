package takamaka.tests.errors.illegalcalltononwhitelistedmethod10;

import java.util.HashSet;
import java.util.Set;

public class C {

	public static void test() {
		Set<String> set = new HashSet<>();
		set.add("hello");
		set.add("how");
		set.add("are");
		set.add("you");
		set.add("?");
		
		set.stream()
			.map(String::length)
			.forEachOrdered(C::process);
	}

	private static void process(int length) {}
}