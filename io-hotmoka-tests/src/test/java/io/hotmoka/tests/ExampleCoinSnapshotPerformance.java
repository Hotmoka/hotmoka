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

import static io.hotmoka.beans.StorageTypes.BOOLEAN;
import static io.hotmoka.helpers.Coin.level2;
import static io.hotmoka.helpers.Coin.level3;
import static io.hotmoka.helpers.Coin.panarea;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.hotmoka.beans.ConstructorSignatures;
import io.hotmoka.beans.MethodSignatures;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.TransactionReferences;
import io.hotmoka.beans.TransactionRequests;
import io.hotmoka.beans.api.requests.SignedTransactionRequest;
import io.hotmoka.beans.api.requests.TransactionRequest;
import io.hotmoka.beans.api.responses.NonInitialTransactionResponse;
import io.hotmoka.beans.api.responses.TransactionResponse;
import io.hotmoka.beans.api.signatures.MethodSignature;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.api.values.BigIntegerValue;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.HashingAlgorithms;
import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.helpers.NonceHelpers;
import io.hotmoka.helpers.api.AccountsNode;
import io.hotmoka.helpers.api.NonceHelper;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;

/**
 * A test that performs repeated transfers between accounts of an ERC20 token, performing snapshots at regular intervals.
 */
class ExampleCoinSnapshotPerformance extends HotmokaTest {
	private final static int NUMBER_OF_DAYS = 10;
	private final static boolean PERFORM_SNAPSHOTS = true;
    private static FileWriter latexFile;
    private static Hasher<TransactionRequest<?>> hasher;

    @BeforeAll
	static void beforeAll() throws Exception {
    	hasher = HashingAlgorithms.sha256().getHasher(TransactionRequest::toByteArray);
		setJar("tokens.jar");
		latexFile = new FileWriter("erc20_snapshots_comparison.tex");
		writePreamble(latexFile);
	}

    @AfterAll
    static void afterAll() throws Exception {
    	writeConclusion(latexFile);
    	latexFile.close();
    }

    /**
     * The test contexts. Method {@link #performanceTest(Context)} will be executed for each of these contexts.
     */
    private static Stream<Context> contexts() {
		return Stream.of(
			new Context("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots", 100),
			new Context("io.hotmoka.examples.tokens.ExampleCoinOZSnapshot", 100),
			new Context("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots", 200),
			new Context("io.hotmoka.examples.tokens.ExampleCoinOZSnapshot", 200) /*,
			new Context("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots", 300),
			new Context("io.hotmoka.examples.tokens.ExampleCoinOZSnapshot", 300),
			new Context("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots", 400),
			new Context("io.hotmoka.examples.tokens.ExampleCoinOZSnapshot", 400),
			new Context("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots", 500),
			new Context("io.hotmoka.examples.tokens.ExampleCoinOZSnapshot", 500),
			new Context("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots", 600),
			new Context("io.hotmoka.examples.tokens.ExampleCoinOZSnapshot", 600),
			new Context("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots", 700),
			new Context("io.hotmoka.examples.tokens.ExampleCoinOZSnapshot", 700),
			new Context("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots", 800),
			new Context("io.hotmoka.examples.tokens.ExampleCoinOZSnapshot", 800),
			new Context("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots", 900),
			new Context("io.hotmoka.examples.tokens.ExampleCoinOZSnapshot", 900),
			new Context("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots", 1000),
			new Context("io.hotmoka.examples.tokens.ExampleCoinOZSnapshot", 1000)*/
		);
	}

	@ParameterizedTest @DisplayName("performance test") @MethodSource("contexts")
	void performanceTest(Context context) throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, IOException, NoSuchElementException, ClassNotFoundException, NodeException, TimeoutException, InterruptedException {
		if (accept(context))
			context.runTest();
	}

	/**
	 * Checks if the given context of test must be run.
	 * 
	 * @param context the context
	 * @return true if the test context is accepted, otherwise it must be skipped
	 */
	private boolean accept(Context context) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, TransactionRejectedException, TransactionException, CodeExecutionException {
		return !isUsingTendermint() || context.numberOfInvestors <= 100; // the others take too much with Tendermint
	}

