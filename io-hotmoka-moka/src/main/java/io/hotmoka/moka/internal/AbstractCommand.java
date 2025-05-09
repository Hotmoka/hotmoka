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

package io.hotmoka.moka.internal;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

import io.hotmoka.crypto.Base58;
import io.hotmoka.crypto.Base58ConversionException;
import io.hotmoka.crypto.Base64;
import io.hotmoka.helpers.SignatureHelpers;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.Account;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.OutOfGasException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StringValue;

public abstract class AbstractCommand implements Runnable {
	public static final BigInteger _100_000 = BigInteger.valueOf(100_000L);
	public static final BigInteger _100_000_000 = BigInteger.valueOf(100_000_000L);

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
	 * @throws TransactionRejectedException
	 * @throws TransactionException
	 * @throws CodeExecutionException
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws InterruptedException 
	 * @throws TimeoutException 
	 * @throws UnknownReferenceException 
	 */
	protected KeyPair readKeys(Account account, Node node, String password) throws IOException, NoSuchAlgorithmException, InvalidKeyException, TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException, UnknownReferenceException {
		StorageReference reference = account.getReference();
		var algorithm = SignatureHelpers.of(node).signatureAlgorithmFor(reference);
		var keys = account.keys(password, algorithm);

		try {
			// we read the classpath of the account object
			TransactionReference classpath = node.getClassTag(reference).getJar();
			// we read the public key stored inside the account in the node (it is Base64-encoded)
			String publicKeyAsFound = ((StringValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(reference, _100_000, classpath, MethodSignatures.PUBLIC_KEY, reference))
				.orElseThrow(() -> new CommandException(MethodSignatures.PUBLIC_KEY + " should not return void"))).getValue();
			// we compare it with what we reconstruct from entropy and password
			String publicKeyAsGiven = Base64.toBase64String(algorithm.encodingOf(keys.getPublic()));
			if (!publicKeyAsGiven.equals(publicKeyAsFound))
				throw new IllegalArgumentException("Incorrect password");
		}
		catch (TransactionException e) {
			// we do not verify the password of the account if the access to its public key
			// costs too much gas (this happens for instance for qTesla accounts);
			// this means that, if the password is incorrect, the node will reject the transaction, not Moka
			if (!(e.getMessage().contains(OutOfGasException.class.getName())))
				throw e;
		}

		return keys;
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

	protected boolean looksLikePublicKey(String s) {
    	try {
            return s != null && Base58.fromBase58String(s).length == 32;
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
        catch (IllegalArgumentException t) {
            return false;
        }
    }

    protected void checkStorageReference(String s) {
		if (!looksLikeStorageReference(s))
			throw new IllegalArgumentException("you should specify a storage reference: 64 hex digits followed by # and a progressive number");
    }
}