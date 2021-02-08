/**
 *
 */
package io.hotmoka.tests;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.BooleanValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;

import static io.hotmoka.beans.Coin.*;
import static io.hotmoka.beans.types.BasicTypes.BOOLEAN;
import static org.junit.jupiter.api.Assertions.*;

class ExampleCoinSnapshotPerformances extends TakamakaTest {
    //private static final ClassType COIN = new ClassType("io.hotmoka.tests.tokens.ExampleCoinOZSnapshot");
    private static final ClassType COIN = new ClassType("io.hotmoka.tests.tokens.ExampleCoinAccessibleSnapshot");
    private static final ClassType EOA = new ClassType("io.takamaka.code.lang.ExternallyOwnedAccount");
    private static final ClassType UBI = new ClassType("io.takamaka.code.math.UnsignedBigInteger");
    private static final ConstructorSignature CONSTRUCTOR_COIN = new ConstructorSignature(COIN);
    private static final ConstructorSignature CONSTRUCTOR_UBI_STR = new ConstructorSignature(UBI, ClassType.STRING);
    private static final BigInteger _10_000 = BigInteger.valueOf(10_000);
    private static final BigInteger _100_000 = BigInteger.valueOf(100_000);
    private static final BigInteger _200_000 = BigInteger.valueOf(200_000);
    private static final BigInteger _1_000_000 = BigInteger.valueOf(1_000_000);
    private static final BigInteger _1_000_000_000 = new BigInteger("1000000000");

    /**
     * The classpath of the classes of code module.
     */
    private TransactionReference classpath_takamaka_code;

    /**
     * Principals
     */
    private StorageReference creator; // The creator (and the owner) of the contract
    private PrivateKey creator_prv_key;
    private final List<StorageReference> investors = new ArrayList<>(); // Investors
    private final List<PrivateKey> investors_prv_key = new ArrayList<>();

    /**
     * Seeds
     */
    // public final long SEED_DO_SNAPSHOT = 923428748;
    // public final Random Random_SEED_DO_SNAPSHOT = new Random(SEED_DO_SNAPSHOT);
    public final long SEED_SEND_A = 192846374;
    public final Random Random_SEED_SEND_A = new Random(SEED_SEND_A);
    public final long SEED_SEND_B = 364579234;
    public final Random Random_SEED_SEND_B = new Random(SEED_SEND_B);
    public final long SEED_TOKEN_MUL = 823645249;
    public final Random Random_SEED_TOKEN_MUL = new Random(SEED_TOKEN_MUL);

    /**
     * Settings
     */
    public final int INVESTORS_NUMBER = 3; // min 1
    public final int DAYS_NUMBER = 2; // at the end of each "day" a snapshot is taken by the creator

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

    /*
     * PS: All probabilities are actually decided in a fixed way (via a constant seed) so as not to change in different
     * executions. However, by changing the seeds we can observe and study different situations.
     */

    @BeforeEach
    void beforeEach() throws Exception {
        setNode("examplecoin.jar", stromboli(1), filicudi(100), filicudi(100), filicudi(100));
        classpath_takamaka_code = takamakaCode();

        /*
         * To make a real performance test it is necessary to have a high quantity of gas available (for this reason
         * the gamete must be enabled directly to distribute the gas to the various investors).
         * Instead of distributing 1 million coins, assign at least 1 billion, as indicated below.
         */
        // creator = getGamete();
        // creator_prv_key = getPrivateKeyOfGamete();
        creator = account(1);
        creator_prv_key = privateKey(1);

        for (int investor = 0; investor < INVESTORS_NUMBER; investor++) {
            KeyPair keys = signature().getKeyPair();

            // @creator creates EOA accounts with a certain amount of coins
            String publicKey = Base64.getEncoder().encodeToString(keys.getPublic().getEncoded());
            StorageReference account = addConstructorCallTransaction(creator_prv_key, creator,
                    _200_000, panarea(1), classpath_takamaka_code,
                    new ConstructorSignature(EOA, ClassType.BIG_INTEGER, ClassType.STRING),
                    new BigIntegerValue(_100_000), new StringValue(publicKey)); // For real performance tests distribute at least _1_000_000_000

            investors.add(account);
            investors_prv_key.add(keys.getPrivate());
        }
    }

