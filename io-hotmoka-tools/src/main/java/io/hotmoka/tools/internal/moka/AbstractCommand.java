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

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.NonInitialTransactionResponse;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.responses.TransactionResponseFailed;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.Node;
import io.hotmoka.remote.RemoteNodeConfig;
import org.bouncycastle.asn1.ASN1BitString;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.*;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPair;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.NoSuchElementException;
import java.util.Scanner;

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

	protected String dumpKeys(StorageReference account, KeyPair keys) throws IOException {
		String fileName = fileFor(account);
	    
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
			oos.writeObject(keys);
		}



		return fileName;
	}

	protected KeyPair readKeys(StorageReference account) throws IOException, ClassNotFoundException {
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileFor(account)))) {
			return (KeyPair) ois.readObject();
		}
		catch (FileNotFoundException e) {
			throw new CommandException("Cannot find the keys of " + account);
		}
	}

	/**
	 * It exports the ED25519 key pair to PEM format.
	 * @param keyPair the key pair
	 * @param privateKeyFilename the name of the private key eg. account1.pri
	 * @param publicKeyFilename the name of the private key eg. account1.pub
	 */
	private static void ed25519toPemFrom(KeyPair keyPair, String privateKeyFilename, String publicKeyFilename) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(ASN1Primitive.fromByteArray(new PKCS8EncodedKeySpec(keyPair.getPrivate().getEncoded()).getEncoded()));

		ASN1Encodable privateKey = privateKeyInfo.parsePrivateKey();
		Ed25519PrivateKeyParameters privKeyParams = new Ed25519PrivateKeyParameters(((ASN1OctetString) privateKey).getOctets(), 0);

		ASN1BitString pubKeyData = privateKeyInfo.getPublicKeyData();
		Ed25519PublicKeyParameters pubKeyParams =  new Ed25519PublicKeyParameters(pubKeyData.getOctets(), 0);

		writePemFile(privKeyParams.getEncoded(), "PRIVATE KEY", privateKeyFilename);
		writePemFile(pubKeyParams.getEncoded(), "PUBLIC KEY", publicKeyFilename);
	}

	/**
	 * It exports the SHA256DSA key pair to PEM format.
	 * @param keyPair the key pair
	 * @param privateKeyFilename the name of the private key eg. account1.pri
	 * @param publicKeyFilename the name of the private key eg. account1.pub
	 */
	private static void sha256DSAtoPemFrom(KeyPair keyPair, String privateKeyFilename, String publicKeyFilename) throws Exception {
		writePemFile(keyPair.getPrivate(), "PRIVATE KEY", privateKeyFilename);
		writePemFile(keyPair.getPublic(), "PUBLIC KEY", publicKeyFilename);
	}

	private static void writePemFile(byte[] key, String description, String filename) throws Exception {
		PemObject pemObject = new PemObject(description, key);
		try(PemWriter pemWriter = new PemWriter(new OutputStreamWriter(new FileOutputStream(filename)))) {
			pemWriter.writeObject(pemObject);
		}

		System.out.println(filename + " exported successfully");
	}

	private static void writePemFile(Key key, String description, String filename) throws Exception {
		writePemFile(key.getEncoded(), description, filename);
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