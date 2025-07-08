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

import static io.hotmoka.helpers.Coin.level2;
import static io.hotmoka.helpers.Coin.level3;
import static io.hotmoka.helpers.Coin.panarea;
import static io.hotmoka.node.StorageTypes.BOOLEAN;
import static java.math.BigInteger.ZERO;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.crypto.Base64;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.MisbehavingNodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnexpectedCodeException;
import io.hotmoka.node.api.UninitializedNodeException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;
import io.hotmoka.node.api.signatures.VoidMethodSignature;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.whitelisting.api.UnsupportedVerificationVersionException;

/**
 * A test that performs repeated transfers between accounts of an ERC20 token.
 */
class WTSC2021 extends HotmokaTest {
	private static int NUMBER_OF_INVESTORS = 100;
	private final static int NUMBER_OF_TRANSFERS = 5;
	private final static int NUMBER_OF_ITERATIONS = 10;
    private final ClassType COIN = StorageTypes.classNamed("io.hotmoka.examples.tokens.ExampleCoin");
    private final ConstructorSignature CONSTRUCTOR_OF_COIN = ConstructorSignatures.of(COIN);
    private final NonVoidMethodSignature TRANSFER = MethodSignatures.ofNonVoid(StorageTypes.IERC20, "transfer", BOOLEAN, StorageTypes.CONTRACT, StorageTypes.INT);
    private final ClassType CREATOR = StorageTypes.classNamed("io.hotmoka.examples.tokens.ExampleCoinCreator");
    private final ConstructorSignature CONSTRUCTOR_OF_CREATOR = ConstructorSignatures.of(CREATOR, StorageTypes.BIG_INTEGER, StorageTypes.STRING);
    private final VoidMethodSignature DISTRIBUTE = MethodSignatures.ofVoid(CREATOR, "distribute", StorageTypes.ACCOUNTS, StorageTypes.IERC20, StorageTypes.INT);
    private StorageReference[] investors;
    private PrivateKey[] privateKeysOfInvestors;
    private StorageReference token;
	private final AtomicInteger numberOfTransactions = new AtomicInteger();
	private ExecutorService customThreadPool;

	@BeforeAll
	static void beforeAll() {
		String cheapTests = System.getProperty("cheapTests");
		if ("true".equals(cheapTests)) {
			System.out.println("Running in cheap mode since cheapTests = true");
			NUMBER_OF_INVESTORS = 4;
		}
	}

	@Test @DisplayName("performance test")
	void performanceTest() {
    	long start = System.currentTimeMillis();

		System.out.printf("Performance test with %d investors of an ERC20 token, each making %d transfers, iterated %d times...\n", NUMBER_OF_INVESTORS, NUMBER_OF_TRANSFERS, NUMBER_OF_ITERATIONS);

		customThreadPool = new ForkJoinPool(NUMBER_OF_INVESTORS);
		IntStream.range(0, NUMBER_OF_ITERATIONS).forEach(this::iteration);
	    long elapsed = System.currentTimeMillis() - start;
	    customThreadPool.shutdownNow();
	
	    System.out.printf("did %s transactions in %.2fms [%d tx/s]\n", numberOfTransactions, elapsed / 1000.0, numberOfTransactions.get() * 1000L / elapsed);
	}

	private void iteration(int num) {
		System.out.println("iteration #" + num);

		try {
			setJar("tokens.jar");
			numberOfTransactions.getAndIncrement();

			setAccounts(Stream.generate(() -> level3(1)).limit(NUMBER_OF_INVESTORS + 1));
			numberOfTransactions.getAndIncrement();

			investors = accounts().limit(NUMBER_OF_INVESTORS).toArray(StorageReference[]::new);
			privateKeysOfInvestors = privateKeys().limit(NUMBER_OF_INVESTORS).toArray(PrivateKey[]::new);

			// the creator is created apart, since it has a different class
			KeyPair keys = signature().getKeyPair();
			PrivateKey privateKeyOfCreator = keys.getPrivate();
			String publicKey = Base64.toBase64String(signature().encodingOf(keys.getPublic()));
			StorageReference creator = addConstructorCallTransaction
				(privateKey(NUMBER_OF_INVESTORS), account(NUMBER_OF_INVESTORS), _50_000, ZERO, jar(), CONSTRUCTOR_OF_CREATOR,
				StorageValues.bigIntegerOf(level2(500)), StorageValues.stringOf(publicKey));

			// @creator creates the coin; initially, @creator will hold all tokens
			token = addConstructorCallTransaction(privateKeyOfCreator, creator, _500_000, panarea(1), jar(), CONSTRUCTOR_OF_COIN);

			// @creator makes a token transfer to each @investor (investors will now have tokens to trade)
			addInstanceVoidMethodCallTransaction(privateKeyOfCreator, creator, _100_000.multiply(BigInteger.valueOf(NUMBER_OF_INVESTORS)), ZERO, jar(),
				DISTRIBUTE, creator, containerOfAccounts(), token, StorageValues.intOf(50_000));

			customThreadPool.submit(() -> IntStream.range(0, NUMBER_OF_INVESTORS).parallel().forEach(this::runTransfersForSender)).get();
		}
		catch (InvalidKeyException | SignatureException | TransactionException | CodeExecutionException | TransactionRejectedException | ExecutionException
				| UnknownReferenceException | IOException | TimeoutException | NoSuchAlgorithmException | UninitializedNodeException | UnsupportedVerificationVersionException | ClosedNodeException | UnexpectedCodeException
				| NoSuchElementException | MisbehavingNodeException e) {
			throw new RuntimeException(e);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

    private void runTransfersForSender(int senderIndex) {
    	StorageReference sender = investors[senderIndex];
    	PrivateKey privateKeyOfSender = privateKeysOfInvestors[senderIndex];
    	var random = new Random(13011973);

    	// choose 5 receivers randomly and send random tokens to them
    	random.ints(0, NUMBER_OF_INVESTORS).limit(NUMBER_OF_TRANSFERS)
    		.forEach(i -> createTransfer(sender, privateKeyOfSender, investors[i], 10 * (random.nextInt(5) + 1)));
    }

    /**
     * Transition that performs the transfer on ERC20
     */
    private void createTransfer(StorageReference sender, PrivateKey privateKeyOfSender, StorageReference receiver, int howMuch) {
		try {
			addInstanceNonVoidMethodCallTransaction(privateKeyOfSender, sender, _500_000, ZERO, jar(), TRANSFER, token, receiver, StorageValues.intOf(howMuch));
		}
		catch (TimeoutException e) {
			// this occurs if the node is remote and very slow, so that the connection timeouts
		}
		catch (UnknownReferenceException | InvalidKeyException | SignatureException | TransactionException | CodeExecutionException | TransactionRejectedException | UnexpectedCodeException | ClosedNodeException e) {
			throw new RuntimeException(e);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

    	numberOfTransactions.getAndIncrement();
    }
}