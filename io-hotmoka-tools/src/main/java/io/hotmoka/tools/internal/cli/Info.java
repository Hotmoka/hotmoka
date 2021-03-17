package io.hotmoka.tools.internal.cli;

import io.hotmoka.nodes.ManifestHelper;
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