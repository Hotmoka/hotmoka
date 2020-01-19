module io.takamaka.code.blockchain {
	exports io.takamaka.code.blockchain;
	exports io.takamaka.code.blockchain.responses;
	exports io.takamaka.code.blockchain.runtime;
	requires io.takamaka.code.constants;
	requires transitive io.hotmoka.beans;
	requires io.takamaka.code.instrumentation;
	requires io.takamaka.code.verification;
	requires io.takamaka.code.whitelisting;
}