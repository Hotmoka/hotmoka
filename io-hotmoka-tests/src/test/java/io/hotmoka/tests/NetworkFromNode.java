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

import static io.hotmoka.beans.StorageTypes.INT;
import static java.math.BigInteger.ONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.network.NetworkExceptionResponse;
import io.hotmoka.network.errors.ErrorModel;
import io.hotmoka.network.requests.ConstructorCallTransactionRequestModel;
import io.hotmoka.network.requests.JarStoreInitialTransactionRequestModel;
import io.hotmoka.network.responses.SignatureAlgorithmResponseModel;
import io.hotmoka.network.updates.ClassTagModel;
import io.hotmoka.network.updates.StateModel;
import io.hotmoka.network.values.StorageReferenceModel;
import io.hotmoka.network.values.TransactionReferenceModel;
import io.hotmoka.node.remote.internal.http.client.RestClientService;
import io.hotmoka.node.service.NodeServiceConfigBuilders;
import io.hotmoka.node.service.NodeServices;
import io.hotmoka.node.service.api.NodeServiceConfig;

/**
 * A test for creating a network server from a Hotmoka node.
 */
class NetworkFromNode extends HotmokaTest {
	private static final ConstructorSignature CONSTRUCTOR_INTERNATIONAL_TIME = new ConstructorSignature("io.hotmoka.examples.basicdependency.InternationalTime", INT, INT, INT);

	private final NodeServiceConfig config = NodeServiceConfigBuilders.defaults().setPort(8081).build();

	/**
	 * The account that holds all funds.
	 */
	private StorageReference master;

	/**
	 * The classpath of the classes being tested.
	 */
	private TransactionReference classpath;

	/**
	 * The private key of {@linkplain #master}.
	 */
	private PrivateKey key;

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("basicdependency.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000, BigInteger.ZERO);
		master = account(0);
		key = privateKey(0);
		classpath = addJarStoreTransaction(key, master, BigInteger.valueOf(5000000), BigInteger.ONE, takamakaCode(), bytesOf("basic.jar"), jar());
	}

	@Test @DisplayName("starts a network server from a Hotmoka node")
	void startNetworkFromNode() {
		var config = NodeServiceConfigBuilders.defaults().setPort(8081).build();
		try (var service = NodeServices.of(config, node)) {
		}
	}

	@Test @DisplayName("starts a network server from a Hotmoka node and checks its signature algorithm")
	void startNetworkFromNodeAndTestSignatureAlgorithm() {
		SignatureAlgorithmResponseModel answer;
		RestClientService service = new RestClientService();

		try (var nodeRestService = NodeServices.of(config, node)) {
			answer = service.get("http://localhost:8081/get/nameOfSignatureAlgorithmForRequests", SignatureAlgorithmResponseModel.class);
		}

		assertTrue("ed25519".equals(answer.algorithm) || "ed25519det".equals(answer.algorithm) || "sha256dsa".equals(answer.algorithm)
			|| "qtesla1".equals(answer.algorithm) || "qtesla3".equals(answer.algorithm) || "empty".equals(answer.algorithm));
	}

	@Test @DisplayName("starts a network server from a Hotmoka node and runs getTakamakaCode()")
	void testGetTakamakaCode() {
		TransactionReferenceModel result;
		RestClientService service = new RestClientService();

		try (var nodeRestService = NodeServices.of(config, node)) {
			result = service.get("http://localhost:8081/get/takamakaCode", TransactionReferenceModel.class);
		}

		assertEquals(node.getTakamakaCode().getHash(), result.hash);
	}

	@Test @DisplayName("starts a network server from a Hotmoka node and runs addJarStoreInitialTransaction()")
	void addJarStoreInitialTransaction() throws IOException {
		ErrorModel errorModel = null;

		try (var nodeRestService = NodeServices.of(config, node)) {
			var request = new JarStoreInitialTransactionRequest(Files.readAllBytes(Paths.get("jars/c13.jar")), node.getTakamakaCode());

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
		assertEquals("Cannot run a JarStoreInitialTransactionRequest in an already initialized node", errorModel.message);
		assertEquals(TransactionRejectedException.class.getName(), errorModel.exceptionClassName);
	}

	@Test @DisplayName("starts a network server from a Hotmoka node and runs addJarStoreInitialTransaction() without a jar")
	void addJarStoreInitialTransactionWithoutJar() {
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

	@Test @DisplayName("starts a network server from a Hotmoka node and calls addConstructorCallTransaction - new Sub(1973)")
	void addConstructorCallTransaction() throws SignatureException, InvalidKeyException {
		StorageReferenceModel result;

		try (var nodeRestService = NodeServices.of(config, node)) {
			var request = new ConstructorCallTransactionRequest(
					signature().getSigner(key, SignedTransactionRequest::toByteArrayWithoutSignature),
					master,
					ONE,
					chainId,
					_50_000,
					ONE,
					classpath,
					new ConstructorSignature("io.hotmoka.examples.basic.Sub", INT),
					StorageValues.intOf(1973)
			);

			var service = new RestClientService();
			result = service.post(
					"http://localhost:8081/add/constructorCallTransaction",
					new ConstructorCallTransactionRequestModel(request),
					StorageReferenceModel.class
			);
		}

		assertNotNull(result.transaction);
	}

	@Test @DisplayName("starts a network server from a Hotmoka node, creates an object and calls getState() on it")
	void testGetState() throws InvalidKeyException, SignatureException {
		StateModel state;

		try (var nodeRestService = NodeServices.of(config, node)) {
			var request = new ConstructorCallTransactionRequest(
					signature().getSigner(key, SignedTransactionRequest::toByteArrayWithoutSignature),
					master,
					ONE,
					chainId,
					_50_000,
					ONE,
					classpath,
					CONSTRUCTOR_INTERNATIONAL_TIME,
					StorageValues.intOf(13), StorageValues.intOf(25), StorageValues.intOf(40)
			);

			// we execute the creation of the object
			var service = new RestClientService();
			StorageReferenceModel object = service.post(
					"http://localhost:8081/add/constructorCallTransaction",
					new ConstructorCallTransactionRequestModel(request),
					StorageReferenceModel.class
			);

			// we query the state of the object
			state = service.post("http://localhost:8081/get/state", object, StateModel.class);
		}

		// the state contains two updates
		assertSame(2, state.updates.size());
	}

	@Test @DisplayName("starts a network server from a Hotmoka node, creates an object and calls getState() on it")
	void testGetClassTag() throws InvalidKeyException, SignatureException {
		ClassTagModel classTag;

		try (var nodeRestService = NodeServices.of(config, node)) {
			var request = new ConstructorCallTransactionRequest(
					signature().getSigner(key, SignedTransactionRequest::toByteArrayWithoutSignature),
					master,
					ONE,
					chainId,
					_50_000,
					ONE,
					classpath,
					CONSTRUCTOR_INTERNATIONAL_TIME,
					StorageValues.intOf(13), StorageValues.intOf(25), StorageValues.intOf(40)
			);

			// we execute the creation of the object
			var service = new RestClientService();
			StorageReferenceModel object = service.post(
					"http://localhost:8081/add/constructorCallTransaction",
					new ConstructorCallTransactionRequestModel(request),
					StorageReferenceModel.class
			);

			// we query the class tag of the object
			classTag = service.post("http://localhost:8081/get/classTag", object, ClassTagModel.class);
		}

		// the state that the class tag holds the name of the class that has been created
		assertEquals(CONSTRUCTOR_INTERNATIONAL_TIME.definingClass.getName(), classTag.className);
	}
}