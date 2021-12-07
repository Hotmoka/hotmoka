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

package io.hotmoka.tools.internal.moka;

import io.hotmoka.helpers.ManifestHelper;
import io.hotmoka.nodes.Node;
import io.hotmoka.remote.RemoteNode;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "info",
	description = "Prints information about a node",
	showDefaultValues = true)
public class Info extends AbstractCommand {

	@Option(names = { "--url" }, description = "the url of the node (without the protocol)", defaultValue = "localhost:8080")
    private String url;

	@Override
	protected void execute() throws Exception {
		try (Node node = RemoteNode.of(remoteNodeConfig(url))) {
			System.out.println("\nInfo about the node:\n" + new ManifestHelper(node));
		}
	}
}