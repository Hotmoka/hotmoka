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
import io.hotmoka.beans.values.BooleanValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;

/**
 * A test that performs repeated transfers between accounts of an ERC20 token, performing snapshots at regular intervals.
 */
class ExampleCoinSnapshotPerformance extends TakamakaTest {
    private ClassType COIN;
    private MethodSignature TRANSFER;
    private final MethodSignature TO_BIG_INTEGER = new NonVoidMethodSignature(UBI, "toBigInteger", ClassType.BIG_INTEGER);
    private final ClassType CREATOR = new ClassType("io.hotmoka.examples.tokens.ExampleCoinCreator");
    private static final ClassType UBI = ClassType.UNSIGNED_BIG_INTEGER;

    /**
     * Principals
     */
    private StorageReference creator; // The creator (and the owner) of the contract
    private PrivateKey privateKeyOfCreator;
    private StorageReference[] investors;
    private PrivateKey[] privateKeysOfInvestors;
    private final Random random = new Random(192846374);
    private StorageReference coin;
	private BigInteger gasConsumedForCPU;
	private BigInteger gasConsumedForRAM;
	private BigInteger gasConsumedForStorage;
	private final AtomicInteger numberOfTransfers = new AtomicInteger();
	private final AtomicInteger numberOfTransactions = new AtomicInteger();
	private static FileWriter nativeFile;
	private static FileWriter openZeppelinFile;

