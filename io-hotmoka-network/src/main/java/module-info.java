module io.hotmoka.network {
	exports io.hotmoka.network;
	exports io.hotmoka.network.internal to spring.beans, spring.context;
	exports io.hotmoka.network.internal.services to spring.beans;
	exports io.hotmoka.network.internal.rest to spring.beans, spring.web;
	exports io.hotmoka.network.internal.models.transactions to spring.core;
	exports io.hotmoka.network.internal.models.updates to spring.core;
	exports io.hotmoka.network.internal.models.storage to spring.core;
	opens io.hotmoka.network.internal to spring.core;
    opens io.hotmoka.network.internal.services to spring.core;
    opens io.hotmoka.network.internal.rest to spring.core;
	requires transitive io.hotmoka.nodes;
    requires org.slf4j;
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.beans;
    requires spring.web;
    requires spring.context;
    requires com.google.gson;
    requires io.hotmoka.tendermint; // TODO: remove later
    requires io.takamaka.code.constants; // TODO: remove later
    requires java.instrument;

    // this makes it possible to compile under Eclipse...
    requires static spring.core;
	requires io.hotmoka.beans;
}