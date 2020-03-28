package io.hotmoka.tendermint;

import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Paths;

public class Main {
	public static void main(String[] args) throws Exception {
		/*Gson gson = new Gson();
		
		String msg = "{\"jsonrpc\": \"2.0\",\"id\": null,\"result\": {\"check_tx\": {\"code\": 0,\"data\": null,\"log\": \"\",\"info\": \"\",\"gasWanted\": \"0\",\"gasUsed\": \"0\",\"events\": [],\"codespace\": \"\"},\"deliver_tx\": {\"\n" + 
				"code\": 0,\"data\": null,\"log\": \"\",\"info\": \"\",\"gasWanted\": \"0\",\"gasUsed\": \"0\",\"events\": [],\"codespace\": \"\"},\"hash\": \"A8F9D987518D25B4FA7D09E6BD9D7E8A791D8BA1090236310BC547F9FC032C0F\",\"height\": \"13\"}}";

		BroadcastTxResponse response = gson.fromJson(msg, BroadcastTxResponse.class);
		System.out.println("jsonrpc: " + response.jsonrpc);
		System.out.println("id: " + response.id);
		System.out.println("error: " + response.error);
		System.out.println("result.hash: " + response.result.hash);
		*/
		
		try (TendermintBlockchain blockchain = TendermintBlockchain.of
				(new URL("http://localhost:26657"), Paths.get("../io-takamaka-code/target/io-takamaka-code-1.0.jar"), BigInteger.valueOf(200_000))) {

			System.out.println("stop Tendermint, then shut down this process");
			Thread.sleep(100_000);
		}
		catch (Exception e) {
			System.out.println("stop Tendermint, then shut down this process");
			Thread.sleep(100_000);
		}
	}
}