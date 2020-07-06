module io.hotmoka.tendermint {
	exports io.hotmoka.tendermint;
	exports io.hotmoka.tendermint.runs;
	requires io.hotmoka.tendermint.dependencies;
	requires io.hotmoka.xodus;
	requires io.hotmoka.nodes;
	requires io.takamaka.code.engine;
	requires io.takamaka.code.constants;
	requires io.hotmoka.patricia;
	requires com.google.gson;
	requires com.google.protobuf;
	requires grpc.stub;
	requires grpc.api;
	requires org.slf4j;
}