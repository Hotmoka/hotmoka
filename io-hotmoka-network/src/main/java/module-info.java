module io.hotmoka.network {
	exports io.hotmoka.network.requests;
	exports io.hotmoka.network.values;
	exports io.hotmoka.network.updates;
	exports io.hotmoka.network.signatures;
	exports io.hotmoka.network.responses;
	exports io.hotmoka.network.errors;
	exports io.hotmoka.network;

    // Gson needs superpowers
    opens io.hotmoka.network.errors to com.google.gson;
    opens io.hotmoka.network.requests to com.google.gson;
    opens io.hotmoka.network.responses to com.google.gson;
    opens io.hotmoka.network.signatures to com.google.gson;
    opens io.hotmoka.network.updates to com.google.gson;
    opens io.hotmoka.network.values to com.google.gson;

	requires transitive io.hotmoka.beans;
}