package io.hotmoka.tools.internal.cli;

import picocli.CommandLine;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.ParseResult;

public class PrintExceptionMessageHandler implements IExecutionExceptionHandler {

	@Override
	public int handleExecutionException(Exception ex, CommandLine cmd, ParseResult parseResult) throws Exception {
		if (ex instanceof CommandException) {
			if (ex.getCause() != null)
				ex = ((CommandException) ex).getCause();

			if (ex instanceof CommandException)
				cmd.getErr().println(cmd.getColorScheme().errorText(ex.getMessage()));
			else
				cmd.getErr().println(cmd.getColorScheme().errorText(ex.getClass().getName() + ": " + ex.getMessage()));

			return cmd.getExitCodeExceptionMapper() != null
					? cmd.getExitCodeExceptionMapper().getExitCode(ex)
							: cmd.getCommandSpec().exitCodeOnExecutionException();
		}
		else
			throw ex;
    }
}