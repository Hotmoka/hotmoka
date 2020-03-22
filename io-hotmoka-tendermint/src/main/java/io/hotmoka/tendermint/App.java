package io.hotmoka.tendermint;

import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;

import java.io.IOException;

public class App {
    public static void main(String[] args) throws IOException, InterruptedException {
        try (Environment env = Environments.newInstance("tmp/storage")) {
            KVStoreApp app = new KVStoreApp(env);
            GrpcServer server = new GrpcServer(app, 26658);
            server.start();
            server.blockUntilShutdown();
        }
    }
}
