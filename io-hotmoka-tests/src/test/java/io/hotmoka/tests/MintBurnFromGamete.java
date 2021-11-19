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

package io.hotmoka.tests;

import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;

/**
 * A test that the gamete can mint and burn coins of any account, if the consensus allows it to.
 */
public class MintBurnFromGamete extends TakamakaTest {

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_100_000);
	}

	@Test
	void mintIntoAccount() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		if (consensus == null || !consensus.allowsMintBurnFromGamete)
			return;

		BigInteger _100 = BigInteger.valueOf(100);
		StorageReference manifest = node.getManifest();
		StorageReference gamete = (StorageReference) runInstanceMethodCallTransaction(manifest, _50_000, takamakaCode(), MethodSignature.GET_GAMETE, manifest);
		BigInteger initialBalanceOfGamete = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(gamete, _100_000, takamakaCode(), CodeSignature.BALANCE, gamete))).value;
		BigInteger initialBalanceOfAccount = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(gamete, _100_000, takamakaCode(), CodeSignature.BALANCE, account(0)))).value;

		addInstanceMethodCallTransaction(privateKeyOfGamete, gamete, _100_000, ZERO, takamakaCode(), CodeSignature.EOA_MINT, account(0), new BigIntegerValue(_100));

		BigInteger finalBalanceOfGamete = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(gamete, _100_000, takamakaCode(), CodeSignature.BALANCE, gamete))).value;
		BigInteger finalBalanceOfAccount = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(gamete, _100_000, takamakaCode(), CodeSignature.BALANCE, account(0)))).value;

		// the account received 100 coins
		assertEquals(_100, finalBalanceOfAccount.subtract(initialBalanceOfAccount));
		
		// the gamete did not pay anything for that
		assertEquals(initialBalanceOfGamete, finalBalanceOfGamete);
	}

	@Test
	void burnFromAccount() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		if (consensus == null || !consensus.allowsMintBurnFromGamete)
			return;

		BigInteger _100 = BigInteger.valueOf(100);
		StorageReference manifest = node.getManifest();
		StorageReference gamete = (StorageReference) runInstanceMethodCallTransaction(manifest, _50_000, takamakaCode(), MethodSignature.GET_GAMETE, manifest);
		BigInteger initialBalanceOfGamete = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(gamete, _100_000, takamakaCode(), CodeSignature.BALANCE, gamete))).value;
		BigInteger initialBalanceOfAccount = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(gamete, _100_000, takamakaCode(), CodeSignature.BALANCE, account(0)))).value;

		addInstanceMethodCallTransaction(privateKeyOfGamete, gamete, _100_000, ZERO, takamakaCode(), CodeSignature.EOA_BURN, account(0), new BigIntegerValue(_100));

		BigInteger finalBalanceOfGamete = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(gamete, _100_000, takamakaCode(), CodeSignature.BALANCE, gamete))).value;
		BigInteger finalBalanceOfAccount = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(gamete, _100_000, takamakaCode(), CodeSignature.BALANCE, account(0)))).value;

		// the account lost 100 coins
		assertEquals(_100, initialBalanceOfAccount.subtract(finalBalanceOfAccount));
		
		// the gamete did not pay anything for that
		assertEquals(initialBalanceOfGamete, finalBalanceOfGamete);
	}

	@Test
	void mintWithIllegalCaller() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		if (consensus == null || !consensus.allowsMintBurnFromGamete)
			return;

		BigInteger _100 = BigInteger.valueOf(100);

		// the account tries to mint coins for itself and fails...
		throwsTransactionExceptionWithCauseAndMessageContaining(IllegalArgumentException.class, "the caller is not allowed to mint coins for an account", () -> {
			addInstanceMethodCallTransaction(privateKey(0), account(0), _100_000, ZERO, takamakaCode(), CodeSignature.EOA_MINT, account(0), new BigIntegerValue(_100));
		});
	}
}