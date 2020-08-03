module io.takamaka.code.engine {
	exports io.takamaka.code.engine;
	
	// classes inside this package will be used only at run time,
	// by instrumented Takamaka code: we do not want to export it
	// and make it visible at compile time
	opens io.takamaka.code.engine.internal.runtime;

	requires io.takamaka.code.constants;
	requires io.hotmoka.beans;
	requires transitive io.hotmoka.crypto;
	requires transitive io.hotmoka.nodes;
	requires io.takamaka.code.instrumentation;
	requires io.takamaka.code.verification;
	requires transitive io.takamaka.code.whitelisting;
	requires org.slf4j;
}