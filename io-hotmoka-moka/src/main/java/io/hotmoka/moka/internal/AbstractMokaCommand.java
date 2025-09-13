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

package io.hotmoka.moka.internal;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

import io.hotmoka.cli.AbstractCommand;
import io.hotmoka.cli.CommandException;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.websockets.beans.MappedEncoder;
import io.hotmoka.websockets.beans.api.JsonRepresentation;
import jakarta.websocket.EncodeException;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;

/**
 * Shared code among the commands that do not need to connect to a remote Hotmoka node.
 */
public abstract class AbstractMokaCommand extends AbstractCommand {

	@Option(names = "--redirection", paramLabel = "<path>", description = "the path where the output must be redirected, if any; if missing, the output is printed to the standard output")
	private Path redirection;

	@Option(names = "--json", description = "print the output in JSON", defaultValue = "false")
	private boolean json;

	protected final static BigInteger _100_000 = BigInteger.valueOf(100_000L);

	/**
	 * Determines if the output is required in JSON format.
	 * 
	 * @return true if and only if that condition holds
	 */
	protected final boolean json() {
		return json;
	}

	/**
	 * Reports on the given output of a command.
	 * 
	 * @param <O> the type of the output to report
	 * @param output the output to report
	 * @param encoder a supplier of a converter of the output into JSON representation; this will
	 *                be used only if {@code json} is true
	 * @throws CommandException if reporting failed
	 */
	protected <O> void report(O output, Supplier<MappedEncoder<O, ? extends JsonRepresentation<O>>> encoder) throws CommandException {
		String textualOutput;

		if (json) {
			try {
				textualOutput = encoder.get().encode(output) + "\n";
			}
			catch (EncodeException e) {
				throw new CommandException("Cannot encode the output of the command in JSON format", e);
			}
		}
		else
			textualOutput = output.toString();

		if (redirection == null)
			System.out.print(textualOutput);
		else {
			try {
				Files.writeString(redirection, textualOutput);
			}
			catch (IOException e) {
				e.printStackTrace();
				throw new CommandException("Cannot write the JSON output as \"" + redirection + "\"");
			}
		}
	}

	/**
	 * Styles the given string as for the command style.
	 * 
	 * @param text the text to style
	 * @return the styled text
	 */
	protected static String asCommand(String text) {
		return Ansi.AUTO.string("@|bold " + text + "|@");
	}

	/**
	 * Styles the given URI in URI style.
	 * 
	 * @param uri the URI to style
	 * @return the styled URI text
	 */
	protected static String asUri(URI uri) {
		return Ansi.AUTO.string("@|blue " + uri + "|@");
	}

	/**
	 * Styles the given path in path style.
	 * 
	 * @param path the path
	 * @return the styled path
	 */
	protected static String asPath(Path path) {
		return Ansi.AUTO.string("@|bold,fg(1;3;1) \"" + path + "\"|@");
	}

	/**
	 * Styles the given transaction reference in transaction reference style.
	 * 
	 * @param reference the reference
	 * @return the styled text
	 */
	protected static String asTransactionReference(TransactionReference reference) {
		return Ansi.AUTO.string("@|fg(5;3;2) " + reference + "|@");
	}

	/**
	 * Styles the given string as for the user interaction style.
	 * 
	 * @param text the text to style
	 * @return the styled text
	 */
	protected static String asInteraction(String text) {
		return Ansi.AUTO.string("@|bold,red " + text + "|@");
	}
}