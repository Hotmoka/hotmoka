module io.hotmoka.local {
	exports io.hotmoka.local;
	
	// classes inside this package will be used only at run time,
	// by instrumented Takamaka code: we do not want to export it:
	// we make them visible at compile time only instead
	opens io.hotmoka.local.internal.runtime;

	requires transitive io.hotmoka.nodes;
	requires io.hotmoka.beans;
	requires io.hotmoka.crypto;
	requires io.takamaka.code.instrumentation;
	requires io.takamaka.code.verification;
	requires io.takamaka.code.constants;
	requires io.takamaka.code.whitelisting;
	requires org.slf4j;
}