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

package io.hotmoka.tools.internal.moka;

import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.NoSuchElementException;
import java.util.Scanner;

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.bouncycastle.util.io.pem.PemReader;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.SignatureAlgorithm;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.NonInitialTransactionResponse;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.responses.TransactionResponseFailed;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.Node;
import io.hotmoka.remote.RemoteNodeConfig;
import io.hotmoka.views.SignatureHelper;

public abstract class AbstractCommand implements Runnable {
	protected static final BigInteger _100_000 = BigInteger.valueOf(100_000L);
	protected static final String ANSI_RESET = "\u001B[0m";
	protected static final String ANSI_BLACK = "\u001B[30m";
	protected static final String ANSI_RED = "\u001B[31m";
	protected static final String ANSI_GREEN = "\u001B[32m";
	protected static final String ANSI_YELLOW = "\u001B[33m";
	protected static final String ANSI_BLUE = "\u001B[34m";
	protected static final String ANSI_PURPLE = "\u001B[35m";
	protected static final String ANSI_CYAN = "\u001B[36m";
	protected static final String ANSI_WHITE = "\u001B[37m";


	@Override
	public final void run() {
		try {
			execute();
		}
		catch (CommandException e) {
			throw e;
		}
		catch (Throwable t) {
			throw new CommandException(t);
		}
	}

	protected abstract void execute() throws Exception;

	protected RemoteNodeConfig remoteNodeConfig(String url) {
		return new RemoteNodeConfig.Builder().setURL(url).build();
	}

	protected void dumpKeys(StorageReference account, KeyPair keys, Node node) throws IOException, NoSuchAlgorithmException, ClassNotFoundException, TransactionRejectedException, TransactionException, CodeExecutionException {
		SignatureAlgorithm<SignedTransactionRequest> algorithm = new SignatureHelper(node).signatureAlgorithmFor(account);
		algorithm.dumpAsPem(account.toString(), keys);
	}

	protected KeyPair readKeys(StorageReference account, Node node) throws NoSuchAlgorithmException, ClassNotFoundException, TransactionRejectedException, TransactionException, CodeExecutionException, NoSuchProviderException, InvalidKeySpecException, IOException {
		SignatureAlgorithm<SignedTransactionRequest> algorithm = new SignatureHelper(node).signatureAlgorithmFor(account);

		if (algorithm.getName().equalsIgnoreCase("ed25519")) {
			return toKeyPairED25519(account + ".pub", account + ".pri");
		}
		else if (algorithm.getName().equalsIgnoreCase("qtesla1") || algorithm.getName().equalsIgnoreCase("qtesla3")) {
			return toKeyPairQTesla(account + ".pub", account + ".pri");
		}
		else {
			return toKeyPairDSA(account + ".pub", account + ".pri");
		}
	}

	private static byte[] getPemFile(String file) throws IOException {
		try (PemReader reader = new PemReader(new FileReader(file))) {
			return reader.readPemObject().getContent();
		}
	}

	/**
	 * It returns a qTESLA KeyPair from the encoded private and public key.
	 * @param publicKeyFilePath the public key file path
	 * @param privateKeyFilePath the private key file path
	 * @return the key pair
	 * @throws IOException if there are errors while reading the files
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 * @throws InvalidKeySpecException
	 */
	private static KeyPair toKeyPairQTesla(String publicKeyFilePath, String privateKeyFilePath) throws IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
		if (Security.getProvider(BouncyCastlePQCProvider.PROVIDER_NAME) == null)
			Security.addProvider(new BouncyCastlePQCProvider());

		byte[] encodedPublicKey = getPemFile(publicKeyFilePath);
		byte[] encodedPrivateKey = getPemFile(privateKeyFilePath);

