/*
Copyright 2023 Fausto Spoto

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

package io.hotmoka.node.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.hotmoka.crypto.Base64ConversionException;
import io.hotmoka.node.ConsensusConfigBuilders;
import io.hotmoka.node.TendermintConsensusConfigBuilders;
import io.hotmoka.testing.AbstractLoggedTests;
import jakarta.websocket.DecodeException;
import jakarta.websocket.EncodeException;

public class ConsensusConfigTests extends AbstractLoggedTests {

	@Test
	@DisplayName("configs are correctly dumped into TOML and reloaded from TOML")
	public void configDumpLoadTOMLWorks(@TempDir Path dir) throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException, Base64ConversionException {
		var path = dir.resolve("config.toml");
		var config1 = ConsensusConfigBuilders.defaults()
			.setChainId("my-chain")
			.setInitialGasPrice(BigInteger.valueOf(1233L))
			.setInitialSupply(BigInteger.valueOf(45678L))
			.setMaxCumulativeSizeOfDependencies(345678L)
			.build();
		Files.writeString(path, config1.toToml(), StandardCharsets.UTF_8);
		var config2 = ConsensusConfigBuilders.load(path).build();
		assertEquals(config1, config2);
	}

	@Test
	@DisplayName("validators configs are correctly dumped into TOML and reloaded from TOML")
	public void validatorsConfigDumpLoadTOMLWorks(@TempDir Path dir) throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException, Base64ConversionException {
		var path = dir.resolve("validators_config.toml");
		var config1 = TendermintConsensusConfigBuilders.defaults()
			.setChainId("my-chain")
			.setBuyerSurcharge(123456)
			.setSlashingForMisbehaving(98768)
			.setInitialGasPrice(BigInteger.valueOf(1233L))
			.setInitialSupply(BigInteger.valueOf(45678L))
			.setMaxCumulativeSizeOfDependencies(345678L)
			.build();
		Files.writeString(path, config1.toToml(), StandardCharsets.UTF_8);
		var config2 = TendermintConsensusConfigBuilders.load(path).build();
		assertEquals(config1, config2);
	}

	@Test
	@DisplayName("configs are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForConfig() throws EncodeException, DecodeException, NoSuchAlgorithmException {
		var expected = ConsensusConfigBuilders.defaults()
			.setChainId("my chain")
			.setInitialSupply(BigInteger.valueOf(100L))
			.build();

		String encoded = new ConsensusConfigBuilders.Encoder().encode(expected);
		var actual = new ConsensusConfigBuilders.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}
}