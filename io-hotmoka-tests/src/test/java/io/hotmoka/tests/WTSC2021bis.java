/**
 *
 */
package io.hotmoka.tests;

import static io.hotmoka.beans.Coin.level2;
import static io.hotmoka.beans.Coin.level3;
import static io.hotmoka.beans.Coin.panarea;
import static io.hotmoka.beans.types.BasicTypes.BOOLEAN;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.InternalFailureException;
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
 * A test that performs repeated transfers between accounts of an ERC20 token.
 */
class WTSC2021bis extends TakamakaTest {
	private final static int NUMBER_OF_INVESTORS = 100;
	private final static int NUMBER_OF_TRANSFERS = 5;
	private final static int NUMBER_OF_ITERATIONS = 10;
    private final ClassType COIN = new ClassType("io.hotmoka.examples.tokens.ExampleCoin");
    private final ConstructorSignature CONSTRUCTOR_OF_COIN = new ConstructorSignature(COIN);
    private final MethodSignature TRANSFER = new NonVoidMethodSignature(ClassType.IERC20, "transfer", BOOLEAN, ClassType.CONTRACT, BasicTypes.INT);
    private final ClassType CREATOR = new ClassType("io.hotmoka.examples.tokens.ExampleCoinCreator");
    private final ConstructorSignature CONSTRUCTOR_OF_CREATOR = new ConstructorSignature(CREATOR, ClassType.BIG_INTEGER, ClassType.STRING);
    private final MethodSignature DISTRIBUTE = new VoidMethodSignature(CREATOR, "distribute", ClassType.ACCOUNTS, ClassType.IERC20, BasicTypes.INT);
    private StorageReference[] investors;
    private PrivateKey[] privateKeysOfInvestors;
    private StorageReference token;
	private final AtomicInteger numberOfTransactions = new AtomicInteger();
	private ExecutorService customThreadPool;

    @Test @DisplayName("performance test")
	void performanceTest() {
		System.out.printf("Performance test with %d investors of an ERC20 token, each making %d transfers, iterated %d times...\n", NUMBER_OF_INVESTORS, NUMBER_OF_TRANSFERS, NUMBER_OF_ITERATIONS);

		long start = System.currentTimeMillis();
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
			KeyPair keys = node.getSignatureAlgorithmForRequests().getKeyPair();
			PrivateKey privateKeyOfCreator = keys.getPrivate();
			String publicKey = Base64.getEncoder().encodeToString(keys.getPublic().getEncoded());
			StorageReference creator = addConstructorCallTransaction
				(privateKey(NUMBER_OF_INVESTORS), account(NUMBER_OF_INVESTORS), _10_000, ZERO, jar(), CONSTRUCTOR_OF_CREATOR,
				new BigIntegerValue(level2(500)), new StringValue(publicKey));

			investors = accounts().limit(NUMBER_OF_INVESTORS).toArray(StorageReference[]::new);
			privateKeysOfInvestors = privateKeys().limit(NUMBER_OF_INVESTORS).toArray(PrivateKey[]::new);

			// @creator creates the coin; initially, @creator will hold all tokens
			token = addConstructorCallTransaction(privateKeyOfCreator, creator, _100_000, panarea(1), jar(), CONSTRUCTOR_OF_COIN);

			// @creator makes a token transfer to each @investor (investors will now have tokens to trade)
			addInstanceMethodCallTransaction(privateKeyOfCreator, creator, _100_000.multiply(BigInteger.valueOf(NUMBER_OF_INVESTORS)), ZERO, jar(),
				DISTRIBUTE, creator, containerOfAccounts(), token, new IntValue(50_000));

			customThreadPool.submit(() -> IntStream.range(0, NUMBER_OF_INVESTORS).parallel().forEach(this::runTransfersForSender)).get();
		}
		catch (Exception e) {
			throw InternalFailureException.of(e);
		}
	}

    private void runTransfersForSender(int senderIndex) {
    	StorageReference sender = investors[senderIndex];
    	PrivateKey privateKeyOfSender = privateKeysOfInvestors[senderIndex];
    	Random random = new Random();

    	// choose 10 receivers randomly and send random tokens to them
    	random.ints(0, NUMBER_OF_INVESTORS).limit(NUMBER_OF_TRANSFERS)
    		.forEach(i -> createTransfer(sender, privateKeyOfSender, investors[i], 10 * (random.nextInt(5) + 1)));
    }

    /**
     * Transition that performs the transfer on ERC20
     */
    private boolean createTransfer(StorageReference sender, PrivateKey privateKeyOfSender, StorageReference receiver, int howMuch) {
    	BooleanValue transfer_result;
		try {
			transfer_result = (BooleanValue) addInstanceMethodCallTransaction(privateKeyOfSender, sender, _100_000, ZERO, jar(), TRANSFER, token, receiver, new IntValue(howMuch));
		}
		catch (Exception e) {
			throw InternalFailureException.of(e);
		}

    	numberOfTransactions.getAndIncrement();

    	return transfer_result.value;
    }
}