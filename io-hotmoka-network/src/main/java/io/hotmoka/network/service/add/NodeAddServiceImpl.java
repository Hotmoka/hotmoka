package io.hotmoka.network.service.add;

import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.network.model.Error;
import io.hotmoka.network.model.transaction.TransactionRequestModel;
import io.hotmoka.network.service.NetworkService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Base64;


@Service
public class NodeAddServiceImpl extends NetworkService implements NodeAddService {


    @Override
    public ResponseEntity<Object> addJarStoreInitialTransaction(TransactionRequestModel transactionRequestModel) {
        return this.map(node -> {

            if (transactionRequestModel.getJar() == null)
                return badRequestResponseOf(new Error("Transaction rejected: Jar missing"));

            byte[] jar = Base64.getDecoder().decode(transactionRequestModel.getJar());
            return okResponseOf(node.addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(jar)));
        });
    }

    @Override
    public ResponseEntity<Object> addGameteCreationTransaction() {
        return null;
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
