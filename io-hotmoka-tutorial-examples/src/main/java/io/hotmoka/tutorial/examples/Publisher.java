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

import java.math.BigInteger;
import java.nio.file.Paths;
import java.security.KeyPair;

import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.helpers.InitializedNodes;
import io.hotmoka.node.ValidatorsConsensusConfigBuilders;
import io.hotmoka.node.service.NodeServices;
import io.hotmoka.node.tendermint.TendermintNodeConfigBuilders;
import io.hotmoka.node.tendermint.TendermintNodes;
import io.takamaka.code.constants.Constants;

/**
 * Run it in Maven as:
 * 
 * mvn exec:exec -Dexec.executable="java" -Dexec.args="-cp %classpath io.hotmoka.tutorial.examples.Publisher"
 */
public class Publisher {
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
	var consensus = ValidatorsConsensusConfigBuilders.defaults()
      .setPublicKeyOfGamete(keys.getPublic())
      .setInitialSupply(BigInteger.valueOf(100_000_000))
      .build();

	try (var original = TendermintNodes.init(config);
      // remove the next line if you want to publish an uninitialized node
      var initialized = InitializedNodes.of(original, consensus, takamakaCodePath);
      var service = NodeServices.of(original, 8001)) {

      System.out.println("\nPress ENTER to turn off the server and exit this program");
      System.in.read();
    }
  }
}