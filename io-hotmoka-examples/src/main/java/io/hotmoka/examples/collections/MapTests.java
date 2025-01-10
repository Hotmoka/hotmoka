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
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;
import java.util.stream.IntStream;

import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageMap;
import io.takamaka.code.util.StorageTreeMap;
import io.takamaka.code.util.StorageMapView.Entry;

/**
 * This class defines methods that test the storage map implementation.
 */
public class MapTests {

	public static @View int testIteration1() {
		StorageMap<BigInteger, BigInteger> map = new StorageTreeMap<>();
		for (BigInteger key = ZERO; key.intValue() < 100; key = key.add(ONE))
			map.put(key, key);

		return sum(map.stream().map(Entry::getValue).mapToInt(BigInteger::intValue));
	}

	private static int sum(IntStream stream) {
		class WrappedInt {
			int i;
		}

		WrappedInt wi = new WrappedInt();
		stream.forEachOrdered(i -> wi.i += i);

		return wi.i;
	}

	public static @View int testUpdate2() {
		StorageMap<BigInteger, BigInteger> map = new StorageTreeMap<>();
		for (BigInteger key = ZERO; key.intValue() < 100; key = key.add(ONE))
			map.put(key, key);

		// we add one to the value bound to each key
		map.keys().forEachOrdered(key -> map.update(key, ONE::add));

		return sum(map.stream().map(Entry::getValue).mapToInt(BigInteger::intValue));
	}

	public static @View long testNullValues() {
		StorageMap<BigInteger, BigInteger> map = new StorageTreeMap<>();
		for (BigInteger key = ZERO; key.intValue() < 100; key = key.add(ONE))
			map.put(key, null);

		class WrappedLong {
			int l;
		}

		WrappedLong wl = new WrappedLong();

		map.stream().map(Entry::getValue).filter(bi -> bi == null).forEachOrdered(__ -> wl.l++);

		return wl.l;
	}
}