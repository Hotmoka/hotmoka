module io.takamaka.code.engine {
	exports io.takamaka.code.engine;
	requires io.takamaka.code.constants;
	requires io.hotmoka.beans;
	requires transitive io.hotmoka.nodes;
	requires io.takamaka.code.instrumentation;
	requires io.takamaka.code.verification;
	requires io.takamaka.code.whitelisting;
}