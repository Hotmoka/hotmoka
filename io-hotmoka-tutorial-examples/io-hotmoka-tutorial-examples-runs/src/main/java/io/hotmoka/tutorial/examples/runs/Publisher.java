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

import java.math.BigInteger;
import java.nio.file.Paths;
import java.security.KeyPair;

import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.node.service.NodeServices;
import io.hotmoka.node.tendermint.TendermintConsensusConfigBuilders;
import io.hotmoka.node.tendermint.TendermintInitializedNodes;
import io.hotmoka.node.tendermint.TendermintNodeConfigBuilders;
import io.hotmoka.node.tendermint.TendermintNodes;
import io.takamaka.code.constants.Constants;

/**
 * A publisher of a Tendermint Hotmoka service online.
 * Run it in Maven as:
 * 
 * mvn exec:exec -Dexec.executable="java" -Dexec.args="-cp %classpath io.hotmoka.tutorial.examples.runs.Publisher"
 */
public class Publisher {

  private Publisher() {}

  /**
   * Publishes a Tendermint Hotmoka service online.
   * 
   * @param args unused
   * @throws Exception if the publication fails
   */
  public static void main(String[] args) throws Exception {
    var config = TendermintNodeConfigBuilders.defaults().build();

    // the path of the runtime Takamaka jar, inside Maven's cache
    var takamakaCodePath = Paths.get
      (System.getProperty("user.home") +
      "/.m2/repository/io/hotmoka/io-takamaka-code/" + Constants.TAKAMAKA_VERSION +
      "/io-takamaka-code-" + Constants.TAKAMAKA_VERSION + ".jar");

    // create a key pair for the gamete
    var signature = SignatureAlgorithms.ed25519();
    var entropy = Entropies.random();
	KeyPair keys = entropy.keys("password", signature);
	var consensus = TendermintConsensusConfigBuilders.defaults()
      .setPublicKeyOfGamete(keys.getPublic())
      .setInitialSupply(BigInteger.valueOf(100_000_000))
      .build();

	try (var original = TendermintNodes.init(config);
      // remove the next line if you want to publish an uninitialized node
      var initialized = TendermintInitializedNodes.of(original, consensus, takamakaCodePath);
      var service = NodeServices.of(original, 8001)) {

      System.out.println("\nPress ENTER to turn off the server and exit this program");
      System.in.read();
    }
  }
}