package io.hotmoka.tests;

import static io.hotmoka.beans.Coin.level2;
import static io.hotmoka.beans.Coin.level3;
import static io.hotmoka.beans.Coin.panarea;
import static io.hotmoka.beans.types.BasicTypes.BOOLEAN;
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
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest.Signer;
import io.hotmoka.beans.responses.NonInitialTransactionResponse;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.nodes.NonceHelper;
import io.hotmoka.views.NodeWithAccounts;

/**
 * A test that performs repeated transfers between accounts of an ERC20 token, performing snapshots at regular intervals.
 */
class ExampleCoinSnapshotPerformance extends TakamakaTest {
    private static FileWriter nativeFile;
	private static FileWriter openZeppelinFile;

    @BeforeAll
	static void beforeAll() throws Exception {
		setJar("tokens.jar");
		
		nativeFile = new FileWriter("native.tex");
		writePreamble(nativeFile);
		openZeppelinFile = new FileWriter("open_zeppelin.tex");
		writePreamble(openZeppelinFile);
	}

    @AfterAll
    static void afterAll() throws Exception {
    	writeConclusion(nativeFile);
    	nativeFile.close();
    	writeConclusion(openZeppelinFile);
    	openZeppelinFile.close();
    }

    /**
     * The test contexts. Method {@link #performanceTest(Context)} will be executed for each of these contexts.
     */
    private static Stream<Context> contexts() {
		return Stream.of(
			new Context("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots", 100, 10),
			new Context("io.hotmoka.examples.tokens.ExampleCoinOZSnapshot", 100, 10),
			new Context("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots", 200, 10),
			new Context("io.hotmoka.examples.tokens.ExampleCoinOZSnapshot", 200, 10) /*,
			new Context("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots", 300, 10),
			new Context("io.hotmoka.examples.tokens.ExampleCoinOZSnapshot", 300, 10),
			new Context("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots", 400, 10),
			new Context("io.hotmoka.examples.tokens.ExampleCoinOZSnapshot", 400, 10),
			new Context("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots", 500, 10),
			new Context("io.hotmoka.examples.tokens.ExampleCoinOZSnapshot", 500, 10),
			new Context("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots", 600, 10),
			new Context("io.hotmoka.examples.tokens.ExampleCoinOZSnapshot", 600, 10),
			new Context("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots", 700, 10),
			new Context("io.hotmoka.examples.tokens.ExampleCoinOZSnapshot", 700, 10),
			new Context("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots", 800, 10),
			new Context("io.hotmoka.examples.tokens.ExampleCoinOZSnapshot", 800, 10),
			new Context("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots", 900, 10),
			new Context("io.hotmoka.examples.tokens.ExampleCoinOZSnapshot", 900, 10),
			new Context("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots", 1000, 10),
			new Context("io.hotmoka.examples.tokens.ExampleCoinOZSnapshot", 1000, 10)*/
		);
	}

	@ParameterizedTest @DisplayName("performance test") @MethodSource("contexts")
	void performanceTest(Context context) throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, IOException {
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
		return tendermintBlockchain == null || context.numberOfInvestors <= 100; // the others take too much with Tendermint
	}

	private static void writePreamble(FileWriter fw) throws IOException {
    	fw.write("\\documentclass{article}\n");
		fw.write("\\begin{document}\n");
		fw.write("\\begin{tabular}{|r|r||r|r||r|r|r||r|}\n");
		fw.write("  \\hline\n");
		fw.write("  \\#investors & \\#snapshots & \\#transfers & \\#transactions & CPU & RAM & storage & time \\\\\\hline\\hline\n");
    }

    private static void writeConclusion(FileWriter fw) throws IOException {
    	fw.write("\\end{tabular}\n");
    	fw.write("\\end{document}\n");
    }

    private static class Context {
    	private final String coinName;
    	private final int numberOfInvestors;
        private final int numberOfSnapshots;
        private final ClassType COIN;
        private final MethodSignature TRANSFER;
        private final MethodSignature YIELD_SNAPSHOT;
        private final static MethodSignature TO_BIG_INTEGER = new NonVoidMethodSignature(ClassType.UNSIGNED_BIG_INTEGER, "toBigInteger", ClassType.BIG_INTEGER);
        private final static ClassType CREATOR = new ClassType("io.hotmoka.examples.tokens.ExampleCoinCreator");
        private final Random random = new Random(192846374);
        private final NonceHelper nonceHelper = new NonceHelper(node);
		private StorageReference creator; // the creator (and owner) of the contract
        private PrivateKey privateKeyOfCreator;
        private StorageReference[] investors;
        private PrivateKey[] privateKeysOfInvestors;
        private StorageReference coin;
    	private BigInteger gasConsumedForCPU = ZERO;
    	private BigInteger gasConsumedForRAM = ZERO;
    	private BigInteger gasConsumedForStorage = ZERO;
    	private final AtomicInteger numberOfTransfers = new AtomicInteger();
    	private final AtomicInteger numberOfTransactions = new AtomicInteger();
		private NodeWithAccounts nodeWithAccounts;

