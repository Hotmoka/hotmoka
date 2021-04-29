module io.hotmoka.tendermint {
	exports io.hotmoka.tendermint;
	exports io.hotmoka.tendermint.views;
	exports io.hotmoka.tendermint.internal.beans to com.google.gson;
	requires io.hotmoka.tendermint.abci;
	requires io.hotmoka.beans;
	requires io.hotmoka.stores;
	requires transitive io.hotmoka.crypto;
	requires transitive io.hotmoka.nodes;
	requires transitive io.hotmoka.views;
	requires io.hotmoka.local;
	requires com.google.gson;
	requires com.google.protobuf;
	requires org.slf4j;
	requires org.bouncycastle.provider;
}