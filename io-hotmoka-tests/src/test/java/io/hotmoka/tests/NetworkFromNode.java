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

package io.hotmoka.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

import io.hotmoka.beans.TransactionRequests;
import io.hotmoka.network.NetworkExceptionResponse;
import io.hotmoka.network.errors.ErrorModel;
import io.hotmoka.network.requests.JarStoreInitialTransactionRequestModel;
import io.hotmoka.network.values.TransactionReferenceModel;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.remote.internal.http.client.RestClientService;
import io.hotmoka.node.service.NodeServiceConfigBuilders;
import io.hotmoka.node.service.NodeServices;
import io.hotmoka.node.service.api.NodeServiceConfig;
import jakarta.websocket.DeploymentException;

/**
 * A test for creating a network server from a Hotmoka node.
 */
class NetworkFromNode extends HotmokaTest {

	private final NodeServiceConfig config = NodeServiceConfigBuilders.defaults().setPort(8081).build();

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("basicdependency.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000, BigInteger.ZERO);
	}

	@Test @DisplayName("starts a network server from a Hotmoka node")
	void startNetworkFromNode() throws DeploymentException, IOException {
		var config = NodeServiceConfigBuilders.defaults().setPort(8081).build();
		try (var service = NodeServices.of(config, node)) {
		}
	}

	@Test @DisplayName("starts a network server from a Hotmoka node and runs addJarStoreInitialTransaction()")
	void addJarStoreInitialTransaction() throws IOException, DeploymentException, NoSuchElementException, NodeException, TimeoutException, InterruptedException {
		ErrorModel errorModel = null;

		try (var nodeRestService = NodeServices.of(config, node)) {
			var request = TransactionRequests.jarStoreInitial(Files.readAllBytes(Paths.get("jars/c13.jar")), node.getTakamakaCode());

			try {
				var service = new RestClientService();
				service.post(
						"http://localhost:8081/add/jarStoreInitialTransaction",
						new JarStoreInitialTransactionRequestModel(request),
						TransactionReferenceModel.class
				);
			} catch (NetworkExceptionResponse networkExceptionResponse){
				errorModel = networkExceptionResponse.errorModel;
			}
		}

		assertNotNull(errorModel);
		assertEquals("Cannot run an initial transaction request in an already initialized node", errorModel.message);
		assertEquals(TransactionRejectedException.class.getName(), errorModel.exceptionClassName);
	}

	@Test @DisplayName("starts a network server from a Hotmoka node and runs addJarStoreInitialTransaction() without a jar")
	void addJarStoreInitialTransactionWithoutJar() throws DeploymentException, IOException {
		ErrorModel errorModel = null;

		try (var nodeRestService = NodeServices.of(config, node)) {
			var bodyJson = new JsonObject();
			bodyJson.addProperty("jar", (String) null);

			try {
				var service = new RestClientService();
				service.post(
						"http://localhost:8081/add/jarStoreInitialTransaction",
						bodyJson,
						TransactionReferenceModel.class
				);
			} catch (NetworkExceptionResponse networkExceptionResponse) {
				errorModel = networkExceptionResponse.errorModel;
			}
		}

		assertNotNull(errorModel);
		assertEquals("unexpected null jar", errorModel.message);
		assertEquals(RuntimeException.class.getName(), errorModel.exceptionClassName);
	}
}