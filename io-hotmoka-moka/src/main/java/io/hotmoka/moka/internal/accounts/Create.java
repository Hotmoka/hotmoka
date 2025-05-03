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

package io.hotmoka.moka.internal.accounts;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.concurrent.TimeoutException;

import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.moka.internal.converters.SignatureOptionConverter;
import io.hotmoka.moka.internal.shared.AbstractCreateAccount;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.remote.api.RemoteNode;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "create", header = "Create a new account object.", showDefaultValues = true)
public class Create extends AbstractCreateAccount {

	@Option(names = "--signature", description = "the signature algorithm of the new account (ed25519, sha256dsa, qtesla1, qtesla3); if missing, the deafult request signature of the node will be used", converter = SignatureOptionConverter.class)
	private SignatureAlgorithm signature;

	@Override
	protected void mkCreationFromPayer(RemoteNode remote) throws CommandException, TimeoutException, InterruptedException, NodeException {
		new CreationFromPayer(remote);
	}

	@Override
	protected void mkCreationFromFaucet(RemoteNode remote) throws CommandException, TimeoutException, InterruptedException, NodeException {
		new CreationFromFaucet(remote);
	}

	private class CreationFromPayer extends AbstractCreateAccount.CreationFromPayer {

		private CreationFromPayer(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
			super(remote);
		}

		@Override
		protected StorageReference executeRequest() throws NodeException, TimeoutException, InterruptedException, CommandException {
			try {
				return remote.addConstructorCallTransaction(request);
			}
			catch (CodeExecutionException | TransactionRejectedException | TransactionException e) {
				throw new CommandException("The creation transaction failed! are the key pair of the payer and its password correct?", e);
			}
		}

		@Override
		protected ConstructorCallTransactionRequest mkRequest(StorageReference payer, String passwordOfPayerAsString, BigInteger balance) throws NodeException, TimeoutException, InterruptedException {
			try {
				Signer<SignedTransactionRequest<?>> signer = signatureOfPayer.getSigner(payerAccount.keys(passwordOfPayerAsString, signatureOfPayer).getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature);
				return TransactionRequests.constructorCall
						(signer, payer, nonce, remote.getConfig().getChainId(), proposedGas, gasPrice, remote.getTakamakaCode(),
								ConstructorSignatures.of(eoaType, StorageTypes.BIG_INTEGER, StorageTypes.STRING),
								StorageValues.bigIntegerOf(balance), StorageValues.stringOf(publicKetOfNewAccountBase64));
			}
			catch (InvalidKeyException | SignatureException e) {
				// the key has been created with the same signature algorithm, it cannot be invalid
				throw new RuntimeException(e);
			}
		}

		@Override
		protected SignatureAlgorithm getSignatureAlgorithmOfNewAccount() throws NodeException, TimeoutException, InterruptedException {
			return signature != null ? signature : remote.getConfig().getSignatureForRequests();
		}
	}

	private class CreationFromFaucet extends AbstractCreateAccount.CreationFromFaucet {

		private CreationFromFaucet(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
			super(remote);
		}

		@Override
		protected SignatureAlgorithm getSignatureAlgorithmOfNewAccount() throws NodeException, TimeoutException, InterruptedException {
			return signature != null ? signature : remote.getConfig().getSignatureForRequests();
		}

		@Override
		protected InstanceMethodCallTransactionRequest mkRequest(BigInteger balance) throws NodeException, TimeoutException, InterruptedException {
			try {
				// we use an empty signature algorithm and an arbitrary key, since the faucet is unsigned
				KeyPair keyPair = signatureOfFaucet.getKeyPair();
				Signer<SignedTransactionRequest<?>> signer = signatureOfFaucet.getSigner(keyPair.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature);

				// we use a random nonce: although the nonce is not checked for calls to the faucet,
				// this avoids the risk of the request being rejected because it is repeated
				return TransactionRequests.instanceMethodCall
						(signer, gamete, new BigInteger(64, new SecureRandom()), remote.getConfig().getChainId(), proposedGas, gasPrice, remote.getTakamakaCode(),
						faucetMethod, gamete, StorageValues.bigIntegerOf(balance), StorageValues.stringOf(publicKetOfNewAccountBase64));
			}
			catch (InvalidKeyException | SignatureException e) {
				// the key has been created with the same (empty!) signature algorithm, thus it cannot be invalid
				throw new RuntimeException(e);
			}
		}

		@Override
		protected StorageReference executeRequest() throws NodeException, TimeoutException, InterruptedException, CommandException {
			try {
				return remote.addInstanceMethodCallTransaction(request)
						.orElseThrow(() -> new CommandException(faucetMethod + " should not return void"))
						.asReturnedReference(faucetMethod, CommandException::new);
			}
			catch (CodeExecutionException | TransactionRejectedException | TransactionException e) {
				throw new CommandException("The creation transaction failed! Is the unsigned faucet open?", e);
			}
		}
	}
}