package io.hotmoka.tools.internal.cli;

import picocli.CommandLine;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.ParseResult;

public class PrintExceptionMessageHandler implements IExecutionExceptionHandler {

	@Override
	public int handleExecutionException(Exception ex, CommandLine cmd, ParseResult parseResult) throws Exception {
		if (ex instanceof CommandException && ex.getCause() != null) {
			ex = ((CommandException) ex).getCause();

			// bold red error message
			cmd.getErr().println(cmd.getColorScheme().errorText(ex.getMessage()));

			return cmd.getExitCodeExceptionMapper() != null
					? cmd.getExitCodeExceptionMapper().getExitCode(ex)
							: cmd.getCommandSpec().exitCodeOnExecutionException();
		}
		else
			throw ex;
    }
}