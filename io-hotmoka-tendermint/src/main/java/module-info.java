module io.hotmoka.tendermint {
	exports io.hotmoka.tendermint;
	exports io.hotmoka.tendermint.views;
	exports io.hotmoka.tendermint.internal.beans to com.google.gson;
	requires io.hotmoka.tendermint.dependencies;
	requires io.hotmoka.beans;
	requires io.hotmoka.stores;
	requires transitive io.hotmoka.crypto;
	requires transitive io.hotmoka.nodes;
	requires io.hotmoka.xodus;
	requires io.hotmoka.local;
	requires io.takamaka.code.constants;
	requires com.google.gson;
	requires com.google.protobuf;
	requires grpc.stub;
	// the following is needed because Eclipse complains about a missing
	// class recursively included; instead, Maven is happy without that, unless JavaDoc is generated;
	// at run time, this module is actually a jar in the unnamed module,
	// since it contains a split package with grpc-context
	requires static grpc.api;
	requires org.slf4j;
	requires org.bouncycastle.provider;
}