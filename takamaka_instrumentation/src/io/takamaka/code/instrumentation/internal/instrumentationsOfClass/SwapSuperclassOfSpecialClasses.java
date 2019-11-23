package io.takamaka.code.instrumentation.internal.instrumentationsOfClass;

import io.takamaka.code.instrumentation.internal.InstrumentedClass;
import io.takamaka.code.verification.Constants;

/**
 * An instrumentation that sets the superclass of some classes, in order to inherit special abilities
 * that must be visible only in the instrumented code.
 */
public class SwapSuperclassOfSpecialClasses extends InstrumentedClass.Builder.ClassLevelInstrumentation {

	public SwapSuperclassOfSpecialClasses(InstrumentedClass.Builder builder) {
		builder.super();

		if (className.equals(Constants.EVENT_NAME))
			instrumentedClass.setSuperclassName(Constants.ABSTRACT_EVENT_NAME);
		else if (className.equals(Constants.STORAGE_NAME))
			instrumentedClass.setSuperclassName(Constants.ABSTRACT_STORAGE_NAME);
	}
}
