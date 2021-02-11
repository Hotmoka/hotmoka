module io.hotmoka.remote {
    exports io.hotmoka.remote;
    requires transitive io.hotmoka.nodes;
    requires transitive io.hotmoka.beans;
    requires java.instrument;
    requires io.hotmoka.service;


    exports io.hotmoka.remote.internal.http.client to spring.web;
    exports io.hotmoka.remote.internal.websockets.client to spring.messaging;

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
    requires org.apache.tomcat.embed.websocket;

    // these make it possible to compile under Eclipse...
    requires static spring.core;
    requires static org.apache.tomcat.embed.core;
}