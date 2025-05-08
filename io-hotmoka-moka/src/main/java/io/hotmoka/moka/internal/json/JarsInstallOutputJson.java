/*
Copyright 2025 Fausto Spoto

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

package io.hotmoka.moka.internal.json;

import java.util.Optional;

import io.hotmoka.moka.api.jars.JarsInstallOutput;
import io.hotmoka.moka.internal.jars.Install;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of the output of the {@code moka jars install} command.
 */
public class JarsInstallOutputJson extends GasCostOutputJson implements JsonRepresentation<JarsInstallOutput> {
	private final TransactionReferences.Json jar;

	protected JarsInstallOutputJson(JarsInstallOutput output) {
		super(output);

		this.jar = output.getJar().map(TransactionReferences.Json::new).orElse(null);
	}

	public Optional<TransactionReferences.Json> getJar() {
		return Optional.ofNullable(jar);
	}

	@Override
	public JarsInstallOutput unmap() throws InconsistentJsonException {
		return new Install.Output(this);
	}
}