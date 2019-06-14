/**
 * 
 */
package takamaka.tests;

import static org.junit.jupiter.api.Assertions.fail;
import static takamaka.blockchain.types.BasicTypes.BYTE;
import static takamaka.blockchain.types.BasicTypes.INT;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import takamaka.blockchain.Classpath;
import takamaka.blockchain.CodeExecutionException;
import takamaka.blockchain.ConstructorSignature;
import takamaka.blockchain.MethodSignature;
import takamaka.blockchain.TransactionException;
import takamaka.blockchain.TransactionReference;
import takamaka.blockchain.request.ConstructorCallTransactionRequest;
import takamaka.blockchain.request.InstanceMethodCallTransactionRequest;
import takamaka.blockchain.request.JarStoreTransactionRequest;
import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.values.BigIntegerValue;
import takamaka.blockchain.values.ByteValue;
import takamaka.blockchain.values.IntValue;
import takamaka.blockchain.values.StorageReference;
import takamaka.lang.RequirementViolationException;
import takamaka.memory.InitializedMemoryBlockchain;

/**
 * A test for the blind auction contract.
 */
class BlindAuction {

	private static final BigInteger _1_000 = BigInteger.valueOf(1_000);

	private static final ClassType BLIND_AUCTION = new ClassType("takamaka.tests.auction.BlindAuction");

	private static final ConstructorSignature CONSTRUCTOR_BLIND_AUCTION = new ConstructorSignature(BLIND_AUCTION, INT, INT);

	private static final ConstructorSignature CONSTRUCTOR_BYTES32 = new ConstructorSignature
		(new ClassType("takamaka.util.Bytes32"),
			BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE,
			BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE,
			BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE,
			BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE);

	private static final MethodSignature BID = new MethodSignature(BLIND_AUCTION, "bid", ClassType.BIG_INTEGER, ClassType.BYTES32);

	private static final MethodSignature REVEAL = new MethodSignature(BLIND_AUCTION, "reveal", ClassType.STORAGE_LIST);

	private static final BigInteger _200_000 = BigInteger.valueOf(200_000);

	/**
	 * The blockchain under test. This is recreated before each test.
	 */
	private InitializedMemoryBlockchain blockchain;

	/**
	 * The classpath of the classes being tested.
	 */
	private Classpath classpath;

	/**
	 * The hashing algorithm used to hide the bids.
	 */
	private MessageDigest digest;

	@BeforeEach
	void beforeEach() throws Exception {
		digest = MessageDigest.getInstance("SHA-256");
		blockchain = new InitializedMemoryBlockchain(Paths.get("../takamaka_runtime/dist/takamaka_base.jar"), _200_000, _200_000, _200_000, _200_000);

		TransactionReference auctions = blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(blockchain.account(0), _200_000, blockchain.takamakaBase,
			Files.readAllBytes(Paths.get("../takamaka_examples/dist/auctions.jar")), blockchain.takamakaBase));

