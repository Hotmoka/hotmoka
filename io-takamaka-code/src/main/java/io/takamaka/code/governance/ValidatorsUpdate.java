package io.takamaka.code.governance;

import io.takamaka.code.lang.Event;
import io.takamaka.code.lang.FromContract;

/**
 * An event triggered when number, identity or properties of the validators have changed.
 */
public final class ValidatorsUpdate extends Event {
	@FromContract ValidatorsUpdate() {}
}