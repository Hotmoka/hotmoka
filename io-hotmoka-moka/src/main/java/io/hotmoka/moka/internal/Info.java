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

package io.hotmoka.moka.internal;

import java.net.URI;

import io.hotmoka.helpers.ManifestHelpers;
import io.hotmoka.node.remote.RemoteNodes;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "info",
	description = "Print information about a node",
	showDefaultValues = true)
public class Info extends AbstractCommand {

	@Option(names = { "--uri" }, description = "the URI of the node", defaultValue = "ws://localhost:8001")
    private URI uri;

	@Override
	protected void execute() throws Exception {
		try (var node = RemoteNodes.of(uri, 10_000L)) {
			System.out.println("\nInfo about the node:\n" + ManifestHelpers.of(node));
		}
	}
}