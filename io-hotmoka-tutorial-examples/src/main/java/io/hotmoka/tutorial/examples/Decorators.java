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
import io.hotmoka.helpers.AccountsNodes;
import io.hotmoka.helpers.InitializedNodes;
import io.hotmoka.helpers.JarsNodes;
import io.hotmoka.node.ConsensusConfigBuilders;
import io.hotmoka.node.disk.DiskNodeConfigBuilders;
import io.hotmoka.node.disk.DiskNodes;
import io.takamaka.code.constants.Constants;

/**
 * Run in the IDE or go inside this project and run
 * 
 * mvn clean package
 * java --module-path ../../hotmoka/io-hotmoka-moka/modules/explicit/:../../hotmoka/io-hotmoka-moka/modules/automatic:target/runs-0.0.1.jar -classpath ../../hotmoka/io-hotmoka-moka/modules/unnamed"/*" --add-modules org.glassfish.tyrus.container.grizzly.server,org.glassfish.tyrus.container.grizzly.client --module runs/runs.Decorators
 */
public class Decorators {
  public final static BigInteger SUPPLY = BigInteger.valueOf(1_000_000_000);

  public static void main(String[] args) throws Exception {
    var config = DiskNodeConfigBuilders.defaults().build();

    // the path of the runtime Takamaka jar, inside Maven's cache
    var takamakaCodePath = Paths.get
      (System.getProperty("user.home") +
      "/.m2/repository/io/hotmoka/io-takamaka-code/" + Constants.TAKAMAKA_VERSION + "/io-takamaka-code-" + Constants.TAKAMAKA_VERSION + ".jar");

    // the path of the user jar to install
    var familyPath = Paths.get("../family/target/family-0.0.1.jar");

    // create a key pair for the gamete
    var signature = SignatureAlgorithms.ed25519();
	var entropy = Entropies.random();
	KeyPair keys = entropy.keys("password", signature);
	var consensus = ConsensusConfigBuilders.defaults()
   		.setInitialSupply(SUPPLY)
   		.setPublicKeyOfGamete(keys.getPublic()).build();

	try (var node = DiskNodes.init(config)) {
      // first view: store the io-takamaka-code jar and create manifest and gamete
	  var initialized = InitializedNodes.of(node, consensus, takamakaCodePath);

      // second view: store family-0.0.1.jar: the gamete will pay for that
      var nodeWithJars = JarsNodes.of(node, initialized.gamete(), keys.getPrivate(), familyPath);

	  // third view: create two accounts, the first with 10,000,000 units of coin
      // and the second with 20,000,000 units of coin; the gamete will pay
      var nodeWithAccounts = AccountsNodes.of
        (node, initialized.gamete(), keys.getPrivate(),
        BigInteger.valueOf(10_000_000), BigInteger.valueOf(20_000_000));

      System.out.println("manifest: " + node.getManifest());
      System.out.println("family-0.0.1.jar: " + nodeWithJars.jar(0));
      System.out.println("account #0: " + nodeWithAccounts.account(0) +
                         "\n  with private key " + nodeWithAccounts.privateKey(0));
      System.out.println("account #1: " + nodeWithAccounts.account(1) +
                         "\n  with private key " + nodeWithAccounts.privateKey(1));
    }
  }
}