	private static void writePreamble(FileWriter fw) throws IOException {
    	fw.write("%\\usepackage{rotating}\n");
		fw.write("\\begin{sidewaysfigure}\n");
		fw.write("\\begin{center}\n");
		fw.write("\\begin{tabular}{|c|r||r|r|r|r||r|r|r||r|}\n");
		fw.write("  \\hline\n");
		fw.write("  Implementation &\n");
		fw.write("  \\multicolumn{1}{|c||}{Investors} &\n");
		fw.write("  \\multicolumn{1}{c|}{Transfers} &\n");
		fw.write("  \\multicolumn{1}{c|}{Mints} &\n");
		fw.write("  \\multicolumn{1}{c|}{Burns} &\n");
		fw.write("  \\multicolumn{1}{c||}{Transactions} &\n");
		fw.write("  \\multicolumn{1}{c|}{CPU} &\n");
		fw.write("  \\multicolumn{1}{c|}{RAM} &\n");
		fw.write("  \\multicolumn{1}{c||}{Storage} &\n");
		fw.write("  \\multicolumn{1}{c|}{Time}\\\\\\hline\n");
    }

    private static void writeConclusion(FileWriter fw) throws IOException {
    	fw.write("\\end{tabular}\n");
    	fw.write("\\end{center}\n");
    	fw.write("\\caption{The result of running our test that\n");
    	fw.write(" simulates ten days of interaction with\n");
    	fw.write(" an ERC20 contract,\n");

    	if (PERFORM_SNAPSHOTS)
    		fw.write(" performing a snapshot at the end of each day.\n");
    	else
    		fw.write(" without performing any snapshot.\n");

    	fw.write(" \\emph{Implementation} is the implementation under test: native Takamaka with efficient snapshots or translated from OpenZeppelin into Takamaka.");
    	fw.write(" \\emph{Investors} is the number of\n");
    	fw.write(" accounts that invest in the ERC20 contract. \\emph{Transfers}, \\emph{Mints} and \\emph{Burns} are the number of\n");
    	fw.write(" transfer, mint and burn transactions performed during the test, respectively.\n");
    	fw.write(" \\emph{Transactions} is the total number of transactions performed by the test, including those for the creation and initialization of the \n");
    	fw.write(" ERC20 contract and for the computation of its snapshots.\n");
    	fw.write(" \\emph{CPU}, \\emph{RAM} and \\emph{Storage} are the gas units consumed for CPU execution, RAM allocations and\n");
    	fw.write(" persistent storage in blockchain, respectively. \\emph{Time} is the time for the execution of the test, in seconds.\n");
    	fw.write("}\n");

    	fw.write("\\label{fig:comparison}\n");

    	fw.write("\\end{sidewaysfigure}\n");
    }

    private static class Context {
    	private final String coinName;
    	private final int numberOfInvestors;
        private final ClassType COIN;
        private final MethodSignature TRANSFER;
        private final MethodSignature BURN;
        private final MethodSignature MINT;
        private final MethodSignature YIELD_SNAPSHOT;
        private final static MethodSignature TO_BIG_INTEGER = MethodSignatures.of(StorageTypes.UNSIGNED_BIG_INTEGER, "toBigInteger", StorageTypes.BIG_INTEGER);
        private final static ClassType CREATOR = StorageTypes.classNamed("io.hotmoka.examples.tokens.ExampleCoinCreator");
        private final Random random = new Random(192846374);
        private final NonceHelper nonceHelper = NonceHelpers.of(node);
		private StorageReference creator; // the creator (and owner) of the contract
        private PrivateKey privateKeyOfCreator;
        private StorageReference[] investors;
        private PrivateKey[] privateKeysOfInvestors;
        private StorageReference coin;
    	private BigInteger gasConsumedForCPU = ZERO;
    	private BigInteger gasConsumedForRAM = ZERO;
    	private BigInteger gasConsumedForStorage = ZERO;
    	private final AtomicInteger numberOfTransfers = new AtomicInteger();
    	private final AtomicInteger numberOfBurns = new AtomicInteger();
    	private final AtomicInteger numberOfMints = new AtomicInteger();
    	private final AtomicInteger numberOfTransactions = new AtomicInteger();
		private AccountsNode nodeWithAccounts;

