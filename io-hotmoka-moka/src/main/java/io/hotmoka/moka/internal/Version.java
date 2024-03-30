package io.hotmoka.moka.internal;

import io.hotmoka.node.AbstractNode;
import picocli.CommandLine.Command;

@Command(name = "version",
	description = "Print version information.",
	showDefaultValues = true)
public class Version extends AbstractCommand {

	@Override
	protected void execute() {
		System.out.println(AbstractNode.HOTMOKA_VERSION);
	}
}