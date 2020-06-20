module io.takamaka.code.instrumentation {
	exports io.takamaka.code.instrumentation;
	requires transitive io.takamaka.code.verification;
	requires transitive io.hotmoka.beans;
	requires io.takamaka.code.whitelisting;
	requires io.takamaka.code.constants;
	requires it.univr.bcel;
	requires org.apache.bcel;
}