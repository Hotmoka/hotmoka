package io.hotmoka.tools.internal.cli;

import picocli.CommandLine;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.ParseResult;

public class PrintExceptionMessageHandler implements IExecutionExceptionHandler {

	@Override
	public int handleExecutionException(Exception ex, CommandLine cmd, ParseResult parseResult) throws Exception {
		if (ex instanceof CommandException) {
			Throwable tex = ex;
			if (tex.getCause() != null)
				tex = ((CommandException) tex).getCause();

			if (tex instanceof CommandException)
				cmd.getErr().println(cmd.getColorScheme().errorText(tex.getMessage()));
			else
				cmd.getErr().println(cmd.getColorScheme().errorText(tex.getClass().getName() + ": " + tex.getMessage()));

			return cmd.getExitCodeExceptionMapper() != null
					? cmd.getExitCodeExceptionMapper().getExitCode(tex)
							: cmd.getCommandSpec().exitCodeOnExecutionException();
		}
		else
			throw ex;
    }
}