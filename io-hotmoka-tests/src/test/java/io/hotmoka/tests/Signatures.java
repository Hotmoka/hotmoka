/**
 * 
 */
package io.hotmoka.tests;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest.Signer;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.LongValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.crypto.SignatureAlgorithm;

/**
 * A test for signing transactions with distinct signatures.
 */
class Signatures extends TakamakaTest {
	private static final BigInteger _100_000 = BigInteger.valueOf(100_000);
	private static final BigInteger _20_000_000 = BigInteger.valueOf(20_000_000);
	private static final BigInteger _1_000_000_000 = BigInteger.valueOf(1_000_000_000);

	@BeforeEach
	void beforeEach() throws Exception {
		setNode(_1_000_000_000);
	}

	@Test @DisplayName("create accounts with distinct signing algorithms")
	void createAccountsWithDistinctSigningAlgorithms() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		IntValue amount = new IntValue(_20_000_000);

		SignatureAlgorithm<SignedTransactionRequest> sha256dsa = SignatureAlgorithm.sha256dsa(SignedTransactionRequest::toByteArrayWithoutSignature);
		KeyPair sha256dsaKeyPair = sha256dsa.getKeyPair();
		StringValue sha256dsaPublicKey = new StringValue(Base64.getEncoder().encodeToString(sha256dsaKeyPair.getPublic().getEncoded()));
		addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, takamakaCode(), new ConstructorSignature("io.takamaka.code.lang.ExternallyOwnedAccountSHA256DSA", BasicTypes.INT, ClassType.STRING), amount, sha256dsaPublicKey);

		SignatureAlgorithm<SignedTransactionRequest> qtesla1 = SignatureAlgorithm.qtesla1(SignedTransactionRequest::toByteArrayWithoutSignature);
		KeyPair qteslaKeyPair = qtesla1.getKeyPair();
		StringValue qteslaPublicKey = new StringValue(Base64.getEncoder().encodeToString(qteslaKeyPair.getPublic().getEncoded()));
		addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, takamakaCode(), new ConstructorSignature("io.takamaka.code.lang.ExternallyOwnedAccountQTESLA1", BasicTypes.INT, ClassType.STRING), amount, qteslaPublicKey);

		SignatureAlgorithm<SignedTransactionRequest> ed25519 = SignatureAlgorithm.ed25519(SignedTransactionRequest::toByteArrayWithoutSignature);
		KeyPair ed25519KeyPair = ed25519.getKeyPair();
		StringValue ed25519PublicKey = new StringValue(Base64.getEncoder().encodeToString(ed25519KeyPair.getPublic().getEncoded()));
		addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, takamakaCode(), new ConstructorSignature("io.takamaka.code.lang.ExternallyOwnedAccountED25519", BasicTypes.INT, ClassType.STRING), amount, ed25519PublicKey);
	}

	@Test @DisplayName("create accounts with distinct signing algorithms and use them for signing transactions")
	void createAccountsWithDistinctSigningAlgorithmsAndUseThem() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		IntValue amount = new IntValue(_20_000_000);
		NonVoidMethodSignature callee = new NonVoidMethodSignature("io.takamaka.code.lang.Coin", "panarea", ClassType.BIG_INTEGER, BasicTypes.LONG);

		SignatureAlgorithm<SignedTransactionRequest> sha256dsa = SignatureAlgorithm.sha256dsa(SignedTransactionRequest::toByteArrayWithoutSignature);
		KeyPair sha256dsaKeyPair = sha256dsa.getKeyPair();
		StringValue sha256dsaPublicKey = new StringValue(Base64.getEncoder().encodeToString(sha256dsaKeyPair.getPublic().getEncoded()));
		StorageReference sha256dsaAccount = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, takamakaCode(), new ConstructorSignature("io.takamaka.code.lang.ExternallyOwnedAccountSHA256DSA", BasicTypes.INT, ClassType.STRING), amount, sha256dsaPublicKey);
		BigIntegerValue sha256dsaResult = (BigIntegerValue) nodeWithAccountsView.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest(Signer.with(sha256dsa, sha256dsaKeyPair), sha256dsaAccount, ZERO, chainId, _100_000, ONE, takamakaCode(), callee, new LongValue(1973)));
		assertEquals(BigInteger.valueOf(1973), sha256dsaResult.value);

		SignatureAlgorithm<SignedTransactionRequest> qtesla1 = SignatureAlgorithm.qtesla1(SignedTransactionRequest::toByteArrayWithoutSignature);
		KeyPair qteslaKeyPair = qtesla1.getKeyPair();
		StringValue qteslaPublicKey = new StringValue(Base64.getEncoder().encodeToString(qteslaKeyPair.getPublic().getEncoded()));
		StorageReference qteslaAccount = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, takamakaCode(), new ConstructorSignature("io.takamaka.code.lang.ExternallyOwnedAccountQTESLA1", BasicTypes.INT, ClassType.STRING), amount, qteslaPublicKey);
		BigIntegerValue qteslaResult = (BigIntegerValue) nodeWithAccountsView.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest(Signer.with(qtesla1, qteslaKeyPair), qteslaAccount, ZERO, chainId, _100_000, ONE, takamakaCode(), callee, new LongValue(1973)));
		assertEquals(BigInteger.valueOf(1973), qteslaResult.value);

		SignatureAlgorithm<SignedTransactionRequest> ed25519 = SignatureAlgorithm.ed25519(SignedTransactionRequest::toByteArrayWithoutSignature);
		KeyPair ed25519KeyPair = ed25519.getKeyPair();
		StringValue ed25519PublicKey = new StringValue(Base64.getEncoder().encodeToString(ed25519KeyPair.getPublic().getEncoded()));
		StorageReference ed25519Account = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, takamakaCode(), new ConstructorSignature("io.takamaka.code.lang.ExternallyOwnedAccountED25519", BasicTypes.INT, ClassType.STRING), amount, ed25519PublicKey);
		BigIntegerValue ed25519Result = (BigIntegerValue) nodeWithAccountsView.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest(Signer.with(ed25519, ed25519KeyPair), ed25519Account, ZERO, chainId, _100_000, ONE, takamakaCode(), callee, new LongValue(1973)));
		assertEquals(BigInteger.valueOf(1973), ed25519Result.value);
	}
}