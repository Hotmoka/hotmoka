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
import io.takamaka.code.util.StorageTreeArray;
import io.takamaka.code.util.StorageTreeByteArray;

/**
 * This class defines methods that test the storage array implementation.
 */
public class ArrayTests {

	public static @View int testRandomInitialization() {
		var array = new StorageTreeArray<BigInteger>(100);
		Random random = new Random(12345L);

		for (int i = 0; i < 50; i++)
			while (array.setIfAbsent(random.nextInt(100), BigInteger.valueOf(i)) != null);

		int sum = 0;
		for (var bi: array)
			if (bi != null)
				sum += bi.intValue();

		return sum;
	}

	public static @View long countNullsAfterRandomInitialization() {
		var array = new StorageTreeArray<BigInteger>(100);
		Random random = new Random(12345L);

		for (int i = 0; i < 50; i++)
			while (array.setIfAbsent(random.nextInt(100), BigInteger.valueOf(i)) != null);

		class WrappedLong {
			long l;
		}

		WrappedLong wl = new WrappedLong();

		// 50 elements of the array should still be null
		array.stream().filter(bi -> bi == null).forEachOrdered(__ -> wl.l++);

		return wl.l;
	}

	public static @View int testUpdateWithDefault1() {
		var array = new StorageTreeArray<BigInteger>(100);
		Random random = new Random(12345L);

		for (int i = 0; i < 50; i++)
			while (array.setIfAbsent(random.nextInt(array.length), BigInteger.valueOf(i)) != null);

		for (int i = 0; i < array.length; i++)
			array.update(i, BigInteger.ZERO, bi -> BigIntegerSupport.add(bi, BigInteger.ONE));

		int sum = 0;
		for (var bi: array)
			sum += bi.intValue();

		return sum;
	}

	public static @View int testUpdateWithDefault2() {
		var array = new StorageTreeArray<BigInteger>(100);
		Random random = new Random(12345L);

		for (int i = 0; i < 50; i++) {
			var bi = BigInteger.valueOf(i);
			array.update(random.nextInt(100), BigInteger.ZERO, bi2 -> BigIntegerSupport.add(bi, bi2));
		}

		int sum = 0;
		for (var bi: array)
			if (bi != null)
				sum += bi.intValue();

		return sum;
	}

	public static @View int testGetOrDefault() {
		var array = new StorageTreeArray<BigInteger>(100);
		var random = new Random(12345L);

		for (int i = 0; i < 50; i++)
			while (array.setIfAbsent(random.nextInt(array.length), BigInteger.valueOf(i)) != null);

		var sum = BigInteger.ZERO;
		for (int i = 0; i < array.length; i++)
			sum = BigIntegerSupport.add(sum, array.getOrDefault(i, BigInteger.ZERO));

		return sum.intValue();
	}

	public static @View int testByteArrayThenIncrease() {
		var array = new StorageTreeByteArray(100);
		var random = new Random(12345L);

		for (byte i = 1; i <= 50; i++) {
			int index;

			do {
				index = random.nextInt(array.length);
			}
			while (array.get(index) != 0);
			
			array.set(index, i);
		}

		for (int i = 0; i < array.length; i++)
			array.set(i, (byte) (array.get(i) + 1));

		int sum = 0;
		for (byte b: array)
			sum += b;

		return sum;
	}
}