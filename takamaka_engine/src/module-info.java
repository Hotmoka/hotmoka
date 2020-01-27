module io.takamaka.code.engine {
	exports io.hotmoka.nodes;
	exports io.takamaka.code.engine;
	exports io.takamaka.code.engine.runtime;
	requires io.takamaka.code.constants;
	requires transitive io.hotmoka.beans;
	requires io.takamaka.code.instrumentation;
	requires io.takamaka.code.verification;
	requires io.takamaka.code.whitelisting;
}