package io.hotmoka.network.service.add;

import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.requests.*;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.crypto.SignatureAlgorithm;
import io.hotmoka.network.model.Error;
import io.hotmoka.network.model.transaction.*;
import io.hotmoka.network.service.NetworkService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.util.Base64;
import java.util.Collection;
import java.util.stream.Stream;


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
    public ResponseEntity<Object> addJarStoreTransaction(JarStoreTransactionRequestModel request) {

        return this.map(node -> {
            SignatureAlgorithm<NonInitialTransactionRequest<?>> signature = node.getSignatureAlgorithmForRequests();
            PrivateKey privateKey = null; // TODO: create this
            byte[] jar = Base64.getDecoder().decode(request.getJar());

            LocalTransactionReference[] dependencies = Stream.ofNullable(request.getDependencies())
                    .flatMap(Collection::stream)
                    .map(transactionModel -> new LocalTransactionReference(transactionModel.getHash()))
                    .toArray(LocalTransactionReference[]::new);

            return okResponseOf(node.addJarStoreTransaction(new JarStoreTransactionRequest(
                            NonInitialTransactionRequest.Signer.with(signature, privateKey),
                            new StorageReference(new LocalTransactionReference(request.getCaller()), request.getCallerProgressive()),
                            request.getNonce(),
                            request.getChainId(),
                            request.getGasLimit(),
                            request.getGasPrice(),
                            node.getTakamakaCode(),
                            jar,
                            dependencies
                    ))
            );
        });
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
