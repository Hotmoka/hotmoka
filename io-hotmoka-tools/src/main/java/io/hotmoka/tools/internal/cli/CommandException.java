package io.hotmoka.tools.internal.cli;

/**
 * An exception thrown during the execution of a CLI command.
 */
public class CommandException extends RuntimeException {

	private static final long serialVersionUID = 3026861370427646020L;

	CommandException(Exception wrapped) {
		super(wrapped);
	}
}