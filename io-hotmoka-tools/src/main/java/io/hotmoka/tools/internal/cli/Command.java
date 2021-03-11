package io.hotmoka.tools.internal.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 * A command of the command-line interface.
 */
public interface Command {

	/**
	 * Adds options for this command inside the given container.
	 * 
	 * @param options the container of all options
	 */
	void populate(Options options);

	/**
	 * Run the command, using the given command-line.
	 * 
	 * @param line the command-line
	 * @return true if and only if the command has been executed (this typical depends on the command-line)
	 * @throws UncheckedException any exception thrown during the execution of the command,
	 *                            wrapped as an {@link #io.hotmoka.tools.internal.cli.Command.UncheckedException}
	 */
	boolean run(CommandLine line) throws UncheckedException;

	/**
	 * The wrapper of an exception thrown during the execution of a command.
	 */
	class UncheckedException extends RuntimeException {
		private static final long serialVersionUID = 4129595351740741064L;

		public UncheckedException(Exception wrapped) {
			super(wrapped);
		}
	}
}