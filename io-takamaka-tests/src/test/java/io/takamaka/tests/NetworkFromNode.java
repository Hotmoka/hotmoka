/**
 * 
 */
package io.takamaka.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.network.Config;
import io.hotmoka.network.NodeService;

/**
 * A test for creating a network server from a Hotmoka node.
 */
class NetworkFromNode extends TakamakaTest {
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);
	private final Config configNoBanner = new Config.Builder().setPort(8080).setSpringBannerModeOn(false).build();

	@BeforeEach
	void beforeEach() throws Exception {
		setNode("abstractfail.jar", _20_000, _20_000);
	}

	@Test @DisplayName("starts a network server from a Hotmoka node")
	void startNetworkFromNode() {
		Config config = new Config.Builder().setPort(8080).setSpringBannerModeOn(true).build();
		NodeService nodeRestService = new NodeService(config, nodeWithJarsView);
		nodeRestService.start();
		nodeRestService.stop();
	}

	@Test @DisplayName("starts a network server from a Hotmoka node and runs getTakamakaCode()")
	void queryTakamakaCode() throws InterruptedException, IOException {
		NodeService nodeRestService = new NodeService(configNoBanner, nodeWithJarsView);
		nodeRestService.start();
		String answer = curl(new URL("http://localhost:8080/get/takamakaCode"));
		nodeRestService.stop();
		assertEquals("{\"hash\":\"" + nodeWithJarsView.getTakamakaCode().getHash() + "\"}", answer);
	}

	@Test @DisplayName("starts a network server from a Hotmoka node and runs addJarStoreInitialTransaction()")
	void addJarStoreInitialTransaction() throws InterruptedException, IOException {
		NodeService nodeRestService = new NodeService(configNoBanner, nodeWithJarsView);
		nodeRestService.start();
		String bodyJson = "{\"jar\": \"UEsDBBQACAgIAPi8Wk8AAAAAAAAAAAAAAAAJAAQATUVUQS1JTkYv/soAAAMAUEsHCAAAAAACAAAAAAAAAFBLAwQUAAgICAD4vFpPAAAAAAAAAAAAAAAAFAAAAE1FVEEtSU5GL01BTklGRVNULk1G803My0xLLS7RDUstKs7Mz7NSMNQz4OVyLkpNLElN0XWqBAoARfRMFDRCk0rzSko1ebl4uQBQSwcI/0lOkzUAAAA2AAAAUEsDBBQACAgIAPi8Wk8AAAAAAAAAAAAAAAAHAAAAQy5jbGFzcz2OT2vCQBDF3yQxaWL815499NamYI5epJdAQRAvSu+rXcK2mkCI4ofqpaAUeugH8ENJXxbpYd7OzM7vzZwvP78AxriL4KEXwkU/wCDArUAOjKnAn5jC1M8C9+HxVeBl5ZsW9Gam0PPddqWrpVpt2HFzXdshMtGi3FVr/WKaDz8bvau9ihHgJkYLPn0zwdCUaa0+1JaRrmmablSRp4u6rFSucc9bPN4mcBqGmcOcFtSQ1ZOtgXbyDUn6nROcLzseUbuEQdwjGhEWtBFfwSHfBmwlRzif/4hvmyG1Y3d1/wBQSwcI0UP7St8AAAAcAQAAUEsBAhQAFAAICAgA+LxaTwAAAAACAAAAAAAAAAkABAAAAAAAAAAAAAAAAAAAAE1FVEEtSU5GL/7KAABQSwECFAAUAAgICAD4vFpP/0lOkzUAAAA2AAAAFAAAAAAAAAAAAAAAAAA9AAAATUVUQS1JTkYvTUFOSUZFU1QuTUZQSwECFAAUAAgICAD4vFpP0UP7St8AAAAcAQAABwAAAAAAAAAAAAAAAAC0AAAAQy5jbGFzc1BLBQYAAAAAAwADALIAAADIAQAAAAA=\"}";
		String result = post("http://localhost:8080/add/jarStoreInitialTransaction", bodyJson);
		nodeRestService.stop();
		assertEquals("{\"message\":\"Transaction rejected\"}", result);
	}

	@Test @DisplayName("starts a network server from a Hotmoka node and runs addJarStoreInitialTransaction() without a jar")
	void addJarStoreInitialTransactionWithoutAJar() throws InterruptedException, IOException {
		NodeService nodeRestService = new NodeService(configNoBanner, nodeWithJarsView);
		nodeRestService.start();
		String jar = null;
		JsonObject bodyJson = new JsonObject();
		bodyJson.addProperty("jar", jar);
		String result = post("http://localhost:8080/add/jarStoreInitialTransaction", bodyJson.toString());
		nodeRestService.stop();
		assertEquals("{\"message\":\"Transaction rejected: Jar missing\"}", result);
	}

	private String curl(URL url) throws IOException {
	    try (InputStream is = url.openStream();
	    	 BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
	    	return br.lines().collect(Collectors.joining("\n"));
	    }
	}

	private String post(String url, String bodyJson) throws IOException {
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
		if (con.getResponseCode() > 299) {
			try(BufferedReader br = new BufferedReader(new InputStreamReader(con.getErrorStream(), "utf-8"))) {
				return br.lines().collect(Collectors.joining("\n"));
			}
		} else {
			try(BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))) {
				return br.lines().collect(Collectors.joining("\n"));
			}
		}
	}
}