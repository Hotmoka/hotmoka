/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.tendermint.abci;

import java.io.IOException;

import io.grpc.ServerBuilder;

/**
 * Creates a server connected to an application through the ABCI.
 */
public class Server {
	private final io.grpc.Server server;

	/**
	 * Creates the server.
	 * 
	 * @param port the port where the server is published
	 * @param abci the ABCI of the application
	 */
	public Server(int port, ABCI abci) {
		this.server = ServerBuilder.forPort(port).addService(abci.service).build();
	}

	/**
	 * Starts the server.
	 * 
	 * @throws IOException if an I/O error occurs
	 */
	public void start() throws IOException {
		server.start();
	}

	/**
	 * Checks if the server is shutdown.
	 * 
	 * @return true if and only if that condition holds
	 */
	public boolean isShutdown() {
		return server.isShutdown();
	}

	/**
	 * Shuts down this server.
	 */
	public void shutdown() {
		server.shutdown();
	}

	/**
	 * Awaits for the termination of this server, after being shut down.
	 * 
	 * @throws InterruptedException if the thread has been interrupted while waiting
	 */
	public void awaitTermination() throws InterruptedException {
		server.awaitTermination();
	}
}