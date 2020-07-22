/**
 * 
 */
package io.takamaka.code.tests;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.network.Config;
import io.hotmoka.network.NodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.Base64;
import java.util.stream.Collectors;

import static io.hotmoka.beans.types.BasicTypes.INT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * A test for creating a network server from a Hotmoka node.
 */
class NetworkFromNode extends TakamakaTest {
	private static final BigInteger ALL_FUNDS = BigInteger.valueOf(1_000_000_000);
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);
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
	void queryTakamakaCode() throws InterruptedException, IOException {
		String answer;

		try (NodeService nodeRestService = NodeService.of(configNoBanner, nodeWithJarsView)) {
			answer = curl(new URL("http://localhost:8080/get/takamakaCode"));
		}

		assertEquals("{\"hash\":\"" + nodeWithJarsView.getTakamakaCode().getHash() + "\"}", answer);
	}

	@Test @DisplayName("starts a network server from a Hotmoka node and runs addJarStoreInitialTransaction()")
	void addJarStoreInitialTransaction() throws InterruptedException, IOException {
		String result;

		try (NodeService nodeRestService = NodeService.of(configNoBanner, nodeWithJarsView)) {
			// TODO: the request should include the returned value of takamakaCode() as its only "dependency" 
			String bodyJson = "{\"jar\": \"UEsDBBQACAgIAPi8Wk8AAAAAAAAAAAAAAAAJAAQATUVUQS1JTkYv/soAAAMAUEsHCAAAAAACAAAAAAAAAFBLAwQUAAgICAD4vFpPAAAAAAAAAAAAAAAAFAAAAE1FVEEtSU5GL01BTklGRVNULk1G803My0xLLS7RDUstKs7Mz7NSMNQz4OVyLkpNLElN0XWqBAoARfRMFDRCk0rzSko1ebl4uQBQSwcI/0lOkzUAAAA2AAAAUEsDBBQACAgIAPi8Wk8AAAAAAAAAAAAAAAAHAAAAQy5jbGFzcz2OT2vCQBDF3yQxaWL815499NamYI5epJdAQRAvSu+rXcK2mkCI4ofqpaAUeugH8ENJXxbpYd7OzM7vzZwvP78AxriL4KEXwkU/wCDArUAOjKnAn5jC1M8C9+HxVeBl5ZsW9Gam0PPddqWrpVpt2HFzXdshMtGi3FVr/WKaDz8bvau9ihHgJkYLPn0zwdCUaa0+1JaRrmmablSRp4u6rFSucc9bPN4mcBqGmcOcFtSQ1ZOtgXbyDUn6nROcLzseUbuEQdwjGhEWtBFfwSHfBmwlRzif/4hvmyG1Y3d1/wBQSwcI0UP7St8AAAAcAQAAUEsBAhQAFAAICAgA+LxaTwAAAAACAAAAAAAAAAkABAAAAAAAAAAAAAAAAAAAAE1FVEEtSU5GL/7KAABQSwECFAAUAAgICAD4vFpP/0lOkzUAAAA2AAAAFAAAAAAAAAAAAAAAAAA9AAAATUVUQS1JTkYvTUFOSUZFU1QuTUZQSwECFAAUAAgICAD4vFpP0UP7St8AAAAcAQAABwAAAAAAAAAAAAAAAAC0AAAAQy5jbGFzc1BLBQYAAAAAAwADALIAAADIAQAAAAA=\"}";
			result = post("http://localhost:8080/add/jarStoreInitialTransaction", bodyJson);
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

		assertEquals("{\"message\":\"Transaction rejected: Jar missing\"}", result);
	}

	@Test @DisplayName("starts a network server from a Hotmoka node and calls addConstructorCallTransaction - new Sub(1973")
	void addConstructorCallTransaction() throws InterruptedException, IOException, SignatureException, InvalidKeyException, NoSuchAlgorithmException {
		String result;

		try (NodeService nodeRestService = NodeService.of(configNoBanner, nodeWithJarsView)) {

			byte [] signature = new ConstructorCallTransactionRequest(
					NonInitialTransactionRequest.Signer.with(signature(), key),
					master,
					BigInteger.ONE,
					chainId,
					_20_000,
					BigInteger.ONE,
					classpath,
					new ConstructorSignature("io.takamaka.tests.basic.Sub", INT),
					new IntValue(1973)
			).getSignature();

			String base64Signature = Base64.getEncoder().encodeToString(signature);

			JsonArray values = new JsonArray();
			values.add(buildValue("int", "1973"));

			JsonObject bodyJson = buildJsonAddConstructorCallTransaction(
					base64Signature,
					master,
					BigInteger.ONE,
					chainId,
					classpath.getHash(),
					_20_000,
					BigInteger.ONE,
					"io.takamaka.tests.basic.Sub",
					values
			);
			result = post("http://localhost:8080/add/constructorCallTransaction", bodyJson.toString());
		}

		JsonObject storageReference = (JsonObject) JsonParser.parseString(result);
		assertNotNull(storageReference.get("hash"));
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

	private JsonObject buildJsonAddConstructorCallTransaction(String signature, StorageReference caller, BigInteger nonce, String chainId, String classpath, BigInteger gasLimit, BigInteger gasPrice, String constructorType, JsonArray values) {
		JsonObject bodyJson = new JsonObject();
		bodyJson.addProperty("classpath", classpath);
		bodyJson.addProperty("signature", signature);
		bodyJson.add("caller", buildCaller(caller));
		bodyJson.addProperty("nonce", nonce);
		bodyJson.addProperty("chainId", chainId);
		bodyJson.addProperty("gasLimit", gasLimit);
		bodyJson.addProperty("gasPrice", gasPrice);
		bodyJson.addProperty("constructorType", constructorType);
		bodyJson.add("values", values);

		return bodyJson;
	}

	private JsonObject buildCaller(StorageReference caller) {
		JsonObject json = new JsonObject();
		json.addProperty("hash", caller.transaction.getHash());
		json.addProperty("progressive", caller.progressive);
		return json;
	}

	private JsonObject buildValue(String type, String value) {
		JsonObject json = new JsonObject();
		json.addProperty("type", type);
		json.addProperty("value", value);
		return json;
	}
}