    @Test @DisplayName("Performances Test")
    void performanceTest() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
        StorageReference example_token = addConstructorCallTransaction(creator_prv_key, creator, _200_000, panarea(1), jar(), CONSTRUCTOR_COIN);
        StorageReference ubi_50000 = createUBI(creator, creator_prv_key, 50000);

        // @creator makes a token transfer to @investor (investors will now have tokens to trade)
        for (StorageReference investor : investors) {
            boolean transfer_result = createTransfer(example_token, creator, creator_prv_key, investor, ubi_50000);
            assertTrue(transfer_result);
        }

        int transfers_number = 0;
        BigInteger snapshot_id_check = BigInteger.valueOf(1);
        for (int day = 0; day < DAYS_NUMBER; day++) {
            for (StorageReference sender : investors) {
                // @sender has a 1/10 chance of sending tokens to other investors [determined by the seed SEED_SEND_A]
                if (Random_SEED_SEND_A.nextInt(10) == 0) {
                    for (StorageReference receiver : investors) {
                        // @sender has a 1/100 chance of sending tokens to @receiver [determined by the seed SEED_SEND_B]
                        if (Random_SEED_SEND_B.nextInt(100) == 0) {
                            // @sender performs a transfer of X tokens to @receiver
                            // with X=100*(number determined by the seed [determined by the seed SEED_TOKEN_MUL])
                            StorageReference ubi_x = createUBI(creator, creator_prv_key, 10 * (Random_SEED_TOKEN_MUL.nextInt(5) + 1));
                            assertNotNull(ubi_x);
                            boolean transfer_result = createTransfer(example_token, sender,
                                    investors_prv_key.get(investors.indexOf(sender)), receiver, ubi_x);
                            // assertTrue(transfer_result); // It is not mandatory to assert this (if a small amount of tokens have been distributed, investors may run out of available tokens)
                            transfers_number++;
                        }
                    }
                }
            }
            // At the end of each day @creator requests a snapshot
            StorageReference snapshot_number_ubi = createSnapshot(example_token, creator, creator_prv_key);

            BigInteger snapshot_number_bi = convertUBItoBI(creator, snapshot_number_ubi);
            assertEquals(snapshot_number_bi, snapshot_id_check); // the snapshot identifier must always be incremented by 1 with respect to the previous one
            snapshot_id_check = snapshot_id_check.add(BigInteger.valueOf(1));
        }
        // System.out.println("Number of Screenshots: " + snapshot_id_check.subtract(BigInteger.valueOf(1)));
        // System.out.println("Number of Transfers: " + transfers_number);
    }

    /**
     * Transition that performs the transfer on ERC20
     */
    public boolean createTransfer(StorageReference token_contract,
                                             StorageReference sender, PrivateKey sender_key,
                                             StorageReference receiver, StorageReference ubi_token_value) throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        BooleanValue transfer_result = (BooleanValue) addInstanceMethodCallTransaction(
                sender_key, sender,
                _200_000, panarea(1), jar(),
                new NonVoidMethodSignature(COIN, "transfer", BOOLEAN, ClassType.CONTRACT, UBI),
                token_contract, receiver, ubi_token_value);
        return transfer_result.value;
    }

    /**
     * UBI Creation Transition
     */
    public StorageReference createUBI(StorageReference account, PrivateKey account_key, int value) throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        return addConstructorCallTransaction(
                account_key, account,
                _100_000, panarea(1), classpath_takamaka_code,
                CONSTRUCTOR_UBI_STR, new StringValue(value + ""));
    }

    /**
     * Transaction to convert UBI to BI
     */
    public BigInteger convertUBItoBI(StorageReference account, StorageReference ubi) throws TransactionException, CodeExecutionException, TransactionRejectedException {
        BigIntegerValue bi = (BigIntegerValue) runInstanceMethodCallTransaction(
                account, _200_000, classpath_takamaka_code,
                new NonVoidMethodSignature(UBI, "toBigInteger", ClassType.BIG_INTEGER),
                ubi);
        return bi.value;
    }

    /**
     * Snapshot Request Transition
     */
    public StorageReference createSnapshot(StorageReference token_contract,
                                      StorageReference account, PrivateKey account_key) throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        return (StorageReference) addInstanceMethodCallTransaction(
                account_key, account,
                _200_000, panarea(1), jar(),
                new NonVoidMethodSignature(COIN, "yieldSnapshot", UBI),
                token_contract);
    }
}