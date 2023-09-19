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

package io.hotmoka.node.disk.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.hotmoka.node.disk.DiskNodeConfigBuilders;
import io.hotmoka.node.disk.api.DiskNodeConfig;
import io.hotmoka.testing.AbstractLoggedTests;

public class ConfigTests extends AbstractLoggedTests {

	@Test
	@DisplayName("configs are correctly dumped into TOML and reloaded from TOML")
	public void dumpLoadTOMLWorks(@TempDir Path dir) throws IOException {
		var path = dir.resolve("config.toml");
		var config1 = createTestConfig();
		Files.writeString(path, config1.toToml(), StandardCharsets.UTF_8);
		var config2 = DiskNodeConfigBuilders.load(path).build();
		assertEquals(config1, config2);
	}

	@Test
	@DisplayName("configs are correctly transformed into builder and back")
	public void toBuilderAndBackWorks(@TempDir Path dir) throws IOException {
		var config1 = createTestConfig();
		var config2 = config1.toBuilder().build();
		assertEquals(config1, config2);
	}

	private DiskNodeConfig createTestConfig() {
		return DiskNodeConfigBuilders.defaults()
			.setDir(Paths.get("mychain"))
			.setMaxGasPerViewTransaction(BigInteger.valueOf(10000L))
			.setMaxPollingAttempts(13)
			.setPollingDelay(100)
			.setRequestCacheSize(17)
			.setResponseCacheSize(19)
			.setTransactionsPerBlock(11)
			.build();
	}
}