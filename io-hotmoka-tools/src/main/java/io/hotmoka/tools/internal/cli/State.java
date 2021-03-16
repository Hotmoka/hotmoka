package io.hotmoka.tools.internal.cli;

import java.util.stream.Stream;

import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.updates.UpdateOfField;
import io.hotmoka.beans.updates.UpdateOfString;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.Node;
import io.hotmoka.remote.RemoteNode;
import io.hotmoka.remote.RemoteNodeConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "state",
	description = "Prints the state of an object",
	showDefaultValues = true)
public class State extends AbstractCommand {

	@Parameters(arity = "1", description = "the reference to the object")
    private String object;

	@Option(names = { "--url" }, description = "the url of the node (without the protocol)", defaultValue = "localhost:8080")
    private String url;

	@Option(names = { "--api" }, description = "prints the public API of the object")
    private boolean api;

	@Override
	public void run() {
		try {
			new Run();
		}
		catch (Exception e) {
			throw new CommandException(e);
		}
	}

	private class Run {
		private final Node node;
		private final Update[] updates;
		private final ClassTag tag;

		private Run() throws Exception {
			StorageReference reference = new StorageReference(object);
			RemoteNodeConfig remoteNodeConfig = new RemoteNodeConfig.Builder().setURL(url).build();

			try (Node node = this.node = RemoteNode.of(remoteNodeConfig)) {
				this.updates = node.getState(reference).sorted().toArray(Update[]::new);
				this.tag = getClassTag();

				printHeader();
				printFieldsInClass();
				printFieldsInherited();
				printAPI();
			}
		}

		private void printAPI() throws ClassNotFoundException {
			System.out.println();
			if (api)
				new PrintAPI(node, tag.jar, tag.clazz.name);
		}

		private void printFieldsInherited() {
			Stream.of(updates)
				.filter(update -> update instanceof UpdateOfField)
				.map(update -> (UpdateOfField) update)
				.filter(update -> !update.field.definingClass.equals(tag.clazz))
				.forEachOrdered(this::printUpdate);
		}

		private void printFieldsInClass() {
			Stream.of(updates)
				.filter(update -> update instanceof UpdateOfField)
				.map(update -> (UpdateOfField) update)
				.filter(update -> update.field.definingClass.equals(tag.clazz))
				.forEachOrdered(this::printUpdate);
		}

		private void printHeader() {
			ClassType clazz = tag.clazz;
			System.out.println(ANSI_RED + "\nThis is the state of object " + object + "@" + url + "\n");
			System.out.println(ANSI_RESET + "class " + clazz + " (from jar installed at " + tag.jar + ")");
		}

		private ClassTag getClassTag() {
			ClassTag tag = Stream.of(updates)
					.filter(update -> update instanceof ClassTag)
					.map(update -> (ClassTag) update)
					.findFirst().get();
			return tag;
		}

		private void printUpdate(UpdateOfField update) {
			if (tag.clazz.equals(update.field.definingClass))
				System.out.println(ANSI_RESET + "  " + update.field.name + ":" + update.field.type + " = " + valueToPrint(update));
			else
				System.out.println(ANSI_CYAN + "\u25b2 " + update.field.name + ":" + update.field.type + " = " + valueToPrint(update) + ANSI_GREEN + " (inherited from " + update.field.definingClass + ")");
		}

		private String valueToPrint(UpdateOfField update) {
			if (update instanceof UpdateOfString)
				return '\"' + update.getValue().toString() + '\"';
			else
				return update.getValue().toString();
		}
	}
}