package io.takamaka.code.dao.action;

import io.takamaka.code.dao.SimplePoll;
import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.View;

/**
 * Action that set a flag to know if a poll is approved or not by the voters
 */
@Exported
public class IsApprovedPollAction extends SimplePoll.Action{

	boolean approved;
	
	public IsApprovedPollAction() {
		approved = false;
	}
	
	@Override
	protected String getDescription() {
		return "set a flag to know if the poll is approved or not by the voters";
	}

	@Override
	protected void run() {
		approved = true;
	}
	
	@View
	public boolean isPollApproved() {
		return approved;
	}

}
