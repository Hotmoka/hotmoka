package io.takamaka.code.dao.action;

import io.takamaka.code.dao.SimplePoll;
import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.View;

/**
 * Action that set a flag to true if the run method is performed
 */
@Exported
public class CheckRunPerformedAction extends SimplePoll.Action{

	private boolean runPerformed;
	
	@Override
	protected String getDescription() {
		return "The action sets a flag to true if the run method is performed";
	}

	@Override
	protected void run() {
		runPerformed = true;
	}
	
	@View
	public boolean isRunPerformed() {
		return runPerformed;
	}

}
