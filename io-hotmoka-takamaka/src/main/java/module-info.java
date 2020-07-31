module io.hotmoka.takamaka {
	exports io.hotmoka.takamaka;
	exports io.hotmoka.takamaka.beans.requests;
	exports io.hotmoka.takamaka.beans.responses;
	requires transitive io.hotmoka.beans;
	requires io.hotmoka.stores;
	requires io.hotmoka.nodes;
	requires io.takamaka.code.engine;
	requires org.slf4j;
}