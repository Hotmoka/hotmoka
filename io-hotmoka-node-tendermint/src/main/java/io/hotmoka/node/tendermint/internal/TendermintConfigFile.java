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

package io.hotmoka.node.tendermint.internal;

import com.moandjiezana.toml.Toml;

import io.hotmoka.node.local.api.LocalNodeConfig;

/**
 * Configuration information extracted from Tendermint's config.toml.
 */
public class TendermintConfigFile {

	/**
	 * The port of the ABCI application, as specified in the Tendermint's configuration file.
	 */
	private final int abciPort;

	/**
	 * The port of the Tendermint process, as specified in the Tendermint's configuration file.
	 */
	private final int tendermintPort;

	/**
	 * The time out commit (in milliseconds), as specified in the Tendermint's configuration file.
	 */
	private final long timeoutCommit;

	/**
	 * Yields the port of the ABCI application, as specified in the Tendermint's configuration file.
	 * 
	 * @return the port of the ABCI application
	 */
	public int getAbciPort() {
		return abciPort;
	}

	/**
	 * Yields the port of the Tendermint process, as specified in the Tendermint's configuration file.
	 * 
	 * @return the port of the Tendermint process
	 */
	public int getTendermintPort() {
		return tendermintPort;
	}

	/**
	 * Yields the commit timeout, as specified in the Tendermint's configuration file.
	 * 
	 * @return the commit timeout, in milliseconds
	 */
	public long getTimeoutCommit() {
		return timeoutCommit;
	}

	/**
	 * Creates the configuration information extracted from Tendermint's {@code config.toml}.
	 * 
	 * @param config the local configuration of the Hotmoka node
	 */
	TendermintConfigFile(LocalNodeConfig<?,?> config) {
		Toml toml = new Toml().read(config.getDir().resolve("tendermint").resolve("config").resolve("config.toml").toFile());
		String proxy_app = toml.getString("proxy_app");
		String expectedPrefix = "tcp://127.0.0.1:";
		if (proxy_app == null || !proxy_app.startsWith(expectedPrefix))
			throw new TendermintException("The Tendermint configuration file must specify a proxy_app property starting with \"" + expectedPrefix + "\"");
	
		try {
			this.abciPort = Integer.parseUnsignedInt(proxy_app.substring(expectedPrefix.length()));
		}
		catch (NumberFormatException e) {
			throw new TendermintException("The port of the proxy_app property in the Tendermint's configuration file cannot be parsed", e);
		}

		String laddr = toml.getTable("rpc").getString("laddr");
		if (laddr == null || !laddr.startsWith(expectedPrefix))
			throw new TendermintException("The Tendermint configuration file must specify a laddr property starting with \"" + expectedPrefix + "\"");

		try {
			this.tendermintPort = Integer.parseUnsignedInt(laddr.substring(expectedPrefix.length()));
		}
		catch (NumberFormatException e) {
			throw new TendermintException("The port of the laddr property in the Tendermint's configuration file cannot be parsed", e);
		}

		String timeoutCommit = toml.getTable("consensus").getString("timeout_commit");
		if (timeoutCommit == null)
			throw new TendermintException("The Tendermint configuration file must specify a commit timeout with timeout_commit");

		long timeoutCommitMs;
		if (timeoutCommit.endsWith("ms")) {
			try {
				timeoutCommitMs = Long.parseUnsignedLong(timeoutCommit.substring(0, timeoutCommit.length() - 2));
			}
			catch (NumberFormatException e) {
				throw new TendermintException("The commit timeout in the Tendermint configuration file is not parsable");
			}
		}
		else if (timeoutCommit.endsWith("s")) {
			try {
				timeoutCommitMs = Long.parseLong(timeoutCommit.substring(0, timeoutCommit.length() - 1)) * 1000L;
			}
			catch (NumberFormatException e) {
				throw new TendermintException("The commit timeout in the Tendermint configuration file is not parsable");
			}
		}
		else throw new TendermintException("The Tendermint configuration file must specify a commit timeout that ends with \"ms\" or with \"s\"");

		this.timeoutCommit = timeoutCommitMs;
	}
}