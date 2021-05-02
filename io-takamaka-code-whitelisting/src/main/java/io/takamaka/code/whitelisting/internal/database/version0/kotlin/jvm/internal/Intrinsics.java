package io.takamaka.code.whitelisting.internal.database.version0.kotlin.jvm.internal;

import java.time.LocalDate;
import java.time.Month;

public interface Intrinsics {
	// Decommentare per mettere in white-list, quindi ricompilare questo sotto-progetto: mvn clean install
    void checkNotNullParameter(java.lang.Object parameter, java.lang.String name);
	void checkNotNullExpressionValue(java.lang.Object value, java.lang.String expression);
	void checkNotNull(java.lang.Object object);
	boolean areEqual(java.lang.Object first, java.lang.Object second);
}