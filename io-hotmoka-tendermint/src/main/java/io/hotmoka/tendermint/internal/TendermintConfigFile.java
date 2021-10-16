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

package io.hotmoka.tendermint.internal;

import io.hotmoka.toml.Toml;

import io.hotmoka.tendermint.TendermintBlockchainConfig;

/**
 * Configuration information extracted from Tendermint' config.toml.
 */
class TendermintConfigFile {

	/**
	 * The port of the ABCI application, as specified in Tendermint's configuration file.
	 */
	public final int abciPort;

	/**
	 * The port of the Tendermint process, as specified in Tendermint's configuration file.
	 */
	public final int tendermintPort;

	TendermintConfigFile(TendermintBlockchainConfig config) {
		Toml toml = new Toml().read(config.dir.resolve("blocks").resolve("config").resolve("config.toml").toFile());
		String proxy_app = toml.getString("proxy_app");
		String expectedPrefix = "tcp://127.0.0.1:";
		if (proxy_app == null || !proxy_app.startsWith(expectedPrefix))
			throw new IllegalArgumentException("The Tendermint configuration file must specify a proxy_app property starting with \"" + expectedPrefix + "\"");
	
		try {
			this.abciPort = Integer.parseUnsignedInt(proxy_app.substring(expectedPrefix.length()));
		}
		catch (NumberFormatException e) {
			throw new IllegalArgumentException("The port of the proxy_app property in Tendermint's configuration file cannot be parsed");
		}

		String laddr = toml.getTable("rpc").getString("laddr");
		if (laddr == null || !laddr.startsWith(expectedPrefix))
			throw new IllegalArgumentException("The Tendermint configuration file must specify a laddr property starting with \"" + expectedPrefix + "\"");

		try {
			this.tendermintPort = Integer.parseUnsignedInt(laddr.substring(expectedPrefix.length()));
		}
		catch (NumberFormatException e) {
			throw new IllegalArgumentException("The port of the laddr property in Tendermint's configuration file cannot be parsed");
		}
	}
}