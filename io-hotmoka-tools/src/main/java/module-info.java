module io.hotmoka.tools {
	exports io.hotmoka.tools;
	requires io.hotmoka.tendermint;
	requires io.hotmoka.service;
	requires io.hotmoka.remote;
    requires io.takamaka.code.constants;
	requires io.hotmoka.beans;
	requires io.takamaka.code.instrumentation;
	requires io.takamaka.code.verification;
	requires info.picocli;
	opens io.hotmoka.tools.internal.cli to info.picocli; // for injecting CLI options
}