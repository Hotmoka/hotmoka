/*
Copyright 2025 Fausto Spoto

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

package io.hotmoka.tests;

import static io.hotmoka.helpers.Coin.panarea;

import java.security.PrivateKey;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;

/**
 * A test for the index of the node.
 */
class Index extends HotmokaTest {

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000, _1_000_000_000);
	}

	private void add(TransactionReference toAdd, List<TransactionReference> where) {
		int size = getIndexSize();

		where.add(toAdd);
		// we ensure that its length is limited to size
		while (where.size() > size)
			where.removeFirst();
	}

	@Test @DisplayName("the index works for successive updates of an object")
	void indexGrows() throws Exception {
		StorageReference eoa1 = account(0);
		PrivateKey key1 = privateKey(0);
		StorageReference eoa2 = account(1);
		PrivateKey key2 = privateKey(1);
		long delay = getDelayBeforeIndexUpdate();
		var expected = new LinkedList<TransactionReference>();

		add(eoa1.getTransaction(), expected);
		Thread.sleep(delay);
		Arrays.equals(node.getIndex(eoa1).toArray(TransactionReference[]::new), expected.toArray(TransactionReference[]::new));

		// eoa1 payes something to eoa2
		var future1 = postInstanceMethodCallTransaction(key1, eoa1, _500_000, panarea(1), takamakaCode(), MethodSignatures.RECEIVE_INT, eoa2, StorageValues.intOf(42));
		add(future1.getReferenceOfRequest(), expected);
		future1.get();
		Thread.sleep(delay);
		Arrays.equals(node.getIndex(eoa1).toArray(TransactionReference[]::new), expected.toArray(TransactionReference[]::new));

		// eoa1 payes something more to eoa2
		var future2 = postInstanceMethodCallTransaction(key1, eoa1, _500_000, panarea(1), takamakaCode(), MethodSignatures.RECEIVE_INT, eoa2, StorageValues.intOf(13));
		add(future2.getReferenceOfRequest(), expected);
		future2.get();
		Thread.sleep(delay);
		Arrays.equals(node.getIndex(eoa1).toArray(TransactionReference[]::new), expected.toArray(TransactionReference[]::new));

		// eoa2 payes something to eoa1
		var future3 = postInstanceMethodCallTransaction(key2, eoa2, _500_000, panarea(1), takamakaCode(), MethodSignatures.RECEIVE_INT, eoa1, StorageValues.intOf(17));
		add(future3.getReferenceOfRequest(), expected);
		future3.get();
		Thread.sleep(delay);
		Arrays.equals(node.getIndex(eoa1).toArray(TransactionReference[]::new), expected.toArray(TransactionReference[]::new));
	}
}