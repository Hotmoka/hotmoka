package io.hotmoka.tendermint_abci;

import java.io.IOException;

import io.grpc.ServerBuilder;

public class Server {
	private final io.grpc.Server server;

	public Server(int port, ABCI abci) {
		this.server = ServerBuilder.forPort(port).addService(abci.service).build();
	}

	public void start() throws IOException {
		server.start();
	}

	public boolean isShutdown() {
		return server.isShutdown();
	}

	public void shutdown() {
		server.shutdown();
	}

	public void awaitTermination() throws InterruptedException {
		server.awaitTermination();
	}
}