/**
 * 
 */
package io.takamaka.code.tests;

import static io.hotmoka.beans.types.BasicTypes.INT;
import static java.math.BigInteger.ONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.network.Config;
import io.hotmoka.network.NodeService;
import io.hotmoka.network.models.requests.ConstructorCallTransactionRequestModel;
import io.hotmoka.network.models.requests.JarStoreInitialTransactionRequestModel;
import io.hotmoka.network.models.updates.ClassTagModel;
import io.hotmoka.network.models.updates.StateModel;
import io.hotmoka.network.models.values.StorageReferenceModel;

/**
 * A test for creating a network server from a Hotmoka node.
 */
class NetworkFromNode extends TakamakaTest {
	private static final BigInteger ALL_FUNDS = BigInteger.valueOf(1_000_000_000);
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);
	private static final ConstructorSignature CONSTRUCTOR_INTERNATIONAL_TIME = new ConstructorSignature("io.takamaka.tests.basicdependency.InternationalTime", INT, INT, INT);

	private final Config configNoBanner = new Config.Builder().setPort(8080).setSpringBannerModeOn(false).build();

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

	@BeforeEach
	void beforeEach() throws Exception {
		setNode("basicdependency.jar", ALL_FUNDS, BigInteger.ZERO);
		master = account(0);
		key = privateKey(0);
		classpath = addJarStoreTransaction(key, master, BigInteger.valueOf(10000), BigInteger.ONE, takamakaCode(), bytesOf("basic.jar"), jar());
	}

	@Test @DisplayName("starts a network server from a Hotmoka node")
	void startNetworkFromNode() {
		Config config = new Config.Builder().setPort(8080).setSpringBannerModeOn(true).build();
		try (NodeService nodeRestService = NodeService.of(config, nodeWithJarsView)) {
		}
	}

	@Test @DisplayName("starts a network server from a Hotmoka node and runs getTakamakaCode()")
	void testGetTakamakaCode() throws InterruptedException, IOException {
		String answer;

		try (NodeService nodeRestService = NodeService.of(configNoBanner, nodeWithJarsView)) {
			answer = curl(new URL("http://localhost:8080/get/takamakaCode"));
		}

		assertTrue(answer.contains("\"hash\":\"" + nodeWithJarsView.getTakamakaCode().getHash()));
	}

	@Test @DisplayName("starts a network server from a Hotmoka node and runs addJarStoreInitialTransaction()")
	void addJarStoreInitialTransaction() throws InterruptedException, IOException {
		Gson gson = new Gson();
		String result;

		try (NodeService nodeRestService = NodeService.of(configNoBanner, nodeWithJarsView)) {
			JarStoreInitialTransactionRequest request = new JarStoreInitialTransactionRequest(Files.readAllBytes(Paths.get("jars/c13.jar")), nodeWithJarsView.getTakamakaCode());
			result = post("http://localhost:8080/add/jarStoreInitialTransaction", gson.toJson(new JarStoreInitialTransactionRequestModel(request)));
		}

		assertEquals("{\"message\":\"Transaction rejected\"}", result);
	}

	@Test @DisplayName("starts a network server from a Hotmoka node and runs addJarStoreInitialTransaction() without a jar")
	void addJarStoreInitialTransactionWithoutAJar() throws InterruptedException, IOException {
		String result;

		try (NodeService nodeRestService = NodeService.of(configNoBanner, nodeWithJarsView)) {
			String jar = null;
			JsonObject bodyJson = new JsonObject();
			bodyJson.addProperty("jar", jar);
			result = post("http://localhost:8080/add/jarStoreInitialTransaction", bodyJson.toString());
		}

		assertEquals("{\"message\":\"unexpected null jar\"}", result);
	}

	@Test @DisplayName("starts a network server from a Hotmoka node and calls addConstructorCallTransaction - new Sub(1973")
	void addConstructorCallTransaction() throws InterruptedException, IOException, SignatureException, InvalidKeyException, NoSuchAlgorithmException {
		String result;
		Gson gson = new Gson();

		try (NodeService nodeRestService = NodeService.of(configNoBanner, nodeWithJarsView)) {
			ConstructorCallTransactionRequest request = new ConstructorCallTransactionRequest(
					NonInitialTransactionRequest.Signer.with(signature(), key),
					master,
					ONE,
					chainId,
					_20_000,
					ONE,
					classpath,
					new ConstructorSignature("io.takamaka.tests.basic.Sub", INT),
					new IntValue(1973)
			);

			result = post("http://localhost:8080/add/constructorCallTransaction", gson.toJson(new ConstructorCallTransactionRequestModel(request)));
		}

		assertNotNull(gson.fromJson(result, StorageReferenceModel.class).transaction);
	}

	@Test @DisplayName("starts a network server from a Hotmoka node, creates an object and calls getState() on it")
	void testGetState() throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, TransactionException, CodeExecutionException, TransactionRejectedException, IOException {
		Gson gson = new Gson();

		try (NodeService nodeRestService = NodeService.of(configNoBanner, nodeWithJarsView)) {
			ConstructorCallTransactionRequest request = new ConstructorCallTransactionRequest(
					NonInitialTransactionRequest.Signer.with(signature(), key),
					master,
					ONE,
					chainId,
					_20_000,
					ONE,
					classpath,
					CONSTRUCTOR_INTERNATIONAL_TIME,
					new IntValue(13), new IntValue(25), new IntValue(40)
			);

			// we execute the creation of the object
			String result = post("http://localhost:8080/add/constructorCallTransaction", gson.toJson(new ConstructorCallTransactionRequestModel(request)));
			StorageReferenceModel object = gson.fromJson(result, StorageReferenceModel.class);

			// we query the state of the object
			result = get("http://localhost:8080/get/state", gson.toJson(object));
			StateModel state = gson.fromJson(result, StateModel.class);

			// the state contains two updates
			assertSame(2L, state.getUpdates().count());
		}
	}

	@Test @DisplayName("starts a network server from a Hotmoka node, creates an object and calls getState() on it")
	void testGetClassTag() throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, TransactionException, CodeExecutionException, TransactionRejectedException, IOException {
		Gson gson = new Gson();

		try (NodeService nodeRestService = NodeService.of(configNoBanner, nodeWithJarsView)) {
			ConstructorCallTransactionRequest request = new ConstructorCallTransactionRequest(
					NonInitialTransactionRequest.Signer.with(signature(), key),
					master,
					ONE,
					chainId,
					_20_000,
					ONE,
					classpath,
					CONSTRUCTOR_INTERNATIONAL_TIME,
					new IntValue(13), new IntValue(25), new IntValue(40)
			);

			// we execute the creation of the object
			String result = post("http://localhost:8080/add/constructorCallTransaction", gson.toJson(new ConstructorCallTransactionRequestModel(request)));
			StorageReferenceModel object = gson.fromJson(result, StorageReferenceModel.class);

			// we query the class tag of the object
			result = get("http://localhost:8080/get/classTag", gson.toJson(object));
			ClassTagModel classTag = gson.fromJson(result, ClassTagModel.class);

			// the state that the class tag holds the name of the class that has been created
			assertEquals(CONSTRUCTOR_INTERNATIONAL_TIME.definingClass.name, classTag.className);
		}
	}

	private static String curl(URL url) throws IOException {
	    try (InputStream is = url.openStream();
	    	 BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
	    	return br.lines().collect(Collectors.joining("\n"));
	    }
	}

	private static String post(String url, String bodyJson) throws IOException {
		URL urlObj = new URL (url);
		HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json; utf-8");
		con.setRequestProperty("Accept", "application/json");
		con.setDoOutput(true);

		try(OutputStream os = con.getOutputStream()) {
			byte[] input = bodyJson.getBytes("utf-8");
			os.write(input, 0, input.length);
		}

		try(BufferedReader br = new BufferedReader(new InputStreamReader(con.getResponseCode() > 299 ? con.getErrorStream() : con.getInputStream(), "utf-8"))) {
			return br.lines().collect(Collectors.joining("\n"));
		}
	}

	private static String get(String url, String bodyJson) throws IOException {
		URL urlObj = new URL (url);
		HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
		con.setRequestMethod("GET");
		con.setRequestProperty("Content-Type", "application/json; utf-8");
		con.setRequestProperty("Accept", "application/json");
		con.setDoOutput(true);

		try(OutputStream os = con.getOutputStream()) {
			byte[] input = bodyJson.getBytes("utf-8");
			os.write(input, 0, input.length);
		}

		try(BufferedReader br = new BufferedReader(new InputStreamReader(con.getResponseCode() > 299 ? con.getErrorStream() : con.getInputStream(), "utf-8"))) {
			return br.lines().collect(Collectors.joining("\n"));
		}
	}
}