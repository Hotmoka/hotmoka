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

package io.hotmoka.tutorial.examples.runs;

import static io.hotmoka.helpers.Coin.panarea;
import static io.hotmoka.node.StorageTypes.BIG_INTEGER;
import static io.hotmoka.node.StorageTypes.BOOLEAN;
import static io.hotmoka.node.StorageTypes.BYTE;
import static io.hotmoka.node.StorageTypes.BYTES32_SNAPSHOT;
import static io.hotmoka.node.StorageTypes.INT;
import static io.hotmoka.node.StorageTypes.PAYABLE_CONTRACT;
import static io.hotmoka.node.StorageValues.byteOf;

import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import io.hotmoka.constants.Constants;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.helpers.GasHelpers;
import io.hotmoka.helpers.NonceHelpers;
import io.hotmoka.helpers.SignatureHelpers;
import io.hotmoka.helpers.api.GasHelper;
import io.hotmoka.helpers.api.NonceHelper;
import io.hotmoka.node.Accounts;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.remote.RemoteNodes;

/**
 * Run it in Maven as (change /home/spoto/hotmoka_tutorial with the directory where you stored the key pairs of the payer accounts
 * and change the payer accounts themselves and their passwords):
 * 
 * mvn compile exec:java -Dexec.mainClass="io.hotmoka.tutorial.examples.runs.Auction" -Dexec.args="/home/spoto/hotmoka_tutorial 45e801831593aca23f1038b79377ec890e3bc4f0e62d30c8ddce8c1cf4bf0763#0 banana 7fd3e62a493e91757d3ea2397bfbf0acf6253108d39cca30088e8266db85a15f#0 mango dc34cdaa1d8ed98306f9d58971e233ea0403ed5aea86bc5c827aea605ba3b5d6#0 strawberry"
 */
public class Auction {

  public final static int NUM_BIDS = 10; // number of bids placed
  public final static int BIDDING_TIME = 100_000; // in milliseconds
  public final static int REVEAL_TIME = 140_000; // in milliseconds

  private final static BigInteger _500_000 = BigInteger.valueOf(500_000);

  private final static ClassType BLIND_AUCTION
    = StorageTypes.classNamed("io.hotmoka.tutorial.examples.auction.BlindAuction");
  private final static ConstructorSignature CONSTRUCTOR_BYTES32_SNAPSHOT
    = ConstructorSignatures.of(BYTES32_SNAPSHOT,
      BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE,
      BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE,
      BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE,
      BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE);

  private final TransactionReference takamakaCode;
  private final StorageReference[] accounts;
  private final List<Signer<SignedTransactionRequest<?>>> signers = new ArrayList<>();
  private final String chainId;
  private final long start;  // the time when bids started being placed
  private final Node node;
  private final TransactionReference classpath;
  private final StorageReference auction;
  private final List<BidToReveal> bids = new ArrayList<>();
  private final GasHelper gasHelper;
  private final NonceHelper nonceHelper;

  public static void main(String[] args) throws Exception {
	try (Node node = RemoteNodes.of(new URI(args[0]), 20000)) {
      new Auction(node, Paths.get(args[1]),
        StorageValues.reference(args[2]), args[3],
        StorageValues.reference(args[4]), args[5],
        StorageValues.reference(args[6]), args[7]);
    }
  }

  /**
   * Class used to keep in memory the bids placed by each player,
   * that will be revealed at the end.
   */
  private class BidToReveal {
    private final int player;
    private final BigInteger value;
    private final boolean fake;
    private final byte[] salt;

    private BidToReveal(int player, BigInteger value, boolean fake, byte[] salt) {
      this.player = player;
      this.value = value;
      this.fake = fake;
      this.salt = salt;
    }

    /**
     * Creates in store a revealed bid corresponding to this object.
     * 
     * @return the storage reference to the freshly created revealed bid
     */
    private StorageReference intoBlockchain() throws Exception {
      StorageReference bytes32 = node.addConstructorCallTransaction(TransactionRequests.constructorCall
        (signers.get(player), accounts[player],
        nonceHelper.getNonceOf(accounts[player]), chainId, _500_000,
        panarea(gasHelper.getSafeGasPrice()), classpath, CONSTRUCTOR_BYTES32_SNAPSHOT,
        byteOf(salt[0]), byteOf(salt[1]), byteOf(salt[2]), byteOf(salt[3]),
        byteOf(salt[4]), byteOf(salt[5]), byteOf(salt[6]), byteOf(salt[7]),
        byteOf(salt[8]), byteOf(salt[9]), byteOf(salt[10]), byteOf(salt[11]),
        byteOf(salt[12]), byteOf(salt[13]), byteOf(salt[14]), byteOf(salt[15]),
        byteOf(salt[16]), byteOf(salt[17]), byteOf(salt[18]), byteOf(salt[19]),
        byteOf(salt[20]), byteOf(salt[21]), byteOf(salt[22]), byteOf(salt[23]),
        byteOf(salt[24]), byteOf(salt[25]), byteOf(salt[26]), byteOf(salt[27]),
        byteOf(salt[28]), byteOf(salt[29]), byteOf(salt[30]), byteOf(salt[31])));

      var CONSTRUCTOR_REVEALED_BID
        = ConstructorSignatures.of(
           StorageTypes.classNamed("io.hotmoka.tutorial.examples.auction.BlindAuction$RevealedBid"),
           BIG_INTEGER, BOOLEAN, BYTES32_SNAPSHOT);

      return node.addConstructorCallTransaction(TransactionRequests.constructorCall
        (signers.get(player), accounts[player],
        nonceHelper.getNonceOf(accounts[player]), chainId,
        _500_000, panarea(gasHelper.getSafeGasPrice()), classpath, CONSTRUCTOR_REVEALED_BID,
        StorageValues.bigIntegerOf(value), StorageValues.booleanOf(fake), bytes32));
    }
  }

