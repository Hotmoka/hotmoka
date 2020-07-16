module io.hotmoka.tendermint {
	exports io.hotmoka.tendermint;
	exports io.hotmoka.tendermint.runs;
	exports io.hotmoka.tendermint.internal.beans to com.google.gson;
	requires io.hotmoka.tendermint.dependencies;
	requires io.hotmoka.beans;
	requires io.hotmoka.stores;
	requires io.hotmoka.nodes;
	requires io.takamaka.code.engine;
	requires io.takamaka.code.constants;
	requires com.google.gson;
	requires com.google.protobuf;
	requires grpc.stub;

	// the following is needed because Eclipse complains about a missing
	// class recursively included; instead, Maven is happy without that;
	// at run time, this module is actually a jar in the unnamed module,
	// since it contains a split package with grpc-context
	requires static grpc.api;
	requires org.slf4j;
}