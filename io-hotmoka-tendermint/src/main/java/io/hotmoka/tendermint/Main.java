package io.hotmoka.tendermint;

import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Paths;

public class Main {
	public static void main(String[] args) throws Exception {
		try (TendermintBlockchain blockchain = TendermintBlockchain.of
				(new URL("http://localhost:26657"), Paths.get("../io-takamaka-code/target/io-takamaka-code-1.0.jar"), BigInteger.valueOf(200_000))) {
			System.out.println("stop Tendermint, then shut down this process");
			Thread.sleep(100_000);
		}
	}
}