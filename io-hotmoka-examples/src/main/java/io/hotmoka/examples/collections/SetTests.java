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

package io.hotmoka.examples.collections;

import java.math.BigInteger;
import java.util.Random;

import io.takamaka.code.lang.View;
import io.takamaka.code.math.BigIntegerSupport;
import io.takamaka.code.util.SnapshottableStorageTreeSet;
import io.takamaka.code.util.StorageTreeSet;

/**
 * This class defines methods that test the storage set implementation.
 */
public class SetTests {

	public static @View boolean testRandomInitialization() {
		var set = new StorageTreeSet<BigInteger>();
		var random = new Random(12345L);

		// set will contain 100 distinct numbers between 0 and 200
		while (set.size() < 100) {
			set.add(BigInteger.valueOf(random.nextInt(200)));
			BigInteger bi = BigInteger.valueOf(random.nextInt(200));
			set.add(bi);
			set.remove(bi);
		}

		var elements = new BigInteger[set.size()];
		int pos = 0;
		for (BigInteger element: set)
			elements[pos++] = element;

		for (pos = 0; pos < elements.length; pos++)
			for (int next = pos + 1; next < elements.length; next++)
				if (BigIntegerSupport.equals(elements[pos], elements[next]))
					return false;

		return true; // we expect this
	}

	public static @View boolean testSnapshottableRandomInitialization() {
		var set = new SnapshottableStorageTreeSet<BigInteger>();
		var random = new Random(12345L);

		// set will contain 100 distinct numbers between 0 and 200
		while (set.size() < 100) {
			set.add(BigInteger.valueOf(random.nextInt(200)));
			BigInteger bi = BigInteger.valueOf(random.nextInt(200));
			set.add(bi);
			set.remove(bi);
		}

		var snapshot = set.snapshot();

		var elements = new BigInteger[snapshot.size()];
		int pos = 0;
		for (BigInteger element: snapshot)
			elements[pos++] = element;

		for (pos = 0; pos < elements.length; pos++)
			for (int next = pos + 1; next < elements.length; next++)
				if (BigIntegerSupport.equals(elements[pos], elements[next]))
					return false;

		return true; // we expect this
	}
}