    	private Context(String coinName, int numberOfInvestors) {
    		this.coinName = coinName;
    		this.numberOfInvestors = numberOfInvestors;
    		this.COIN = StorageTypes.classNamed(coinName);
    		this.TRANSFER = MethodSignatures.of(COIN, "transfer", BOOLEAN, StorageTypes.CONTRACT, StorageTypes.INT);
    		this.BURN = MethodSignatures.ofVoid(COIN, "burn", StorageTypes.CONTRACT, StorageTypes.INT);
    		this.MINT = MethodSignatures.ofVoid(COIN, "mint", StorageTypes.CONTRACT, StorageTypes.INT);
    		this.YIELD_SNAPSHOT = MethodSignatures.of(COIN, "yieldSnapshot", StorageTypes.UNSIGNED_BIG_INTEGER);
    	}

    	private void runTest() throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, TransactionRejectedException, TransactionException, CodeExecutionException, IOException, NoSuchElementException, ClassNotFoundException, NodeException, TimeoutException, InterruptedException {
    		init();
			createCreator(); // the creator is created apart, since it has a different class

			long start = System.currentTimeMillis();
			createCoin();
			distributeInitialTokens();
			letDaysPass();
			long elapsed = System.currentTimeMillis() - start;

			end(elapsed);
    	}

    	private boolean isNative() {
    		return "io.hotmoka.examples.tokens.ExampleCoinWithSnapshots".equals(coinName);
    	}

    	@Override
    	public String toString() {
    		if (isNative())
    			return "native,       #investors = " + numberOfInvestors;
    		else
    			return "OpenZeppelin, #investors = " + numberOfInvestors;
    	}

    	/**
    	 * Initializes the state for this test context.
    	 */
    	private void init() throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, TransactionRejectedException, TransactionException, CodeExecutionException, NoSuchElementException, ClassNotFoundException, NodeException, TimeoutException, InterruptedException {
    		System.out.printf("Performance test with %s... ", this);
    		// the last extra account is used only to create the creator of the token
    		nodeWithAccounts = mkAccounts(Stream.generate(() -> level3(1)).limit(numberOfInvestors + 1));
    		investors = nodeWithAccounts.accounts().limit(numberOfInvestors).toArray(StorageReference[]::new);
    	    privateKeysOfInvestors = nodeWithAccounts.privateKeys().limit(numberOfInvestors).toArray(PrivateKey[]::new);
    	}

    	private void createCreator() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
    		KeyPair keys = signature().getKeyPair();
    	    privateKeyOfCreator = keys.getPrivate();
    		String publicKey = Base64.toBase64String(signature().encodingOf(keys.getPublic()));
    		var request = TransactionRequests.constructorCall
    			(signature().getSigner(nodeWithAccounts.privateKey(numberOfInvestors), SignedTransactionRequest::toByteArrayWithoutSignature), nodeWithAccounts.account(numberOfInvestors), ZERO, chainId, _50_000, ZERO, jar(), ConstructorSignatures.of(CREATOR, StorageTypes.BIG_INTEGER, StorageTypes.STRING),
    			StorageValues.bigIntegerOf(level2(500)), StorageValues.stringOf(publicKey));
    		creator = node.addConstructorCallTransaction(request);
    	}

    	private void distributeInitialTokens() throws InvalidKeyException, SignatureException, TransactionRejectedException, TransactionException, CodeExecutionException, NoSuchElementException, NodeException, TimeoutException, InterruptedException {
    		var request = TransactionRequests.instanceMethodCall(signature().getSigner(privateKeyOfCreator, SignedTransactionRequest::toByteArrayWithoutSignature), creator, ONE, chainId, _100_000.multiply(BigInteger.valueOf(numberOfInvestors)), ZERO, jar(),
    			MethodSignatures.ofVoid(CREATOR, "distribute", StorageTypes.ACCOUNTS, StorageTypes.IERC20, StorageTypes.INT), creator, nodeWithAccounts.container(), coin, StorageValues.intOf(50_000));
    	    node.addInstanceMethodCallTransaction(request);
    	    trace(TransactionReferences.of(hasher.hash(request)));
    	}

