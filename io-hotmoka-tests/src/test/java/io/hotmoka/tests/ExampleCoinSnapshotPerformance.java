/**
 *
 */
package io.hotmoka.tests;

import static io.hotmoka.beans.Coin.level3;
import static io.hotmoka.beans.Coin.panarea;
import static io.hotmoka.beans.types.BasicTypes.BOOLEAN;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest.Signer;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.NonInitialTransactionResponse;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.BooleanValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.crypto.HashingAlgorithm;

/**
 * A test that performs repeated transfers between accounts of an ERC20 token, performing snapshots at regular intervals.
 */
class ExampleCoinSnapshotPerformance extends TakamakaTest {
    private ClassType COIN;
    private static final ClassType UBI = ClassType.UNSIGNED_BIG_INTEGER;
    private static final ConstructorSignature CONSTRUCTOR_UBI_STR = new ConstructorSignature(UBI, ClassType.STRING);

    /**
     * The classpath of the classes of code module.
     */
    private TransactionReference classpath_takamaka_code;

    /**
     * Principals
     */
    private StorageReference creator; // The creator (and the owner) of the contract
    private PrivateKey privateKeyOfCreator;
    private List<StorageReference> investors;
    private List<PrivateKey> privateKeysOfInvestors;

    /**
     * Seeds
     */
    // private final long SEED_DO_SNAPSHOT = 923428748;
    // private final Random Random_SEED_DO_SNAPSHOT = new Random(SEED_DO_SNAPSHOT);
    private final long SEED_SEND_A = 192846374;
    private final long SEED_SEND_B = 364579234;
    private final long SEED_TOKEN_MUL = 823645249;

	private BigInteger gasConsumedForCPU;
	private BigInteger gasConsumedForRAM;
	private BigInteger gasConsumedForStorage;
	private int numberOfTransactions;
	private static FileWriter nativeFile;
	private static FileWriter openZeppelinFile;

    /*
        #### STRUCTURE ###
        - @creator create the example_token contract
        - @creator initializes #INVESTORS_NUMBER EOA accounts EOA
        - For @investor in #INVESTORS_NUMBER {
                - @creator makes a token transfer to @investor (investors will now have tokens to trade)
            }
        - For #DAYS_NUMBER  {
            - For @sender in #INVESTORS_NUMBER {
               - @sender has a 1/10 chance of sending tokens to other investors [determined by the seed SEED_SEND_A] {
                   - For @receiver in #INVESTORS_NUMBER {
                        @sender has a 1/100 chance of sending tokens to @receiver [determined by the seed SEED_SEND_B] {
                            - @sender performs a transfer of X tokens to @receiver
                                with X=100*(number determined by the seed [determined by the seed SEED_TOKEN_MUL])
                        }
                     }
                 }
             }
            - At the end of each day @creator requests a snapshot
          }
    */

