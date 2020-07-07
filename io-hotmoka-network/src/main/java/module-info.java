module io.hotmoka.network {
	exports io.hotmoka.network;
	exports io.hotmoka.network.service.add to spring.beans;
	exports io.hotmoka.network.service.get to spring.beans;
	exports io.hotmoka.network.service.post to spring.beans;
	exports io.hotmoka.network.rest to spring.beans, spring.web;
	opens io.hotmoka.network to spring.core;
    opens io.hotmoka.network.service to spring.core;
    opens io.hotmoka.network.rest to spring.core;
    opens io.hotmoka.network.service.run to spring.beans;
	requires io.hotmoka.nodes;
    requires org.slf4j;
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.beans;
    requires spring.web;
    requires spring.context;
    requires io.hotmoka.tendermint;
    requires io.takamaka.code.constants;
    requires java.instrument;

    // this makes it possible to compile under Eclipse...
    requires static spring.core;
}