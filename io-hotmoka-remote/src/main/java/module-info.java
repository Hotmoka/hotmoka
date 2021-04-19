module io.hotmoka.remote {
    exports io.hotmoka.remote;
    requires transitive io.hotmoka.nodes;
    requires transitive io.hotmoka.beans;
    requires java.instrument;
    requires io.hotmoka.network;

    exports io.hotmoka.remote.internal.websockets.client to nv.websocket.client;

    requires org.slf4j;
    requires com.google.gson;
    requires nv.websocket.client;
}