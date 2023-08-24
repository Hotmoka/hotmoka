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

package io.hotmoka.helpers.internal;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.SignatureException;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.crypto.Signers;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.helpers.NonceHelpers;
import io.hotmoka.helpers.api.MintBurnHelper;
import io.hotmoka.helpers.api.NonceHelper;
import io.hotmoka.nodes.api.Node;

/**
 * Implementation of a helper for minting and burning coins of an account in the accounts ledger
 * of the node. Only the gamete can do that and only if the node allows mint and burn from the gamete.
 */
public class MintBurnHelperImpl implements MintBurnHelper {
	private final Node node;
	private final TransactionReference takamakaCode;
	private final NonceHelper nonceHelper;
	private final StorageReference accountsLedger;
	private final StorageReference gamete;
	private final String chainId;
	private final static BigInteger _1_000_000 = BigInteger.valueOf(1_000_000L);
	private final static BigInteger _100_000 = BigInteger.valueOf(100_000L);

	/**
	 * Creates a helper for minting and burning coins of an account.
	 * 
	 * @param node the node whose accounts are considered
	 * @throws CodeExecutionException if some transaction fails
	 * @throws TransactionException if some transaction fails
	 * @throws TransactionRejectedException if some transaction fails
	 */
	public MintBurnHelperImpl(Node node) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		this.node = node;
		StorageReference manifest = node.getManifest();
		this.takamakaCode = node.getTakamakaCode();
		this.nonceHelper = NonceHelpers.of(node);
		this.accountsLedger = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _1_000_000, takamakaCode, CodeSignature.GET_ACCOUNTS_LEDGER, manifest));
		this.chainId = ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _1_000_000, takamakaCode, CodeSignature.GET_CHAIN_ID, manifest))).value;
		this.gamete = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _100_000, takamakaCode, CodeSignature.GET_GAMETE, manifest));
	}

	@Override
	public StorageReference mint(KeyPair keysOfGamete, SignatureAlgorithm<SignedTransactionRequest> signatureAlgorithm, String publicKey, BigInteger amount) throws InvalidKeyException, SignatureException, TransactionRejectedException, TransactionException, CodeExecutionException {
		var signer = Signers.with(signatureAlgorithm, keysOfGamete);

		// we look up the account in the accounts ledger; if it is not there, it will be created
		// we use 0 as gas price, so that the gamete will not pay for that (the add method of the accounts ledger
		// is special when called from the gamete, the constraints on the gas price are not applied then)
		var account = (StorageReference) node.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(signer, gamete, nonceHelper.getNonceOf(gamete), chainId, _1_000_000, BigInteger.ZERO, takamakaCode, CodeSignature.ADD_INTO_ACCOUNTS_LEDGER, accountsLedger, new BigIntegerValue(BigInteger.ZERO), new StringValue(publicKey)));

		if (amount.signum() != 0)
			// we mint coins for the account
			node.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(signer, gamete, nonceHelper.getNonceOf(gamete), chainId, _1_000_000, BigInteger.ZERO, takamakaCode, CodeSignature.EOA_MINT, account, new BigIntegerValue(amount)));

		return account;
	}

	@Override
	public StorageReference burn(KeyPair keysOfGamete, SignatureAlgorithm<SignedTransactionRequest> signatureAlgorithm, String publicKey, BigInteger amount) throws InvalidKeyException, SignatureException, TransactionRejectedException, TransactionException, CodeExecutionException {
		var signer = Signers.with(signatureAlgorithm, keysOfGamete);
		
		// we look up the account in the accounts ledger
		var account = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _1_000_000, takamakaCode, CodeSignature.GET_FROM_ACCOUNTS_LEDGER, accountsLedger, new StringValue(publicKey)));

		if (amount.signum() != 0)
			// we burn coins from the account
			node.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(signer, gamete, nonceHelper.getNonceOf(gamete), chainId, _1_000_000, BigInteger.ZERO, takamakaCode, CodeSignature.EOA_BURN, account, new BigIntegerValue(amount)));

		return account;
	}
}