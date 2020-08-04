module io.hotmoka.network {
	exports io.hotmoka.network;
	exports io.hotmoka.network.models.requests;
	exports io.hotmoka.network.models.values;
	exports io.hotmoka.network.models.updates;
	exports io.hotmoka.network.models.signatures;
	exports io.hotmoka.network.models.responses;
	exports io.hotmoka.network.models.errors;

	// Spring needs superpowers
	exports io.hotmoka.network.internal to spring.beans, spring.context;
	exports io.hotmoka.network.internal.services to spring.beans, spring.web;
	exports io.hotmoka.network.internal.rest to spring.beans, spring.web;
	opens io.hotmoka.network.internal to spring.core;
    opens io.hotmoka.network.internal.services to spring.core; //, com.google.gson;
    opens io.hotmoka.network.internal.rest to spring.core;

	requires transitive io.hotmoka.nodes;
	requires transitive io.hotmoka.beans;
    requires org.slf4j;
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.beans;
    requires spring.web;
    requires spring.context;
    requires com.google.gson;
    requires java.instrument;

    // this makes it possible to compile under Eclipse...
    requires static spring.core;
}