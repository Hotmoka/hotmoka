package io.hotmoka.network.service.add;

import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.InitializationTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.network.model.Error;
import io.hotmoka.network.model.transaction.GameteCreationTransactionRequestModel;
import io.hotmoka.network.model.transaction.JarStoreInitialTransactionRequestModel;
import io.hotmoka.network.model.transaction.RGGameteCreationTransactionRequestModel;
import io.hotmoka.network.model.transaction.TransactionModel;
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
            return okResponseOf(node.addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(jar, node.getTakamakaCode())));
        });
    }

    @Override
    public ResponseEntity<Object> addGameteCreationTransaction(GameteCreationTransactionRequestModel request) {
        return this.map(node -> okResponseOf(node.addGameteCreationTransaction(new GameteCreationTransactionRequest(
                        node.getTakamakaCode(),
                        request.getAmount(),
                        request.getPublicKey()
                ))
        ));
    }

    @Override
    public ResponseEntity<Object> addRedGreenGameteCreationTransaction(RGGameteCreationTransactionRequestModel request) {
        return this.map(node -> okResponseOf(node.addRedGreenGameteCreationTransaction(new RedGreenGameteCreationTransactionRequest(
                    node.getTakamakaCode(),
                    request.getAmount(),
                    request.getRedAmount(),
                    request.getPublicKey()
                ))
        ));
    }

    @Override
    public ResponseEntity<Object> addInitializationTransaction(TransactionModel request) {
        return this.map(node -> {
            StorageReference storageReference = new StorageReference(new LocalTransactionReference(request.getHash()), request.getProgressive());
            node.addInitializationTransaction(new InitializationTransactionRequest(node.getTakamakaCode(), storageReference));
            return noContentResponse();
        });
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
