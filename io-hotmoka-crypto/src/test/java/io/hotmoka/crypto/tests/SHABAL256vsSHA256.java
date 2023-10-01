/*
Copyright 2023 Fausto Spoto

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

package io.hotmoka.crypto.tests;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.function.Function;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.crypto.HashingAlgorithms;
import io.hotmoka.testing.AbstractLoggedTests;

public class SHABAL256vsSHA256 extends AbstractLoggedTests {

	@Test
    @DisplayName("10,000,000 iterated sha256")
    void iteratedSHA256() throws Exception {
		long start = System.currentTimeMillis();
    	var data = "HELLO HASHING".getBytes();
    	var hasher = HashingAlgorithms.sha256().getHasher(Function.identity());
        for (int i = 0; i < 10_000_000; i++)
        	data = hasher.hash(data);

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("sha256 took " + elapsed + "ms");

        assertArrayEquals(new byte[] { -43, -79, -34, 42, -36, -29, 104, 32, 82, 63, 57, -116, 112, -90, 57, -59, -66, 86, -94, -7, 14, 56, -7, 55, -45, -3, -62, -39, -75, -27, -28, 21 }, data);
	}

	@Test
    @DisplayName("10,000,000 iterated shabal256")
    void iteratedSHABAL256() throws Exception {
		long start = System.currentTimeMillis();
    	var data = "HELLO HASHING".getBytes();
    	var hasher = HashingAlgorithms.shabal256().getHasher(Function.identity());
        for (int i = 0; i < 10_000_000; i++)
        	data = hasher.hash(data);

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("shabal256 took " + elapsed + "ms");

        assertArrayEquals(new byte[] { -110, -11, 92, -50, 122, 2, 38, -72, 74, 124, 82, -29, -9, 122, 63, 3, -27, -127, 35, -30, 8, -11, -39, 87, -90, -97, -118, 14, 29, 45, 62, 91 }, data);
	}
}