		classpath = new Classpath(auctions, true);
	}

	@Test @DisplayName("the three players put bids before end of bidding time")
	void bids() throws TransactionException, CodeExecutionException {
		StorageReference auction = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(blockchain.account(0), _1_000, classpath, CONSTRUCTOR_BLIND_AUCTION, new IntValue(40000), new IntValue(40000)));

		Random random = new Random();
		for (int i = 1; i <= 100; i++) {
			int player = 1 + random.nextInt(3);
			BigInteger deposit = BigInteger.valueOf(random.nextInt(1000));
			BigInteger value = BigInteger.valueOf(random.nextInt(1000));
			boolean fake = random.nextBoolean();
			byte[] salt = new byte[32];
			random.nextBytes(salt);
			digest.reset();
			digest.update(value.toByteArray());
        	digest.update(fake ? (byte) 0 : (byte) 1);
        	digest.update(salt);
        	byte[] hash = digest.digest();
        	StorageReference bytes32 = blockchain.addConstructorCallTransaction
       			(new ConstructorCallTransactionRequest(blockchain.account(0), _1_000, classpath, CONSTRUCTOR_BYTES32,
       			new ByteValue(hash[0]), new ByteValue(hash[1]), new ByteValue(hash[2]), new ByteValue(hash[3]),
       			new ByteValue(hash[4]), new ByteValue(hash[5]), new ByteValue(hash[6]), new ByteValue(hash[7]),
       			new ByteValue(hash[8]), new ByteValue(hash[9]), new ByteValue(hash[10]), new ByteValue(hash[11]),
       			new ByteValue(hash[12]), new ByteValue(hash[13]), new ByteValue(hash[14]), new ByteValue(hash[15]),
       			new ByteValue(hash[16]), new ByteValue(hash[17]), new ByteValue(hash[18]), new ByteValue(hash[19]),
       			new ByteValue(hash[20]), new ByteValue(hash[21]), new ByteValue(hash[22]), new ByteValue(hash[23]),
       			new ByteValue(hash[24]), new ByteValue(hash[25]), new ByteValue(hash[26]), new ByteValue(hash[27]),
       			new ByteValue(hash[28]), new ByteValue(hash[29]), new ByteValue(hash[30]), new ByteValue(hash[31])));

        	blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(blockchain.account(player), _1_000, classpath, BID, auction, new BigIntegerValue(deposit), bytes32));
		}
	}

	@Test @DisplayName("the three players put bids but bidding time expires")
	void biddingTimeExpires() throws TransactionException, CodeExecutionException {
		StorageReference auction = blockchain.addConstructorCallTransaction
				(new ConstructorCallTransactionRequest(blockchain.account(0), _1_000, classpath, CONSTRUCTOR_BLIND_AUCTION, new IntValue(4000), new IntValue(40000)));

		try {
			Random random = new Random();
			for (int i = 1; i <= 100; i++) {
				int player = 1 + random.nextInt(3);
				BigInteger deposit = BigInteger.valueOf(random.nextInt(1000));
				BigInteger value = BigInteger.valueOf(random.nextInt(1000));
				boolean fake = random.nextBoolean();
				byte[] salt = new byte[32];
				random.nextBytes(salt);
				digest.reset();
				digest.update(value.toByteArray());
				digest.update(fake ? (byte) 0 : (byte) 1);
				digest.update(salt);
				byte[] hash = digest.digest();
				StorageReference bytes32 = blockchain.addConstructorCallTransaction
					(new ConstructorCallTransactionRequest(blockchain.account(0), _1_000, classpath, CONSTRUCTOR_BYTES32,
						new ByteValue(hash[0]), new ByteValue(hash[1]), new ByteValue(hash[2]), new ByteValue(hash[3]),
						new ByteValue(hash[4]), new ByteValue(hash[5]), new ByteValue(hash[6]), new ByteValue(hash[7]),
						new ByteValue(hash[8]), new ByteValue(hash[9]), new ByteValue(hash[10]), new ByteValue(hash[11]),
						new ByteValue(hash[12]), new ByteValue(hash[13]), new ByteValue(hash[14]), new ByteValue(hash[15]),
						new ByteValue(hash[16]), new ByteValue(hash[17]), new ByteValue(hash[18]), new ByteValue(hash[19]),
						new ByteValue(hash[20]), new ByteValue(hash[21]), new ByteValue(hash[22]), new ByteValue(hash[23]),
						new ByteValue(hash[24]), new ByteValue(hash[25]), new ByteValue(hash[26]), new ByteValue(hash[27]),
						new ByteValue(hash[28]), new ByteValue(hash[29]), new ByteValue(hash[30]), new ByteValue(hash[31])));

				blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
						(blockchain.account(player), _1_000, classpath, BID, auction, new BigIntegerValue(deposit), bytes32));

				try {
					Thread.sleep(1000);
				}
				catch (InterruptedException e) {}
			}
		}
		catch (TransactionException e) {
			if (e.getCause() instanceof RequirementViolationException)
				return;

			fail("wrong exception");
		}

		fail("no exception");
	}
}