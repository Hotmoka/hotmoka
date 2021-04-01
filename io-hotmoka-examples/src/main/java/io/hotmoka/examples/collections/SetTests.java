package io.hotmoka.examples.collections;

import java.math.BigInteger;
import java.util.Random;

import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageTreeSet;

/**
 * This class defines methods that test the storage set implementation.
 */
public class SetTests {

	public static @View boolean testRandomInitialization() {
		StorageTreeSet<BigInteger> set = new StorageTreeSet<>();
		Random random = new Random(12345L);

		// set will contain 100 distinct numbers between 0 and 200
		while (set.size() < 100)
			set.add(BigInteger.valueOf(random.nextInt(200)));

		BigInteger[] elements = set.stream().toArray(BigInteger[]::new);
		for (int pos = 0; pos < elements.length; pos++)
			for (int next = pos + 1; next < elements.length; next++)
				if (elements[pos].equals(elements[next]))
					return false;

		return true; // we expect this
	}
}