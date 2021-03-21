package io.hotmoka.tests;

import static java.math.BigInteger.ONE;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest.Signer;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.crypto.SignatureAlgorithm;
import io.takamaka.code.constants.Constants;

public class Faucet extends TakamakaTest {

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_10_000_000);
	}

	@Test
	void fundNewAccount() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		if (consensus == null || !consensus.allowsUnsignedFaucet)
			return;

		StorageReference manifest = node.getManifest();
		StorageReference gamete = (StorageReference) runInstanceMethodCallTransaction(manifest, _10_000, takamakaCode(), MethodSignature.GET_GAMETE, manifest);

		// we generate the key pair of the new account created by the faucet
		SignatureAlgorithm<SignedTransactionRequest> signature = signature();
		KeyPair keys = signature.getKeyPair();
		String publicKey = Base64.getEncoder().encodeToString(keys.getPublic().getEncoded());

		// we use an arbitrary signature for calling the faucet, since it won't be checked
		Signer signer = Signer.with(signature, signature.getKeyPair());

		StorageReference account = (StorageReference) node.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(signer, gamete, getNonceOf(gamete), chainId, _10_000, ONE, takamakaCode(),
			new NonVoidMethodSignature(ClassType.GAMETE, "faucet", ClassType.EOA, BasicTypes.INT, ClassType.STRING),
			gamete, new IntValue(100_000), new StringValue(publicKey)));

		assertNotNull(account);
	}

	@Test
	void callToFaucetFailsIfCallerIsNotTheGamete() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		StorageReference manifest = node.getManifest();
		StorageReference gamete = (StorageReference) runInstanceMethodCallTransaction(manifest, _10_000, takamakaCode(), MethodSignature.GET_GAMETE, manifest);

		// we generate the key pair of the new account created by the faucet
		KeyPair keys = signature().getKeyPair();
		String publicKey = Base64.getEncoder().encodeToString(keys.getPublic().getEncoded());

		// we use an arbitrary signature for calling the faucet, since it won't be checked
		Signer signer = Signer.with(signature(), privateKey(0));
		StorageReference caller = account(0);

		throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
			node.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(signer, caller, getNonceOf(caller), chainId, _10_000, ONE, takamakaCode(),
				new NonVoidMethodSignature(ClassType.GAMETE, "faucet", ClassType.EOA, BasicTypes.INT, ClassType.STRING),
				gamete, new IntValue(100_000), new StringValue(publicKey))));
	}
}