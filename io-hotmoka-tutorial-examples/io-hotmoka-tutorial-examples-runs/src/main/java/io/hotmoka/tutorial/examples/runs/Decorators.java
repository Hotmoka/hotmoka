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

import static io.hotmoka.constants.Constants.HOTMOKA_VERSION;
import static io.takamaka.code.constants.Constants.TAKAMAKA_VERSION;

import java.math.BigInteger;
import java.nio.file.Paths;
import java.security.KeyPair;

import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.helpers.AccountsNodes;
import io.hotmoka.helpers.InitializedNodes;
import io.hotmoka.helpers.JarsNodes;
import io.hotmoka.node.ConsensusConfigBuilders;
import io.hotmoka.node.disk.DiskNodeConfigBuilders;
import io.hotmoka.node.disk.DiskNodes;

/**
 * Run it in Maven as:
 * 
 * mvn exec:exec -Dexec.executable="java" -Dexec.args="-cp %classpath io.hotmoka.tutorial.examples.runs.Decorators"
 */
public class Decorators {
 
  public static void main(String[] args) throws Exception {
    var config = DiskNodeConfigBuilders.defaults().build();

    // the path of the runtime Takamaka jar, inside Maven's cache
    var takamakaCodePath = Paths.get
      (System.getProperty("user.home") +
      "/.m2/repository/io/hotmoka/io-takamaka-code/" + TAKAMAKA_VERSION
      + "/io-takamaka-code-" + TAKAMAKA_VERSION + ".jar");

    // the path of the user jar to install
    var familyPath = Paths.get(System.getProperty("user.home")
      + "/.m2/repository/io/hotmoka/io-hotmoka-tutorial-examples-family/"
      + HOTMOKA_VERSION
      + "/io-hotmoka-tutorial-examples-family-" + HOTMOKA_VERSION + ".jar");

    // create a key pair for the gamete
    var signature = SignatureAlgorithms.ed25519();
	var entropy = Entropies.random();
	KeyPair keys = entropy.keys("mypassword", signature);
	var consensus = ConsensusConfigBuilders.defaults()
   	  .setInitialSupply(BigInteger.valueOf(1_000_000_000))
   	  .setPublicKeyOfGamete(keys.getPublic()).build();

	try (var node = DiskNodes.init(config)) {
      // first view: store the io-takamaka-code jar and create manifest and gamete
	  var initialized = InitializedNodes.of(node, consensus, takamakaCodePath);

      // second view: store the family jar: the gamete will pay for that
      var nodeWithJars = JarsNodes.of(node, initialized.gamete(), keys.getPrivate(), familyPath);

	  // third view: create two accounts, the first with 10,000,000 units of coin
      // and the second with 20,000,000 units of coin; the gamete will pay
      var nodeWithAccounts = AccountsNodes.of
        (node, initialized.gamete(), keys.getPrivate(),
        BigInteger.valueOf(10_000_000), BigInteger.valueOf(20_000_000));

      System.out.println("manifest: " + node.getManifest());
      System.out.println("family jar: " + nodeWithJars.jar(0));
      System.out.println("account #0: " + nodeWithAccounts.account(0) +
                         "\n  with private key " + nodeWithAccounts.privateKey(0));
      System.out.println("account #1: " + nodeWithAccounts.account(1) +
                         "\n  with private key " + nodeWithAccounts.privateKey(1));
    }
  }
}