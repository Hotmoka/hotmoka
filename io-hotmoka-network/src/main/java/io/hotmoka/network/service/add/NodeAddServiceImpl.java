package io.hotmoka.network.service.add;

import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.network.model.Error;
import io.hotmoka.network.model.transaction.GameteCreationTransactionRequestModel;
import io.hotmoka.network.model.transaction.JarStoreInitialTransactionRequestModel;
import io.hotmoka.network.service.NetworkService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Base64;


@Service
public class NodeAddServiceImpl extends NetworkService implements NodeAddService {


    @Override
    public ResponseEntity<Object> addJarStoreInitialTransaction(JarStoreInitialTransactionRequestModel request) {
        return this.map(node -> {

            if (request.getJar() == null)
                return badRequestResponseOf(new Error("Transaction rejected: Jar missing"));

            byte[] jar = Base64.getDecoder().decode(request.getJar());
            return okResponseOf(node.addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(jar)));
        });
    }

    @Override
    public ResponseEntity<Object> addGameteCreationTransaction(GameteCreationTransactionRequestModel request) {
        return this.map(node ->
                okResponseOf(node.addGameteCreationTransaction(new GameteCreationTransactionRequest(
                                node.getTakamakaCode(),
                                request.getAmount(),
                                request.getPublicKey()
                        ))
                )
        );
    }

    @Override
    public ResponseEntity<Object> addRedGreenGameteCreationTransaction() {
        return null;
    }

    @Override
    public ResponseEntity<Object> addInitializationTransaction() {
        return null;
    }

    @Override
    public ResponseEntity<Object> addJarStoreTransaction() {
        return null;
    }

    @Override
    public ResponseEntity<Object> addConstructorCallTransaction() {
        return null;
    }

    @Override
    public ResponseEntity<Object> addInstanceMethodCallTransaction() {
        return null;
    }

    @Override
    public ResponseEntity<Object> addStaticMethodCallTransaction() {
        return null;
    }
}
