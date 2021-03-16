package io.hotmoka.tools.internal.cli;

import io.hotmoka.nodes.ManifestHelper;
import io.hotmoka.nodes.Node;
import io.hotmoka.remote.RemoteNode;
import io.hotmoka.remote.RemoteNodeConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "info",
	description = "Prints information about a node",
	showDefaultValues = true)
public class Info extends AbstractCommand {

	@Option(names = { "--url" }, description = "the url of the node (without the protocol)", defaultValue = "localhost:8080")
    private String url;

	@Override
	public void run() {
		RemoteNodeConfig remoteNodeConfig = new RemoteNodeConfig.Builder().setURL(url).build();

		try (Node node = RemoteNode.of(remoteNodeConfig)) {
			System.out.println("\nInfo about the node:\n" + new ManifestHelper(node));
		}
		catch (Exception e) {
			throw new CommandException(e);
		}
	}
}