  private Auction(Node node, Path dir, StorageReference account1, String password1, StorageReference account2, String password2, StorageReference account3, String password3) throws Exception {
    this.node = node;
    takamakaCode = node.getTakamakaCode();
    accounts = new StorageReference[] { account1, account2, account3 };
    var signature = node.getConfig().getSignatureForRequests();
    Function<? super SignedTransactionRequest<?>, byte[]> toBytes = SignedTransactionRequest<?>::toByteArrayWithoutSignature;
	signers.add(signature.getSigner(loadKeys(node, dir, account1, password1).getPrivate(), toBytes));
	signers.add(signature.getSigner(loadKeys(node, dir, account2, password2).getPrivate(), toBytes));
	signers.add(signature.getSigner(loadKeys(node, dir, account3, password3).getPrivate(), toBytes));
    gasHelper = GasHelpers.of(node);
    nonceHelper = NonceHelpers.of(node);
    chainId = node.getConfig().getChainId();
    classpath = installJar();
    auction = createContract();
    start = System.currentTimeMillis();

    StorageReference expectedWinner = placeBids();
    waitUntilEndOfBiddingTime();
    revealBids();
    waitUntilEndOfRevealTime();
    StorageValue winner = askForWinner();

    // show that the contract computes the correct winner
    System.out.println("expected winner: " + expectedWinner);
    System.out.println("actual winner: " + winner);
  }

  private StorageReference createContract() throws Exception {
    System.out.println("Creating contract");

    var CONSTRUCTOR_BLIND_AUCTION = ConstructorSignatures.of(BLIND_AUCTION, INT, INT);

    return node.addConstructorCallTransaction
      (TransactionRequests.constructorCall(signers.get(0), accounts[0],
      nonceHelper.getNonceOf(accounts[0]), chainId, _500_000, panarea(gasHelper.getSafeGasPrice()),
      classpath, CONSTRUCTOR_BLIND_AUCTION,
      StorageValues.intOf(BIDDING_TIME), StorageValues.intOf(REVEAL_TIME)));
  }

  private TransactionReference installJar() throws Exception {
    System.out.println("Installing jar");

    //the path of the user jar to install
    var auctionPath = Paths.get(System.getProperty("user.home")
      + "/.m2/repository/io/hotmoka/io-hotmoka-tutorial-examples-auction/"
      + Constants.HOTMOKA_VERSION
      + "/io-hotmoka-tutorial-examples-auction-" + Constants.HOTMOKA_VERSION + ".jar");

    return node.addJarStoreTransaction(TransactionRequests.jarStore
      (signers.get(0), // an object that signs with the payer's private key
      accounts[0], // payer
      nonceHelper.getNonceOf(accounts[0]), // payer's nonce
      chainId, // chain identifier
      BigInteger.valueOf(1_000_000), // gas limit: enough for this very small jar
      gasHelper.getSafeGasPrice(), // gas price: at least the current gas price of the network
      takamakaCode, // class path for the execution of the transaction
      Files.readAllBytes(auctionPath), // bytes of the jar to install
      takamakaCode)); // dependency
  }

  private StorageReference placeBids() throws Exception {
    var maxBid = BigInteger.ZERO;
    StorageReference expectedWinner = null;
    var random = new Random();
    var BID = MethodSignatures.ofVoid(BLIND_AUCTION, "bid", BIG_INTEGER, BYTES32_SNAPSHOT);

    int i = 1;
    while (i <= NUM_BIDS) { // generate NUM_BIDS random bids
      System.out.println("Placing bid " + i + "/" + NUM_BIDS);
      int player = 1 + random.nextInt(accounts.length - 1);
      var deposit = BigInteger.valueOf(random.nextInt(1000));
      var value = BigInteger.valueOf(random.nextInt(1000));
      var fake = random.nextBoolean();
      var salt = new byte[32];
      random.nextBytes(salt); // random 32 bytes of salt for each bid

      // create a Bytes32 hash of the bid in the store of the node
      StorageReference bytes32 = codeAsBytes32(player, value, fake, salt);

      // keep note of the best bid, to verify the result at the end
      if (!fake && deposit.compareTo(value) >= 0)
        if (expectedWinner == null || value.compareTo(maxBid) > 0) {
          maxBid = value;
          expectedWinner = accounts[player];
        }
        else if (value.equals(maxBid))
          // we do not allow ex aequos, since the winner
          // would depend on the fastest player to reveal
          continue;

      // keep the explicit bid in memory, not yet in the node,
      // since it would be visible there
      bids.add(new BidToReveal(player, value, fake, salt));

      // place a hashed bid in the node
      node.addInstanceMethodCallTransaction(TransactionRequests.instanceMethodCall
        (signers.get(player), accounts[player],
        nonceHelper.getNonceOf(accounts[player]), chainId,
        _500_000, panarea(gasHelper.getSafeGasPrice()), classpath, BID,
        auction, StorageValues.bigIntegerOf(deposit), bytes32));

      i++;
    }

    return expectedWinner;
  }