    	private Context(String coinName, int numberOfInvestors, int numberOfSnapshots) {
    		this.coinName = coinName;
    		this.numberOfInvestors = numberOfInvestors;
    		this.numberOfSnapshots = numberOfSnapshots;
    		this.COIN = new ClassType(coinName);
    		this.TRANSFER = new NonVoidMethodSignature(COIN, "transfer", BOOLEAN, ClassType.CONTRACT, BasicTypes.INT);
    		this.YIELD_SNAPSHOT = new NonVoidMethodSignature(COIN, "yieldSnapshot", ClassType.UNSIGNED_BIG_INTEGER);
    	}

    	private void runTest() throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, TransactionRejectedException, TransactionException, CodeExecutionException, IOException {
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
    			return "native,       #investors = " + numberOfInvestors + ", #numberOfSnapshots = " + numberOfSnapshots;
    		else
    			return "OpenZeppelin, #investors = " + numberOfInvestors + ", #numberOfSnapshots = " + numberOfSnapshots;
    	}

    	/**
    	 * Initializes the state for this test context.
    	 */
    	private void init() throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, TransactionRejectedException, TransactionException, CodeExecutionException {
    		System.out.printf("Performance test with %s... ", this);
    		// the last extra account is used only to create the creator of the token
    		nodeWithAccounts = mkAccounts(Stream.generate(() -> level3(1)).limit(numberOfInvestors + 1));
    		investors = nodeWithAccounts.accounts().limit(numberOfInvestors).toArray(StorageReference[]::new);
    	    privateKeysOfInvestors = nodeWithAccounts.privateKeys().limit(numberOfInvestors).toArray(PrivateKey[]::new);
    	}

    	private void createCreator() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
    		KeyPair keys = signature().getKeyPair();
    	    privateKeyOfCreator = keys.getPrivate();
    		String publicKey = Base64.getEncoder().encodeToString(keys.getPublic().getEncoded());
    		ConstructorCallTransactionRequest request = new ConstructorCallTransactionRequest
    			(Signer.with(signature(), nodeWithAccounts.privateKey(numberOfInvestors)), nodeWithAccounts.account(numberOfInvestors), ZERO, chainId, _50_000, ZERO, jar(), new ConstructorSignature(CREATOR, ClassType.BIG_INTEGER, ClassType.STRING),
    			new BigIntegerValue(level2(500)), new StringValue(publicKey));
    		creator = node.addConstructorCallTransaction(request);
    	}

    	private void distributeInitialTokens() throws InvalidKeyException, SignatureException, TransactionRejectedException, TransactionException, CodeExecutionException {
    		InstanceMethodCallTransactionRequest request = new InstanceMethodCallTransactionRequest(Signer.with(signature(), privateKeyOfCreator), creator, ONE, chainId, _100_000.multiply(BigInteger.valueOf(numberOfInvestors)), ZERO, jar(),
        		new VoidMethodSignature(CREATOR, "distribute", ClassType.ACCOUNTS, ClassType.IERC20, BasicTypes.INT), creator, nodeWithAccounts.container(), coin, new IntValue(50_000));
    	    node.addInstanceMethodCallTransaction(request);
    	    trace(request.getReference());
    	}

    	private void createCoin() throws InvalidKeyException, SignatureException, TransactionRejectedException, TransactionException, CodeExecutionException {
    		ConstructorCallTransactionRequest request = new ConstructorCallTransactionRequest
    	    	(Signer.with(signature(), privateKeyOfCreator), creator, ZERO, chainId, _500_000, panarea(1), jar(), new ConstructorSignature(COIN));
    	    coin = node.addConstructorCallTransaction(request);
    	    trace(request.getReference());
    	}

