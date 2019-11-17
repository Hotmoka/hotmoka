package io.takamaka.code.instrumentation.internal.instrumentationsOfClass;

import io.takamaka.code.instrumentation.internal.ClassInstrumentation;
import io.takamaka.code.verification.Constants;

/**
 * An instrumentation that sets the superclass of some classes, in order to inherit special abilities
 * that must be visible only in the instrumented code.
 */
public class SwapSuperclassOfSpecialClasses {

	public SwapSuperclassOfSpecialClasses(ClassInstrumentation.Instrumenter instrumenter) {
		if (instrumenter.className.equals(Constants.EVENT_NAME))
			instrumenter.clazz.setSuperclassName(Constants.ABSTRACT_EVENT_NAME);
		else if (instrumenter.className.equals(Constants.STORAGE_NAME))
			instrumenter.clazz.setSuperclassName(Constants.ABSTRACT_STORAGE_NAME);
	}
}