    	private void createCoin() throws InvalidKeyException, SignatureException, TransactionRejectedException, TransactionException, CodeExecutionException, NoSuchElementException, NodeException, TimeoutException, InterruptedException {
    		var request = TransactionRequests.constructorCall
    	    	(signature().getSigner(privateKeyOfCreator, SignedTransactionRequest::toByteArrayWithoutSignature), creator, ZERO, chainId, _500_000, panarea(1), jar(), ConstructorSignatures.of(COIN));
    	    coin = node.addConstructorCallTransaction(request);
    	    trace(TransactionReferences.of(hasher.hash(request)));
    	}

    	private void letDaysPass() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException, NoSuchElementException, NodeException, InterruptedException, TimeoutException {
    		for (int day = 1; day <= NUMBER_OF_DAYS; day++)
    			if (PERFORM_SNAPSHOTS)
    				assertSame(nextDay(), day); // the snapshot identifier starts from 1
    			else
    				nextDay();
    	}

    	/**
    	 * Performs the transactions for a day.
    	 * 
    	 * @return the id of the snapshot performed at the end of the day
    	 */
    	private int nextDay() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException, NoSuchElementException, NodeException, InterruptedException, TimeoutException {
    		IntStream.range(0, investors.length).forEach(this::actionsOfAccount);
    		return PERFORM_SNAPSHOTS ? convertUBItoInt(createSnapshot()) : 0;
    	}

    	private void actionsOfAccount(int index) {
    		StorageReference account = investors[index];

    		try {
    			switch (random.nextInt(10)) {

    			case 0:
    			case 1: // the account sends tokens to another random account: 20% of probability
    				PrivateKey privateKeyOfSender = privateKeysOfInvestors[index];

    				// we select 1/100 of the potential receivers
    				for (int howMany = investors.length / 100; howMany > 0; howMany--) {
    					int amount = 10 * (random.nextInt(5) + 1);
    					int receiverIndex;

    					do {
    						receiverIndex = random.nextInt(investors.length);
    					}
    					while (receiverIndex == index);

    					// the following might fail if the account runs out of tokens, but that's fine
    					transfer(account, privateKeyOfSender, investors[receiverIndex], amount);
    				}

    				break;

    			case 5: // some coins of the account get burnt: 10% of probability
    				burn(account, 10 * (random.nextInt(5) + 1));
    				break;

    			case 7: // some coins of the account get minted: 10% of probability
    				mint(account, 10 * (random.nextInt(5) + 1));
    				break;
    			}
    		}
    		catch (InvalidKeyException | SignatureException | TransactionRejectedException | TransactionException | CodeExecutionException | NodeException | NoSuchElementException | InterruptedException | TimeoutException e) {
    			throw new RuntimeException(e);
    		}
    	}

    	/**
		 * Transition that performs the transfer on ERC20
    	 * @throws NodeException 
    	 * @throws NoSuchElementException 
    	 * @throws TimeoutException 
    	 * @throws InterruptedException 
		 */
		private void transfer(StorageReference sender, PrivateKey privateKeyOfSender, StorageReference receiver, int howMuch) throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException, NoSuchElementException, NodeException, InterruptedException, TimeoutException {
			var request = TransactionRequests.instanceMethodCall
				(signature().getSigner(privateKeyOfSender, SignedTransactionRequest::toByteArrayWithoutSignature), sender, nonceHelper.getNonceOf(sender), chainId, _10_000_000, ZERO, jar(),
				TRANSFER, coin, receiver, StorageValues.intOf(howMuch));
			node.addInstanceMethodCallTransaction(request);
			trace(TransactionReferences.of(hasher.hash(request)));
			numberOfTransfers.getAndIncrement();
		}