    	private void letDaysPass() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
    		for (int day = 1; day <= numberOfSnapshots; day++)
    	    	assertSame(nextDay(), day); // the snapshot identifier starts from 1
    	}

    	/**
    	 * Performs the transactions for a day.
    	 * 
    	 * @return the id of the snapshot performed at the end of the day
    	 */
    	private int nextDay() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
    		IntStream.range(0, investors.length) //.parallel()
    			.forEach(this::runTransfersForSender);

    		return convertUBItoInt(createSnapshot());
    	}

    	private void runTransfersForSender(int senderIndex) {
        	// TODO: aggiungere mint e burn

        	// 1/10 of the senders send coins at each day
        	if (random.nextInt(10) == 0) {
        		StorageReference sender = investors[senderIndex];
            	PrivateKey privateKeyOfSender = privateKeysOfInvestors[senderIndex];

            	// we select 1/100 of the potential receivers
        		for (int howMany = investors.length / 100; howMany > 0; howMany--) {
        			int amount = 10 * (random.nextInt(5) + 1);
        			int receiverIndex;
        			
        			do {
        				receiverIndex = random.nextInt(investors.length);
        			}
        			while (receiverIndex == senderIndex);

        			StorageReference receiver = investors[receiverIndex];

        			try {
    					createTransfer(sender, privateKeyOfSender, receiver, amount);
    				}
    				catch (Exception e) {
    					throw InternalFailureException.of(e);
    				}

    				//assertTrue(transfer_result); // it is not mandatory to assert this (if a small amount of tokens have been distributed, investors may run out of tokens)
    				numberOfTransfers.getAndIncrement();
        		}
        	}
        }

    	private int convertUBItoInt(StorageReference ubi) throws TransactionException, CodeExecutionException, TransactionRejectedException {
    		InstanceMethodCallTransactionRequest request = new InstanceMethodCallTransactionRequest(creator, _50_000, jar(), TO_BIG_INTEGER, ubi);
        	BigIntegerValue bi = (BigIntegerValue) node.runInstanceMethodCallTransaction(request);
            return bi.value.intValue();
        }

        private StorageReference createSnapshot() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        	InstanceMethodCallTransactionRequest request = new InstanceMethodCallTransactionRequest
        		(Signer.with(signature(), privateKeyOfCreator), creator, nonceHelper.getNonceOf(creator), chainId, _500_000, ZERO, jar(), YIELD_SNAPSHOT, coin);
            StorageReference result = (StorageReference) node.addInstanceMethodCallTransaction(request);
            trace(result.transaction);

            return result;
        }

        /**
         * Transition that performs the transfer on ERC20
         */
        private void createTransfer(StorageReference sender, PrivateKey privateKeyOfSender, StorageReference receiver, int howMuch) throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        	InstanceMethodCallTransactionRequest request = new InstanceMethodCallTransactionRequest
        		(Signer.with(signature(), privateKeyOfSender), sender, nonceHelper.getNonceOf(sender), chainId, _10_000_000, ZERO, jar(),
        		TRANSFER, coin, receiver, new IntValue(howMuch));
        	node.addInstanceMethodCallTransaction(request);
        	trace(request.getReference());
        }

        private final Object tracingLock = new Object();

        private void trace(TransactionReference reference) throws TransactionRejectedException {
        	TransactionResponse response = node.getResponse(reference);

        	synchronized (tracingLock) {
        		if (response instanceof NonInitialTransactionResponse) {
        			NonInitialTransactionResponse nitr = (NonInitialTransactionResponse) response;
        			gasConsumedForCPU = gasConsumedForCPU.add(nitr.gasConsumedForCPU);
        			gasConsumedForRAM = gasConsumedForRAM.add(nitr.gasConsumedForRAM);
        			gasConsumedForStorage = gasConsumedForStorage.add(nitr.gasConsumedForStorage);
        		}
        	}

        	numberOfTransactions.getAndIncrement();
        }

        private void end(long elapsed) throws IOException {
        	@SuppressWarnings("resource")
			var fw = isNative() ? nativeFile : openZeppelinFile;
    		fw.write(String.format("  %d & %d & %s & %s & %d & %d & %d & %.2f\\\\\\hline\n",
    			numberOfInvestors, numberOfSnapshots, numberOfTransfers, numberOfTransactions, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, elapsed / 1000.0));

    		System.out.printf("did %s transfers and %s transactions in %.2fs; consumed %d units of gas for CPU, %d for RAM and %d for storage\n",
    	    	numberOfTransfers, numberOfTransactions, elapsed / 1000.0, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
    	}
    }
}