    @BeforeAll
	static void beforeAll() throws Exception {
		setJar("examplecoin.jar");
		
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
    			return "native, #investors = " + numberOfInvestors + ", #numberOfSnapshots = " + numberOfSnapshots;
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

    private static Stream<Context> contexts() {
    	return Stream.of(
    		new Context("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots", 10, 5)
    		/*new Context("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots", 100, 5),
    		new Context("io.hotmoka.examples.tokens.ExampleCoinOZSnapshot", 100, 5),
    		new Context("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots", 200, 5),
    		new Context("io.hotmoka.examples.tokens.ExampleCoinOZSnapshot", 200, 5) /*,
    		new Context("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots", 400, 5),
    		new Context("io.hotmoka.examples.tokens.ExampleCoinOZSnapshot", 400, 5),
    		new Context("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots", 800, 5),
    		new Context("io.hotmoka.examples.tokens.ExampleCoinOZSnapshot", 800, 5),
    		new Context("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots", 1600, 5),
    		new Context("io.hotmoka.examples.tokens.ExampleCoinOZSnapshot", 1600, 5),
    		new Context("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots", 3200, 5),
    		new Context("io.hotmoka.examples.tokens.ExampleCoinOZSnapshot", 3200, 5),
    		new Context("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots", 800, 5),
    		new Context("io.hotmoka.examples.tokens.ExampleCoinOZSnapshot", 800, 5),
    		new Context("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots", 800, 10),
    		new Context("io.hotmoka.examples.tokens.ExampleCoinOZSnapshot", 800, 10),
    		new Context("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots", 800, 20),
    		new Context("io.hotmoka.examples.tokens.ExampleCoinOZSnapshot", 800, 20),
    		new Context("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots", 800, 40),
    		new Context("io.hotmoka.examples.tokens.ExampleCoinOZSnapshot", 800, 40),
    		new Context("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots", 800, 80),
    		new Context("io.hotmoka.examples.tokens.ExampleCoinOZSnapshot", 800, 80),
    		new Context("io.hotmoka.examples.tokens.ExampleCoinWithSnapshots", 800, 160),
    		new Context("io.hotmoka.examples.tokens.ExampleCoinOZSnapshot", 800, 160)*/
    	);
    }

    @ParameterizedTest @DisplayName("performance test")
    @MethodSource("contexts")
    void performanceTest(Context context) throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, IOException {
    	int numberOfInvestors = context.numberOfInvestors;
    	int numberOfSnapshots = context.numberOfSnapshots;

    	/*if (tendermintBlockchain != null) {
			// the Tendermint blockchain is slower and requires more time for all transactions in this test
    		numberOfInvestors = 5;
			numberOfSnapshots = 4;
		}*/

    	System.out.printf("Performance test with %s... ", context);

    	Random Random_SEED_SEND_A = new Random(SEED_SEND_A);
    	Random Random_SEED_SEND_B = new Random(SEED_SEND_B);
        Random Random_SEED_TOKEN_MUL = new Random(SEED_TOKEN_MUL);
    	COIN = new ClassType(context.coinName);
    	ConstructorSignature constructorOfCoin = new ConstructorSignature(COIN);
    	gasConsumedForCPU = ZERO;
    	gasConsumedForRAM = ZERO;
    	gasConsumedForStorage = ZERO;
    	numberOfTransactions = 0;
    	hashingForRequests = HashingAlgorithm.sha256(Marshallable::toByteArray);
    	setAccounts(Stream.generate(() -> level3(1)).limit(numberOfInvestors + 1));
        classpath_takamaka_code = takamakaCode();
        creator = account(numberOfInvestors);
        privateKeyOfCreator = privateKey(numberOfInvestors);
        investors = accounts().limit(numberOfInvestors).collect(Collectors.toList());
        privateKeysOfInvestors = privateKeys().limit(numberOfInvestors).collect(Collectors.toList());

        long start = System.currentTimeMillis();
        StorageReference example_token = addConstructorCallTransaction(privateKeyOfCreator, creator, _100_000, panarea(1), jar(), constructorOfCoin);
        StorageReference ubi_50000 = createUBI(creator, privateKeyOfCreator, 50000);

        // @creator makes a token transf)er to @investor (investors will now have tokens to trade)
        for (StorageReference investor : investors) {
            boolean transfer_result = createTransfer(example_token, creator, privateKeyOfCreator, investor, ubi_50000);
            assertTrue(transfer_result);
        }

        int numberOfTransfers = 0;
        int snapshotId = 0;
        for (int day = 0; day < numberOfSnapshots; day++) {
        	int senderIndex = 0;
            for (StorageReference sender : investors) {
                // @sender has a 1/10 chance of sending tokens to other investors [determined by the seed SEED_SEND_A]
                if (Random_SEED_SEND_A.nextInt(10) == 0) {
                    for (StorageReference receiver : investors) {
                        // @sender has a 1/100 chance of sending tokens to @receiver [determined by the seed SEED_SEND_B]
                        if (Random_SEED_SEND_B.nextInt(100) == 0) {
                            // @sender performs a transfer of X tokens to @receiver
                            // with X=100*(number determined by the seed [determined by the seed SEED_TOKEN_MUL])
                            StorageReference ubi_x = createUBI(creator, privateKeyOfCreator, 10 * (Random_SEED_TOKEN_MUL.nextInt(5) + 1));
                            assertNotNull(ubi_x);
                            //boolean transfer_result =
                            createTransfer(example_token, sender, privateKeysOfInvestors.get(senderIndex), receiver, ubi_x);
                            //assertTrue(transfer_result); // It is not mandatory to assert this (if a small amount of tokens have been distributed, investors may run out of available tokens)
                            numberOfTransfers++;
                        }
                    }
                }

                senderIndex++;
            }
            // At the end of each day @creator requests a snapshot
            StorageReference snapshot_number_ubi = createSnapshot(example_token, creator, privateKeyOfCreator);

            BigInteger snapshotIdAsInContract = convertUBItoBI(creator, snapshot_number_ubi);
            snapshotId++;
            assertEquals(snapshotIdAsInContract.intValue(), snapshotId); // the snapshot identifier must always be incremented by 1 with respect to the previous one
        }

        long elapsed = System.currentTimeMillis() - start;

        context.writeToFile(numberOfTransfers, numberOfTransactions, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, elapsed);

        System.out.printf("did %d transfers and %d transactions in %.2fms; consumed %d units of gas for CPU, %d for RAM and %d for storage\n",
        	numberOfTransfers, numberOfTransactions, elapsed / 1000.0, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
    }

    /**
     * Transition that performs the transfer on ERC20
     */
    private boolean createTransfer(StorageReference token_contract,
                                             StorageReference sender, PrivateKey sender_key,
                                             StorageReference receiver, StorageReference ubi_token_value) throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {

    	InstanceMethodCallTransactionRequest request = new InstanceMethodCallTransactionRequest(Signer.with(node.getSignatureAlgorithmForRequests(), sender_key), sender, getNonceOf(sender), chainId, _100_000, ZERO, jar(),
    			new NonVoidMethodSignature(COIN, "transfer", BOOLEAN, ClassType.CONTRACT, UBI),
                token_contract, receiver, ubi_token_value);

    	BooleanValue transfer_result = (BooleanValue) node.addInstanceMethodCallTransaction(request);

    	trace(referenceOf(request));

    	return transfer_result.value;
    }

    private void trace(TransactionReference reference) throws TransactionRejectedException {
    	TransactionResponse response = node.getResponse(reference);
    	if (response instanceof NonInitialTransactionResponse) {
    		NonInitialTransactionResponse nitr = (NonInitialTransactionResponse) response;
    		gasConsumedForCPU = gasConsumedForCPU.add(nitr.gasConsumedForCPU);
    		gasConsumedForRAM = gasConsumedForRAM.add(nitr.gasConsumedForRAM);
    		gasConsumedForStorage = gasConsumedForStorage.add(nitr.gasConsumedForStorage);
    	}

    	numberOfTransactions++;
    }

    /**
	 * Yields the reference to the translation that would be originated for the given request.
	 * 
	 * @param request the request
	 * @return the transaction reference
	 */
	private LocalTransactionReference referenceOf(TransactionRequest<?> request) {
		return new LocalTransactionReference(bytesToHex(hashingForRequests.hash(request)));
	}

	/**
	 * Translates an array of bytes into a hexadecimal string.
	 * 
	 * @param bytes the bytes
	 * @return the string
	 */
	private static String bytesToHex(byte[] bytes) {
	    byte[] hexChars = new byte[bytes.length * 2];
	    int pos = 0;
	    for (byte b: bytes) {
	        int v = b & 0xFF;
	        hexChars[pos++] = HEX_ARRAY[v >>> 4];
	        hexChars[pos++] = HEX_ARRAY[v & 0x0F];
	    }
	
	    return new String(hexChars, StandardCharsets.UTF_8);
	}

	/**
	 * The string of the hexadecimal digits.
	 */
	private final static String HEX_CHARS = "0123456789abcdef";

	/**
	 * The array of hexadecimal digits.
	 */
	private final static byte[] HEX_ARRAY = HEX_CHARS.getBytes();

	/**
	 * The hashing algorithm for transaction requests.
	 */
	private HashingAlgorithm<? super TransactionRequest<?>> hashingForRequests;

	/**
     * UBI Creation Transition
     */
    private StorageReference createUBI(StorageReference account, PrivateKey account_key, int value) throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
    	StorageReference result = addConstructorCallTransaction(
                account_key, account,
                _10_000, ZERO, classpath_takamaka_code,
                CONSTRUCTOR_UBI_STR, new StringValue(String.valueOf(value)));

    	trace(result.transaction);

    	return result;
    }

    /**
     * Transaction to convert UBI to BI
     */
    private BigInteger convertUBItoBI(StorageReference account, StorageReference ubi) throws TransactionException, CodeExecutionException, TransactionRejectedException {
        BigIntegerValue bi = (BigIntegerValue) runInstanceMethodCallTransaction(
                account, _10_000, classpath_takamaka_code,
                new NonVoidMethodSignature(UBI, "toBigInteger", ClassType.BIG_INTEGER),
                ubi);
        return bi.value;
    }

    /**
     * Snapshot Request Transition
     */
    private StorageReference createSnapshot(StorageReference token_contract,
                                      StorageReference account, PrivateKey account_key) throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        StorageReference result = (StorageReference) addInstanceMethodCallTransaction(
                account_key, account,
                _100_000, ZERO, jar(),
                new NonVoidMethodSignature(COIN, "yieldSnapshot", UBI),
                token_contract);

        trace(result.transaction);

        return result;
    }
}