    /*
        #### STRUCTURE OF THE TRACED EXECUTION ###
        - @creator create the coin contract
        - @creator distributes an initial number of tokens to each @investor in #INVESTORS_NUMBER
        - For #DAYS_NUMBER  {
            - For @sender in #INVESTORS_NUMBER
               - @sender has a 1/10 chance of sending random tokens to 1/100 random other investors
            - @creator requests a snapshot
          }
    */

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
		if (accept(context)) {
			createCreator(context); // the creator is created apart, since it has a different class

			long start = System.currentTimeMillis();
			createCoin();
			distributeInitialTokens(context);
			letDaysPass(context);
			long elapsed = System.currentTimeMillis() - start;

			end(context, elapsed);
		}
	}

	private void letDaysPass(Context context) throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
		for (int day = 1; day <= context.numberOfSnapshots; day++)
	    	assertSame(nextDay(), day); // the snapshot identifier starts from 1
	}

	private void end(Context context, long elapsed) throws IOException {
		context.writeToFile(numberOfTransfers.get(), numberOfTransactions.get(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, elapsed);
	    System.out.printf("did %s transfers and %s transactions in %.2fs; consumed %d units of gas for CPU, %d for RAM and %d for storage\n",
	    	numberOfTransfers, numberOfTransactions, elapsed / 1000.0, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
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

	/**
	 * Initializes the state for the given test context.
	 * 
	 * @param context the context
	 * @return true if the test context is accepted, otherwise it must be skipped
	 */
	private boolean accept(Context context) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, TransactionRejectedException, TransactionException, CodeExecutionException {
		int numberOfInvestors = context.numberOfInvestors;
		if (tendermintBlockchain != null && numberOfInvestors > 100) // these take too much with Tendermint
			return false;

		System.out.printf("Performance test with %s... ", context);
		COIN = new ClassType(context.coinName);
		TRANSFER = new NonVoidMethodSignature(COIN, "transfer", BOOLEAN, ClassType.CONTRACT, BasicTypes.INT);
		gasConsumedForCPU = ZERO;
		gasConsumedForRAM = ZERO;
		gasConsumedForStorage = ZERO;
		numberOfTransactions.set(0);
		numberOfTransfers.set(0);
		// the last extra account is used only to create the creator of the token
		setAccounts(Stream.generate(() -> level3(1)).limit(numberOfInvestors + 1));
		investors = accounts().limit(numberOfInvestors).toArray(StorageReference[]::new);
	    privateKeysOfInvestors = privateKeys().limit(numberOfInvestors).toArray(PrivateKey[]::new);

	    return true;
	}

	private void distributeInitialTokens(Context context) throws InvalidKeyException, SignatureException, TransactionRejectedException, TransactionException, CodeExecutionException {
		InstanceMethodCallTransactionRequest request = new InstanceMethodCallTransactionRequest(Signer.with(signature(), privateKeyOfCreator), creator, ONE, chainId, _100_000.multiply(BigInteger.valueOf(context.numberOfInvestors)), ZERO, jar(),
    		new VoidMethodSignature(CREATOR, "distribute", ClassType.ACCOUNTS, ClassType.IERC20, BasicTypes.INT), creator, containerOfAccounts(), coin, new IntValue(50_000));
	    node.addInstanceMethodCallTransaction(request);
	    trace(request.getReference());
	}

	private void createCreator(Context context) throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		KeyPair keys = signature().getKeyPair();
	    privateKeyOfCreator = keys.getPrivate();
		String publicKey = Base64.getEncoder().encodeToString(keys.getPublic().getEncoded());
		int numberOfInvestors = context.numberOfInvestors;
		creator = addConstructorCallTransaction
			(privateKey(numberOfInvestors), account(numberOfInvestors), _50_000, ZERO, jar(), new ConstructorSignature(CREATOR, ClassType.BIG_INTEGER, ClassType.STRING),
			new BigIntegerValue(level2(500)), new StringValue(publicKey));
	}

	private void createCoin() throws InvalidKeyException, SignatureException, TransactionRejectedException, TransactionException, CodeExecutionException {
		ConstructorCallTransactionRequest request = new ConstructorCallTransactionRequest
	    	(Signer.with(signature(), privateKeyOfCreator), creator, ZERO, chainId, _500_000, panarea(1), jar(), new ConstructorSignature(COIN));
	    coin = node.addConstructorCallTransaction(request);
	    trace(request.getReference());
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

    /*
     * PS: All probabilities are actually decided in a fixed way (via a constant seed) so as not to change in different
     * executions. However, by changing the seeds we can observe and study different situations.
     */

    private static class Context {
    	private final String coinName;
    	private final int numberOfInvestors;
        private final int numberOfSnapshots;

    	private Context(String coinName, int numberOfInvestors, int numberOfSnapshots) {
    		this.coinName = coinName;
    		this.numberOfInvestors = numberOfInvestors;
    		this.numberOfSnapshots = numberOfSnapshots;
    	}

    	@Override
    	public String toString() {
    		if (coinName.equals("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots"))
    			return "native,       #investors = " + numberOfInvestors + ", #numberOfSnapshots = " + numberOfSnapshots;
    		else
    			return "OpenZeppelin, #investors = " + numberOfInvestors + ", #numberOfSnapshots = " + numberOfSnapshots;
    	}

    	private void writeToFile(int numberOfTransfers, int numberOfTransactions, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage, long time) throws IOException {
    		FileWriter fw;
    		if (coinName.equals("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots"))
    			fw = nativeFile;
    		else
    			fw = openZeppelinFile;

    		fw.write(String.format("  %d & %d & %d & %d & %d & %d & %d & %.2f\\\\\\hline\n",
    			numberOfInvestors, numberOfSnapshots, numberOfTransfers, numberOfTransactions,
    			gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, time / 1000.0));
    	}
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

    /**
     * Transition that performs the transfer on ERC20
     */
    private boolean createTransfer(StorageReference sender, PrivateKey privateKeyOfSender, StorageReference receiver, int howMuch)
    		throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {

    	InstanceMethodCallTransactionRequest request = new InstanceMethodCallTransactionRequest
    		(Signer.with(signature(), privateKeyOfSender), sender, getNonceOf(sender), chainId, _10_000_000, ZERO, jar(),
    		TRANSFER, coin, receiver, new IntValue(howMuch));

    	BooleanValue transfer_result = (BooleanValue) node.addInstanceMethodCallTransaction(request);

    	trace(request.getReference());

    	return transfer_result.value;
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

    private int convertUBItoInt(StorageReference ubi) throws TransactionException, CodeExecutionException, TransactionRejectedException {
    	BigIntegerValue bi = (BigIntegerValue) runInstanceMethodCallTransaction(creator, _50_000, jar(), TO_BIG_INTEGER, ubi);
        return bi.value.intValue();
    }

    private StorageReference createSnapshot() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        StorageReference result = (StorageReference) addInstanceMethodCallTransaction(privateKeyOfCreator, creator, _500_000, ZERO, jar(), new NonVoidMethodSignature(COIN, "yieldSnapshot", UBI), coin);
        trace(result.transaction);

        return result;
    }
}