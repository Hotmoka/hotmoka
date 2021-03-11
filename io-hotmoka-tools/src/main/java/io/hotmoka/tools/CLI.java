package io.hotmoka.tools;

import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import io.hotmoka.tools.internal.cli.Command;
import io.hotmoka.tools.internal.cli.Command.UncheckedException;
import io.hotmoka.tools.internal.cli.Init;

/**
 * A command-line interface for some basic commands over a Hotmoka node.
 * 
 * This class is meant to be run from the parent directory, after building the project, with this command-line:
 * 
 * java --module-path modules/explicit:modules/automatic --class-path "modules/unnamed/*" --module io.hotmoka.tools/io.hotmoka.tools.CLI
 */
public class CLI {

	private final Command[] commands = { new Init() };

	public static void main(String[] args) throws Exception {
		new CLI(args);
	}

	private CLI(String[] args) throws Exception {
		Options options = createOptions();

		try {
			CommandLine line = new DefaultParser().parse(options, args);
			if (Stream.of(commands).noneMatch(command -> command.run(line)))
				printHelp(options);
		}
		catch (UncheckedException e) {
			throw (Exception) e.getCause();
		}
		catch (ParseException e) {
	    	System.err.println("Syntax error: " + e.getMessage());
	    	printHelp(options);
	    }
	}

	private Options createOptions() {
		Options options = new Options();
		Stream.of(commands).forEachOrdered(command -> command.populate(options));

		return options;
	}

	private void printHelp(Options options) {
		new HelpFormatter().printHelp("java " + getClass().getName(), options);
	}
}