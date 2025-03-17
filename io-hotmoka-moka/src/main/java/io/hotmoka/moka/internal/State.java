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
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.signatures.FieldSignature;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.updates.ClassTag;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.updates.UpdateOfField;
import io.hotmoka.node.api.updates.UpdateOfString;
import io.hotmoka.node.remote.RemoteNodes;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "state",
	description = "Print the state of an object",
	showDefaultValues = true)
public class State extends AbstractCommand {

	@Parameters(index = "0", description = "the storage reference of the object")
    private String object;

	@Option(names = { "--uri" }, description = "the URI of the node", defaultValue = "ws://localhost:8001")
    private URI uri;

	@Option(names = { "--api" }, description = "print the public API of the object")
    private boolean api;

	@Override
	protected void execute() throws Exception {
		new Run();
	}

	private class Run {
		private final Node node;
		private final Update[] updates;
		private final ClassTag tag;

		private Run() throws Exception {
			checkStorageReference(object);
			var reference = StorageValues.reference(object);

			try (var node = this.node = RemoteNodes.of(uri, 10_000)) {
				this.updates = node.getState(reference).sorted().toArray(Update[]::new);
				this.tag = getClassTag();

				printHeader();
				printFieldsInClass();
				printFieldsInherited();
				printAPI();
			}
		}

		private void printAPI() throws ClassNotFoundException, TransactionRejectedException, TransactionException, CodeExecutionException, UnknownReferenceException, NodeException, TimeoutException, InterruptedException {
			System.out.println();
			if (api)
				new PrintAPI(node, tag);
		}

		private void printFieldsInherited() {
			Stream.of(updates)
				.filter(update -> update instanceof UpdateOfField)
				.map(update -> (UpdateOfField) update)
				.filter(update -> !update.getField().getDefiningClass().equals(tag.getClazz()))
				.forEachOrdered(this::printUpdate);
		}

		private void printFieldsInClass() {
			Stream.of(updates)
				.filter(update -> update instanceof UpdateOfField)
				.map(update -> (UpdateOfField) update)
				.filter(update -> update.getField().getDefiningClass().equals(tag.getClazz()))
				.forEachOrdered(this::printUpdate);
		}

		private void printHeader() {
			ClassType clazz = tag.getClazz();
			System.out.println(ANSI_RED + "\nThis is the state of object " + object + "@" + uri + "\n");
			System.out.println(ANSI_RESET + "class " + clazz + " (from jar installed at " + tag.getJar() + ")");
		}

		private ClassTag getClassTag() {
			return Stream.of(updates)
					.filter(update -> update instanceof ClassTag)
					.map(update -> (ClassTag) update)
					.findFirst().get();
		}

		private void printUpdate(UpdateOfField update) {
			FieldSignature field = update.getField();
			if (tag.getClazz().equals(field.getDefiningClass()))
				System.out.println(ANSI_RESET + "  " + field.getName() + ":" + field.getType() + " = " + valueToPrint(update));
			else
				System.out.println(ANSI_CYAN + "\u25b2 " + field.getName() + ":" + field.getType() + " = " + valueToPrint(update) + ANSI_GREEN + " (inherited from " + field.getDefiningClass() + ")");
		}

		private String valueToPrint(UpdateOfField update) {
			if (update instanceof UpdateOfString)
				return '\"' + update.getValue().toString() + '\"';
			else
				return update.getValue().toString();
		}
	}
}