  private void revealBids() throws Exception {
    var REVEAL = MethodSignatures.ofVoid
      (BLIND_AUCTION, "reveal",
       StorageTypes.classNamed("io.hotmoka.tutorial.examples.auction.BlindAuction$RevealedBid"));

    // we create the revealed bids in blockchain; this is safe now, since the bidding time is over
    int counter = 1;
    for (BidToReveal bid: bids) {
      System.out.println("Revealing bid " + counter++ + "/" + bids.size());
      int player = bid.player;
      StorageReference bidInBlockchain = bid.intoBlockchain();
      node.addInstanceMethodCallTransaction(TransactionRequests.instanceMethodCall
        (signers.get(player), accounts[player],
        nonceHelper.getNonceOf(accounts[player]), chainId, _500_000,
        panarea(gasHelper.getSafeGasPrice()),
        classpath, REVEAL, auction, bidInBlockchain));
    }
  }

  private StorageReference askForWinner() throws Exception {
    var AUCTION_END = MethodSignatures.ofNonVoid
      (BLIND_AUCTION, "auctionEnd", PAYABLE_CONTRACT);

    StorageValue winner = node.addInstanceMethodCallTransaction
      (TransactionRequests.instanceMethodCall
      (signers.get(0), accounts[0], nonceHelper.getNonceOf(accounts[0]),
      chainId, _500_000, panarea(gasHelper.getSafeGasPrice()),
      classpath, AUCTION_END, auction)).get();

    // the winner is normally a StorageReference,
    // but it could be a NullValue if all bids were fake
    return winner instanceof StorageReference sr ? sr : null;
  }

  private void waitUntilEndOfBiddingTime() {
    waitUntil(BIDDING_TIME + 5000, "Waiting until the end of the bidding time");
  }

  private void waitUntilEndOfRevealTime() {
    waitUntil(BIDDING_TIME + REVEAL_TIME + 5000, "Waiting until the end of the revealing time");
  }

  /**
   * Waits until a specific time after start.
   */
  private void waitUntil(long duration, String forWhat) {
    long msToWait = start + duration - System.currentTimeMillis();
    System.out.println(forWhat + " (" + msToWait + "ms still missing)");
	try {
      Thread.sleep(start + duration - System.currentTimeMillis());
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Hashes a bid and put it in the store of the node, in hashed form.
   */
  private StorageReference codeAsBytes32(int player, BigInteger value, boolean fake, byte[] salt) throws Exception {
	// the hashing algorithm used to hide the bids
	var digest = MessageDigest.getInstance("SHA-256");
    digest.update(value.toByteArray());
    digest.update(fake ? (byte) 0 : (byte) 1);
    digest.update(salt);
    byte[] hash = digest.digest();
    return createBytes32(player, hash);
  }

  /**
   * Creates a Bytes32Snapshot object in the store of the node.
   */
  private StorageReference createBytes32(int player, byte[] hash) throws Exception {
    return node.addConstructorCallTransaction
      (TransactionRequests.constructorCall(
      signers.get(player),
      accounts[player],
      nonceHelper.getNonceOf(accounts[player]), chainId,
      _500_000, panarea(gasHelper.getSafeGasPrice()),
      classpath, CONSTRUCTOR_BYTES32_SNAPSHOT,
      byteOf(hash[0]), byteOf(hash[1]),
      byteOf(hash[2]), byteOf(hash[3]),
      byteOf(hash[4]), byteOf(hash[5]),
      byteOf(hash[6]), byteOf(hash[7]),
      byteOf(hash[8]), byteOf(hash[9]),
      byteOf(hash[10]), byteOf(hash[11]),
      byteOf(hash[12]), byteOf(hash[13]),
      byteOf(hash[14]), byteOf(hash[15]),
      byteOf(hash[16]), byteOf(hash[17]),
      byteOf(hash[18]), byteOf(hash[19]),
      byteOf(hash[20]), byteOf(hash[21]),
      byteOf(hash[22]), byteOf(hash[23]),
      byteOf(hash[24]), byteOf(hash[25]),
      byteOf(hash[26]), byteOf(hash[27]),
      byteOf(hash[28]), byteOf(hash[29]),
      byteOf(hash[30]), byteOf(hash[31])));
  }

  private static KeyPair loadKeys(Node node, Path dir, StorageReference account, String password) throws Exception {
    return Accounts.of(account, dir).keys(password, SignatureHelpers.of(node).signatureAlgorithmFor(account));
  }
}