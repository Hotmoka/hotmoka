module io.hotmoka.service {
	exports io.hotmoka.service;
	exports io.hotmoka.service.models.requests;
	exports io.hotmoka.service.models.values;
	exports io.hotmoka.service.models.updates;
	exports io.hotmoka.service.models.signatures;
	exports io.hotmoka.service.models.responses;
	exports io.hotmoka.service.models.errors;

	// Spring needs superpowers
	exports io.hotmoka.service.internal to spring.beans, spring.context;
	exports io.hotmoka.service.internal.services to spring.beans, spring.web;
	exports io.hotmoka.service.internal.http.controllers to spring.beans, spring.web;
	exports io.hotmoka.service.internal.websockets to spring.beans;
	exports io.hotmoka.service.internal.websockets.controllers to spring.beans, spring.messaging;
	exports io.hotmoka.service.internal.websockets.config to spring.beans;
	opens io.hotmoka.service.internal to spring.core;
    opens io.hotmoka.service.internal.services to spring.core; //, com.google.gson;
    opens io.hotmoka.service.internal.http.controllers to spring.core;
    opens io.hotmoka.service.internal.websockets to spring.core, spring.context;
    opens io.hotmoka.service.internal.websockets.config to spring.core, spring.context;

    // Gson needs superpowers as well
    opens io.hotmoka.service.models.errors to com.google.gson;
    opens io.hotmoka.service.models.requests to com.google.gson;
    opens io.hotmoka.service.models.responses to com.google.gson;
    opens io.hotmoka.service.models.signatures to com.google.gson;
    opens io.hotmoka.service.models.updates to com.google.gson;
    opens io.hotmoka.service.models.values to com.google.gson;

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
    requires org.apache.tomcat.embed.websocket;

    // these make it possible to compile under Eclipse...
    requires static spring.core;
    requires static org.apache.tomcat.embed.core;
}