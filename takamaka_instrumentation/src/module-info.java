module io.takamaka.code.instrumentation {
	exports io.takamaka.code.instrumentation;
	exports io.takamaka.code.instrumentation.issues;
	requires transitive io.takamaka.code.whitelisting;
	requires it.univr.bcel;
}