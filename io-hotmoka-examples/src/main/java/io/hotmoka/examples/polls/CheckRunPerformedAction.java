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

package io.hotmoka.examples.polls;

import io.takamaka.code.dao.SimplePoll;
import io.takamaka.code.lang.View;

/**
 * Action that set a flag to true if the run method is performed
 */
public class CheckRunPerformedAction extends SimplePoll.Action {

	private boolean runPerformed;
	
	@Override
	public String getDescription() {
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