		private void burn(StorageReference victim, int howMuch) throws InvalidKeyException, SignatureException, NoSuchElementException, TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, InterruptedException, TimeoutException {
    		var request = TransactionRequests.instanceMethodCall
    			(signature().getSigner(privateKeyOfCreator, SignedTransactionRequest::toByteArrayWithoutSignature), creator, nonceHelper.getNonceOf(creator), chainId, _10_000_000, ZERO, jar(), BURN, coin, victim, StorageValues.intOf(howMuch));
            node.addInstanceMethodCallTransaction(request);
            trace(TransactionReferences.of(hasher.hash(request)));
            numberOfBurns.getAndIncrement();
		}

    	private void mint(StorageReference beneficiary, int howMuch) throws InvalidKeyException, SignatureException, NoSuchElementException, TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, InterruptedException, TimeoutException {
    		var request = TransactionRequests.instanceMethodCall
    			(signature().getSigner(privateKeyOfCreator, SignedTransactionRequest::toByteArrayWithoutSignature), creator, nonceHelper.getNonceOf(creator), chainId, _10_000_000, ZERO, jar(), MINT, coin, beneficiary, StorageValues.intOf(howMuch));
            node.addInstanceMethodCallTransaction(request);
            trace(TransactionReferences.of(hasher.hash(request)));
            numberOfMints.getAndIncrement();
		}

    	private int convertUBItoInt(StorageReference ubi) throws TransactionException, CodeExecutionException, TransactionRejectedException {
    		var request = TransactionRequests.instanceViewMethodCall(creator, _50_000, jar(), TO_BIG_INTEGER, ubi);
        	BigIntegerValue bi = (BigIntegerValue) node.runInstanceMethodCallTransaction(request);
            return bi.getValue().intValue();
        }

        private StorageReference createSnapshot() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException, NoSuchElementException, NodeException, InterruptedException, TimeoutException {
        	var request = TransactionRequests.instanceMethodCall
        		(signature().getSigner(privateKeyOfCreator, SignedTransactionRequest::toByteArrayWithoutSignature), creator, nonceHelper.getNonceOf(creator), chainId, _500_000, ZERO, jar(), YIELD_SNAPSHOT, coin);
            StorageReference result = (StorageReference) node.addInstanceMethodCallTransaction(request);
            trace(result.getTransaction());

            return result;
        }

        private final Object tracingLock = new Object();

        private void trace(TransactionReference reference) throws TransactionRejectedException, NoSuchElementException, NodeException, TimeoutException, InterruptedException {
        	TransactionResponse response = node.getResponse(reference);

        	synchronized (tracingLock) {
        		if (response instanceof NonInitialTransactionResponse) {
        			NonInitialTransactionResponse nitr = (NonInitialTransactionResponse) response;
        			gasConsumedForCPU = gasConsumedForCPU.add(nitr.getGasConsumedForCPU());
        			gasConsumedForRAM = gasConsumedForRAM.add(nitr.getGasConsumedForRAM());
        			gasConsumedForStorage = gasConsumedForStorage.add(nitr.getGasConsumedForStorage());
        		}
        	}

        	numberOfTransactions.getAndIncrement();
        }

        private void end(long elapsed) throws IOException {
			var fw = latexFile;

        	if (isNative())
        		fw.write("\\hline\n\\textbf{Native} & ");
        	else
        		fw.write("\\textbf{OpenZeppelin} & ");

        	fw.write(String.format("%d & %s & %s & %s & %s & \\textbf{%d} & \\textbf{%d} & \\textbf{%d} & \\textbf{%.2f}\\\\\\hline\n",
       			numberOfInvestors, numberOfTransfers, numberOfMints, numberOfBurns, numberOfTransactions, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, elapsed / 1000.0));

        	System.out.printf("did %s transfers, %s mints, %s burns and %s transactions in %.2fs; consumed %d units of gas for CPU, %d for RAM and %d for storage\n",
    	    	numberOfTransfers, numberOfMints, numberOfBurns, numberOfTransactions, elapsed / 1000.0, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
    	}
    }
}