package io.hotmoka.tendermint;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

class GrpcServer {
    private final Server server;
    private final int port;

    GrpcServer(BindableService service, int port) {
        this.port = port;
        this.server = ServerBuilder.forPort(port)
                .addService(service)
                .build();
    }

    void start() throws IOException {
        server.start();
        System.out.println("gRPC server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.print("shutting down gRPC server since JVM is shutting down... ");
            server.shutdown();
            System.out.println("done");
        }));
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    void blockUntilShutdown() throws InterruptedException {
        server.awaitTermination();
    }
}