module io.takamaka.code.instrumentation {
	exports io.takamaka.code.instrumentation;
	requires transitive io.takamaka.code.verification;
	requires io.takamaka.code.whitelisting;
	requires io.takamaka.code.constants;
	requires it.univr.bcel;
}