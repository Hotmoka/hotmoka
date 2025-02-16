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

import static java.math.BigInteger.ONE;

import java.math.BigInteger;

import io.takamaka.code.lang.View;
import io.takamaka.code.math.BigIntegerSupport;
import io.takamaka.code.util.StorageIntMap;
import io.takamaka.code.util.StorageTreeIntMap;

/**
 * This class defines methods that test the storage map with integer keys implementation.
 */
public class IntMapTests {

	public static @View int testIteration1() {
		var map = new StorageTreeIntMap<BigInteger>();
		for (int key = 0; key < 100; key++)
			map.put(key, BigInteger.valueOf(key));

		return sum(map);
	}

	private static int sum(StorageIntMap<BigInteger> map) {
		class WrappedInt {
			int i;
		}

		var wi = new WrappedInt();
		map.forEachValue(i -> wi.i += i.intValue());

		return wi.i;
	}

	public static @View int testUpdate2() {
		var map = new StorageTreeIntMap<BigInteger>();
		for (int key = 0; key < 100; key++)
			map.put(key, BigInteger.valueOf(key));

		// we add one to the value bound to each key
		for (int key = 0; key < 100; key++)
			map.update(key, bi -> BigIntegerSupport.add(bi, ONE));

		return sum(map);
	}

	public static @View long testNullValues() {
		var map = new StorageTreeIntMap<BigInteger>();
		for (int key = 0; key < 100; key++)
			map.put(key, null);

		class WrappedLong {
			int l;
		}

		var wl = new WrappedLong();

		map.forEachValue(value -> { if (value == null) wl.l++; } );

		return wl.l;
	}
}