module io.hotmoka.takamaka {
	exports io.hotmoka.takamaka;
	exports io.hotmoka.takamaka.beans.requests;
	exports io.hotmoka.takamaka.beans.responses;
	requires transitive io.hotmoka.beans;
	requires io.hotmoka.stores;
	requires transitive io.hotmoka.nodes;
	requires io.hotmoka.local;
	requires org.slf4j;
}