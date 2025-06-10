/*
    Copyright (C) 2021 Fausto Spoto (fausto.spoto@gmail.com)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package io.hotmoka.tutorial.examples;

import static io.hotmoka.helpers.Coin.panarea;
import static io.hotmoka.node.StorageTypes.INT;
import static java.math.BigInteger.ONE;

import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;

import io.hotmoka.crypto.api.Signer;
import io.hotmoka.helpers.GasHelpers;
import io.hotmoka.helpers.SignatureHelpers;
import io.hotmoka.node.Accounts;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.remote.RemoteNodes;
import io.takamaka.code.constants.Constants;

/**
 * Run it in Maven as (change /home/spoto/hotmoka_tutorial with the directory where you stored the key pairs of the payer account
 * and change the payer account itself and its password):
 * 
 * mvn compile exec:java -Dexec.mainClass="io.hotmoka.tutorial.examples.FamilyStorage" -Dexec.args="/home/spoto/hotmoka_tutorial b3a367310a195bb888a5b722e8246f8bad6e0fc8dfefcf44a2b8d760b5b655ef#0 chocolate"
 */
public class FamilyStorage {

  private final static ClassType PERSON = StorageTypes.classNamed("family.Person");

  public static void main(String[] args) throws Exception {

	// the path of the user jar to install
    var familyPath = Paths.get(System.getProperty("user.home")
      + "/.m2/repository/io/hotmoka/io-takamaka-code-examples-family_storage/"
      + Constants.TAKAMAKA_VERSION
      + "/io-takamaka-code-examples-family_storage-" + Constants.TAKAMAKA_VERSION + ".jar");

    var dir = Paths.get(args[1]);
    var payer = StorageValues.reference(args[2]);
    var password = args[3];

    try (var node = RemoteNodes.of(new URI(args[0]), 80000)) {
      // we get a reference to where io-takamaka-code-X.Y.Z.jar has been stored
      TransactionReference takamakaCode = node.getTakamakaCode();

      // we get the signing algorithm to use for requests
      var signature = node.getConfig().getSignatureForRequests();

	  KeyPair keys = loadKeys(node, dir, payer, password);

	  // we create a signer that signs with the private key of our account
	  Signer<SignedTransactionRequest<?>> signer = signature.getSigner
	    (keys.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature);

	  // we get the nonce of our account: we use the account itself as caller and
	  // an arbitrary nonce (ZERO in the code) since we are running
	  // a @View method of the account
	  BigInteger nonce = node
	    .runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
	      (payer, // payer
	      BigInteger.valueOf(50_000), // gas limit
	      takamakaCode, // class path for the execution of the transaction
	      MethodSignatures.NONCE, // method
	      payer)).get() // receiver of the method call
	    .asBigInteger(__ -> new ClassCastException());

	  // we get the chain identifier of the network
	  String chainId = node.getConfig().getChainId();

	  var gasHelper = GasHelpers.of(node);

      // we install family-0.0.1-SNAPSHOT.jar in the node: our account will pay
      TransactionReference family = node
        .addJarStoreTransaction(TransactionRequests.jarStore
          (signer, // an object that signs with the payer's private key
          payer, // payer
          nonce, // payer's nonce: relevant since this is not a call to a @View method!
          chainId, // chain identifier: relevant since this is not a call to a @View method!
          BigInteger.valueOf(300_000), // gas limit: enough for this very small jar
          gasHelper.getSafeGasPrice(), // gas price: at least the current gas price of the network
          takamakaCode, // class path for the execution of the transaction
          Files.readAllBytes(familyPath), // bytes of the jar to install
          takamakaCode)); // dependencies of the jar that is being installed

      // we increase our copy of the nonce, ready for further
      // transactions having the account as payer
      nonce = nonce.add(ONE);

      // call the constructor of Person and store in einstein the new object in blockchain
      StorageReference einstein = node.addConstructorCallTransaction
        (TransactionRequests.constructorCall
         (signer, // an object that signs with the payer's private key
         payer, // payer
         nonce, // payer's nonce: relevant since this is not a call to a @View method!
         chainId, // chain identifier: relevant since this is not a call to a @View method!
         BigInteger.valueOf(50_000), // gas limit: enough for a small object
         panarea(gasHelper.getSafeGasPrice()), // gas price, in panareas
         family, // class path for the execution of the transaction

         // constructor Person(String,int,int,int)
         ConstructorSignatures.of(PERSON, StorageTypes.STRING, INT, INT, INT),

         // actual arguments
         StorageValues.stringOf("Einstein"), StorageValues.intOf(14),
         StorageValues.intOf(4), StorageValues.intOf(1879)
      ));

      System.out.println("new object allocated at " + einstein);

      // we increase our copy of the nonce, ready for further
      // transactions having the account as payer
      nonce = nonce.add(ONE);
    }
  }

  private static KeyPair loadKeys(Node node, Path dir, StorageReference account, String password)
		  throws Exception {

    return Accounts.of(account, dir)
    		.keys(password, SignatureHelpers.of(node).signatureAlgorithmFor(account));
  }
}