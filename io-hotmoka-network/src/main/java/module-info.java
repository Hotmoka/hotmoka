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
	exports io.hotmoka.network.internal.http.controllers to spring.beans, spring.web;
	exports io.hotmoka.network.internal.websockets to spring.beans;
	exports io.hotmoka.network.internal.websockets.controllers to spring.beans;
	opens io.hotmoka.network.internal to spring.core;
    opens io.hotmoka.network.internal.services to spring.core; //, com.google.gson;
    opens io.hotmoka.network.internal.http.controllers to spring.core, spring.messaging;
    opens io.hotmoka.network.internal.websockets to spring.core, spring.context;
    requires org.apache.tomcat.embed.websocket;

    // Gson needs superpowers as well
    opens io.hotmoka.network.models.errors to com.google.gson;
    opens io.hotmoka.network.models.requests to com.google.gson;
    opens io.hotmoka.network.models.responses to com.google.gson;
    opens io.hotmoka.network.models.signatures to com.google.gson;
    opens io.hotmoka.network.models.updates to com.google.gson;
    opens io.hotmoka.network.models.values to com.google.gson;

    requires transitive io.hotmoka.nodes;
	requires transitive io.hotmoka.beans;
    requires org.slf4j;
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.beans;
    requires spring.web;
    requires spring.context;
    requires spring.boot.starter.websocket;
    requires transitive spring.websocket;
    requires spring.messaging;
    requires com.google.gson;
    requires java.instrument;

    // these make it possible to compile under Eclipse...
    requires static spring.core;
    requires static org.apache.tomcat.embed.core;
}