		// key factory
		KeyFactory keyFactory = KeyFactory.getInstance("qTESLA", "BCPQC");
		PublicKey publicKeyObj = keyFactory.generatePublic(new X509EncodedKeySpec(encodedPublicKey));
		PrivateKey privateKeyObj = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(encodedPrivateKey));

		return new KeyPair(publicKeyObj, privateKeyObj);
	}

	/**
	 * It returns an Ed25519 KeyPair from the encoded private and public key.
	 * @param publicKeyFilePath the public key file path
	 * @param privateKeyFilePath the private key file path
	 * @return the key pair
	 * @throws IOException if there are errors while reading the files
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 * @throws InvalidKeySpecException
	 */
	private static KeyPair toKeyPairED25519(String publicKeyFilePath, String privateKeyFilePath) throws IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
			Security.addProvider(new BouncyCastleProvider());

		byte[] encodedPublicKey = getPemFile(publicKeyFilePath);
		byte[] encodedPrivateKey = getPemFile(privateKeyFilePath);

		// private key
		Ed25519PrivateKeyParameters privateKeyParams = new Ed25519PrivateKeyParameters(encodedPrivateKey, 0);
		byte[] pkcs8Encoded = PrivateKeyInfoFactory.createPrivateKeyInfo(privateKeyParams).getEncoded();

		// public key
		Ed25519PublicKeyParameters publicKeyParams = new Ed25519PublicKeyParameters(encodedPublicKey, 0);
		byte[] spkiEncoded = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(publicKeyParams).getEncoded();

		// key factory
		KeyFactory keyFactory = KeyFactory.getInstance("Ed25519", "BC");
		PublicKey publicKeyObj = keyFactory.generatePublic(new X509EncodedKeySpec(spkiEncoded));
		PrivateKey privateKeyObj = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(pkcs8Encoded));

		return new KeyPair(publicKeyObj, privateKeyObj);
	}

	/**
	 * It returns a DSA KeyPair from the encoded private and public key.
	 * @param publicKeyFilePath the public key file path
	 * @param privateKeyFilePath the private key file path
	 * @return the key pair
	 * @throws IOException if there are errors while reading the files
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 */
	private static KeyPair toKeyPairDSA(String publicKeyFilePath, String privateKeyFilePath) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		byte[] encodedPublicKey = getPemFile(publicKeyFilePath);
		byte[] encodedPrivateKey = getPemFile(privateKeyFilePath);

		KeyFactory keyFactory = KeyFactory.getInstance("DSA");
		PublicKey publicKeyObj = keyFactory.generatePublic(new X509EncodedKeySpec(encodedPublicKey));
		PrivateKey privateKeyObj = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(encodedPrivateKey));

		return new KeyPair(publicKeyObj, privateKeyObj);
	}

	protected String fileFor(StorageReference account) {
		return account.toString() + ".keys";
	}

	protected BigInteger gasForCreatingAccountWithSignature(String signature, Node node) {
		switch (signature) {
		case "ed25519":
			return _100_000;
		case "sha256dsa":
			return BigInteger.valueOf(200_000L);
		case "qtesla1":
			return BigInteger.valueOf(3_000_000L);
		case "qtesla3":
			return BigInteger.valueOf(6_000_000L);
		case "empty":
			return _100_000;
		default:
			throw new IllegalArgumentException("unknown signature algorithm " + signature);
		}
	}

	protected BigInteger gasForTransactionWhosePayerHasSignature(String signature, Node node) {
		switch (signature) {
		case "ed25519":
		case "sha256dsa":
			return _100_000;
		case "qtesla1":
			return BigInteger.valueOf(300_000L);
		case "qtesla3":
			return BigInteger.valueOf(400_000L);
		case "empty":
			return _100_000;
		default:
			throw new IllegalArgumentException("unknown signature algorithm " + signature);
		}
	}

	protected void printCosts(Node node, TransactionRequest<?>... requests) {
		BigInteger forPenalty = BigInteger.ZERO;
		BigInteger forCPU = BigInteger.ZERO;
		BigInteger forRAM = BigInteger.ZERO;
		BigInteger forStorage = BigInteger.ZERO;

		for (TransactionRequest<?> request: requests)
			if (request != null)
				try {
					TransactionResponse response = node.getResponse(request.getReference());
					if (response instanceof NonInitialTransactionResponse) {
						NonInitialTransactionResponse responseWithGas = (NonInitialTransactionResponse) response;
						forCPU = forCPU.add(responseWithGas.gasConsumedForCPU);
						forRAM = forRAM.add(responseWithGas.gasConsumedForRAM);
						forStorage = forStorage.add(responseWithGas.gasConsumedForStorage);
						if (responseWithGas instanceof TransactionResponseFailed)
							forPenalty = forPenalty.add(((TransactionResponseFailed) responseWithGas).gasConsumedForPenalty());
					}
				}
				catch (TransactionRejectedException | NoSuchElementException e) {}

		System.out.println(ANSI_CYAN + "Total gas consumed: " + forCPU.add(forRAM).add(forStorage).add(forPenalty));
		System.out.println(ANSI_GREEN + "  for CPU: " + forCPU);
		System.out.println("  for RAM: " + forRAM);
		System.out.println("  for storage: " + forStorage);
		System.out.println("  for penalty: " + forPenalty + ANSI_RESET);
	}

	protected void yesNo(String message) {
		System.out.print(message);
		@SuppressWarnings("resource")
		Scanner keyboard = new Scanner(System.in);
		String answer = keyboard.nextLine();
		if (!"Y".equals(answer))
			throw new CommandException("Stopped");
	}
}