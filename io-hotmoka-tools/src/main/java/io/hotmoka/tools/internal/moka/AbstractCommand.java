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

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

import io.hotmoka.beans.MethodSignatures;
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.TransactionRequests;
import io.hotmoka.beans.api.requests.TransactionRequest;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.api.values.StringValue;
import io.hotmoka.crypto.Base58;
import io.hotmoka.crypto.Base58ConversionException;
import io.hotmoka.helpers.GasCounters;
import io.hotmoka.helpers.SignatureHelpers;
import io.hotmoka.node.OutOfGasError;
import io.hotmoka.node.api.Account;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.remote.RemoteNodeConfigBuilders;
import io.hotmoka.node.remote.api.RemoteNodeConfig;

public abstract class AbstractCommand implements Runnable {
	protected static final BigInteger _100_000 = BigInteger.valueOf(100_000L);
	protected static final BigInteger _100_000_000 = BigInteger.valueOf(100_000_000L);
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

	protected final static RemoteNodeConfig remoteNodeConfig(String url) {
		return RemoteNodeConfigBuilders.defaults().setURL(url).build();
	}

	/**
	 * Reconstructs the key pair of the given account, from the entropy contained in the PEM file with the name of the account.
	 * Uses the password to reconstruct the key pair and then checks that the reconstructed public key matches the key
	 * in the account stored in the node.
	 * 
	 * @param account the account
	 * @param node the node where the account exists
	 * @param password the password of the account
	 * @return the key pair
	 * @throws IllegalArgumentException if the password is not correct (it does  not match what stored in the account in the node)
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws ClassNotFoundException
	 * @throws TransactionRejectedException
	 * @throws TransactionException
	 * @throws CodeExecutionException
	 */
	protected KeyPair readKeys(Account account, Node node, String password) throws IOException, NoSuchAlgorithmException, ClassNotFoundException, InvalidKeyException, TransactionRejectedException, TransactionException, CodeExecutionException {
		StorageReference reference = account.getReference();
		var algorithm = SignatureHelpers.of(node).signatureAlgorithmFor(reference);
		var keys = account.keys(password, algorithm);

		try {
			// we read the classpath of the account object
			TransactionReference classpath = node.getClassTag(reference).getJar();
			// we read the public key stored inside the account in the node (it is Base64-encoded)
			String publicKeyAsFound = ((StringValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(reference, _100_000, classpath, MethodSignatures.PUBLIC_KEY, reference))).getValue();
			// we compare it with what we reconstruct from entropy and password
			String publicKeyAsGiven = Base64.getEncoder().encodeToString(algorithm.encodingOf(keys.getPublic())); // TODO
			if (!publicKeyAsGiven.equals(publicKeyAsFound))
				throw new IllegalArgumentException("Incorrect password");
		}
		catch (TransactionException e) {
			// we do not verify the password of the account if the access to its public key
			// costs too much gas (this happens for instance for qTesla accounts);
			// this means that, if the password is incorrect, the node will reject the transaction, not Moka
			if (!(e.getMessage().contains(OutOfGasError.class.getName())))
				throw e;
		}

		return keys;
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
		var gasCounter = GasCounters.of(node, requests);
		String result = ANSI_CYAN + "Total gas consumed: " + gasCounter.total() + "\n";
		result += ANSI_GREEN + "  for CPU: " + gasCounter.forCPU() + "\n";
		result += "  for RAM: " + gasCounter.forRAM() + "\n";
		result += "  for storage: " + gasCounter.forStorage() + "\n";
		result += "  for penalty: " + gasCounter.forPenalty() + ANSI_RESET;
		System.out.println(result);
	}

	protected void yesNo(String message) {
		System.out.print(message);
		@SuppressWarnings("resource")
		var keyboard = new Scanner(System.in);
		String answer = keyboard.nextLine();
		if (!"Y".equals(answer))
			throw new CommandException("Stopped");
	}

	protected String ask(String message) {
		System.out.print(message);
		return new String(System.console().readPassword());

		/*@SuppressWarnings("resource")
		Scanner keyboard = new Scanner(System.in);
		return keyboard.nextLine();*/
	}

	protected String ensurePassword(String password, String actor, boolean interactive, boolean isFaucet) {
		if (password != null && isFaucet)
			throw new IllegalArgumentException("the password of " + actor + " has no meaning when it is the faucet");
		
		if (password != null && interactive)
			throw new IllegalArgumentException("the password of " + actor + " can be provided as command switch only in non-interactive mode.");

		if (password == null && !isFaucet)
			if (!interactive) {
				System.out.println("Using the empty string as password of " + actor + ".");
				return "";
			}
			else
				return ask("Please specify the password of " + actor + ": ");
		else
			return password;
	}

	protected void printPassphrase(Account account) {
		System.out.println("Please take note of the following passphrase of 36 words,");
        System.out.println("you will need it to reinstall the account in this or another machine or application in the future:\n");
        AtomicInteger counter = new AtomicInteger(0);
        account.bip39Words().stream().forEachOrdered(word -> System.out.printf("%2d: %s\n", counter.incrementAndGet(), word));
	}

	protected boolean looksLikePublicKey(String s) {
    	try {
            return s != null && Base58.decode(s).length == 32;
        }
    	catch (Base58ConversionException e) {
            return false;
        }
    }

    protected boolean looksLikeStorageReference(String s) {
        try {
        	if (s == null)
        		return false;

        	StorageValues.reference(s);
            return true;
        }
        catch (Throwable t) {
            return false;
        }
    }

    protected void checkPublicKey(String s) {
    	if (!looksLikePublicKey(s))
			throw new IllegalArgumentException("you must specify a Base58-encoded key");
    }

    protected void checkStorageReference(String s) {
		if (!looksLikeStorageReference(s))
			throw new IllegalArgumentException("you should specify a storage reference: 64 hex digits followed by # and a progressive number");
    }
}