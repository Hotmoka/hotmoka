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

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
    private static final ClassType COIN = new ClassType("io.hotmoka.tests.tokens.ExampleCoinAccessibleSnapshot");
    private static final ClassType UBI = ClassType.UNSIGNED_BIG_INTEGER;
    private static final ConstructorSignature CONSTRUCTOR_COIN = new ConstructorSignature(COIN);
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
    private final Random Random_SEED_SEND_A = new Random(SEED_SEND_A);
    private final long SEED_SEND_B = 364579234;
    private final Random Random_SEED_SEND_B = new Random(SEED_SEND_B);
    private final long SEED_TOKEN_MUL = 823645249;
    private final Random Random_SEED_TOKEN_MUL = new Random(SEED_TOKEN_MUL);

    /**
     * Settings
     */
    private int INVESTORS_NUMBER = 400; // min 1
    private int DAYS_NUMBER = 10; // at the end of each "day" a snapshot is taken by the creator
	private BigInteger gasConsumedForCPU;
	private BigInteger gasConsumedForRAM;
	private BigInteger gasConsumedForStorage;

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
	}

    /*
     * PS: All probabilities are actually decided in a fixed way (via a constant seed) so as not to change in different
     * executions. However, by changing the seeds we can observe and study different situations.
     */

    @BeforeEach
    void beforeEach() throws Exception {
    	if (tendermintBlockchain != null) {
			// the Tendermint blockchain is slower and requires more time for all transactions in this test
    		INVESTORS_NUMBER = 5;
			DAYS_NUMBER = 4;
		}

    	gasConsumedForCPU = ZERO;
    	gasConsumedForRAM = ZERO;
    	gasConsumedForStorage = ZERO;
    	hashingForRequests = HashingAlgorithm.sha256(Marshallable::toByteArray);
    	setAccounts(Stream.generate(() -> level3(1)).limit(INVESTORS_NUMBER + 1));
        classpath_takamaka_code = takamakaCode();
        creator = account(INVESTORS_NUMBER);
        privateKeyOfCreator = privateKey(INVESTORS_NUMBER);
        investors = accounts().limit(INVESTORS_NUMBER).collect(Collectors.toList());
        privateKeysOfInvestors = privateKeys().limit(INVESTORS_NUMBER).collect(Collectors.toList());
    }

    @Test @DisplayName("Performance test")
    void performanceTest() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
    	System.out.printf("Performance test with %d investors along %d days... ", INVESTORS_NUMBER, DAYS_NUMBER);
        StorageReference example_token = addConstructorCallTransaction(privateKeyOfCreator, creator, _100_000, panarea(1), jar(), CONSTRUCTOR_COIN);
        StorageReference ubi_50000 = createUBI(creator, privateKeyOfCreator, 50000);

        // @creator makes a token transfer to @investor (investors will now have tokens to trade)
        for (StorageReference investor : investors) {
            boolean transfer_result = createTransfer(example_token, creator, privateKeyOfCreator, investor, ubi_50000);
            assertTrue(transfer_result);
        }

        int numberOfTransfers = 0;
        int snapshotId = 0;
        for (int day = 0; day < DAYS_NUMBER; day++) {
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
                            boolean transfer_result =
                            createTransfer(example_token, sender, privateKeysOfInvestors.get(senderIndex), receiver, ubi_x);
                            assertTrue(transfer_result); // It is not mandatory to assert this (if a small amount of tokens have been distributed, investors may run out of available tokens)
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

        System.out.printf("did %d snapshots and %d transfers; consumed %d units of gas for CPU, %d for RAM and %d for storage\n", snapshotId, numberOfTransfers, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
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

    	traceGas(referenceOf(request));

    	return transfer_result.value;
    }

    private void traceGas(TransactionReference reference) throws TransactionRejectedException {
    	TransactionResponse response = node.getResponse(reference);
    	if (response instanceof NonInitialTransactionResponse) {
    		NonInitialTransactionResponse nitr = (NonInitialTransactionResponse) response;
    		gasConsumedForCPU = gasConsumedForCPU.add(nitr.gasConsumedForCPU);
    		gasConsumedForRAM = gasConsumedForRAM.add(nitr.gasConsumedForRAM);
    		gasConsumedForStorage = gasConsumedForStorage.add(nitr.gasConsumedForStorage);
    	}
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

    	traceGas(result.transaction);

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

        traceGas(result.transaction);

        return result;
    }
}