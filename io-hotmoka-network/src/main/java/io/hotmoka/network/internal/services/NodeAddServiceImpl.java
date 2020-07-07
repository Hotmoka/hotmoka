package io.hotmoka.network.internal.services;

import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.requests.*;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.crypto.SignatureAlgorithm;
import io.hotmoka.network.internal.models.Error;
import io.hotmoka.network.internal.models.storage.StorageModel;
import io.hotmoka.network.internal.models.transactions.*;
import io.hotmoka.network.internal.util.StorageResolver;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
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
    public ResponseEntity<Object> addInitializationTransaction(StorageModel request) {
        return this.map(node -> {
            StorageReference manifest = StorageResolver.resolveStorageReference(request.getHash(), request.getProgressive());
            node.addInitializationTransaction(new InitializationTransactionRequest(node.getTakamakaCode(), manifest));
            return noContentResponse();
        });
    }

    @Override
    public ResponseEntity<Object> addJarStoreTransaction(JarStoreTransactionRequestModel request) {

        return this.map(node -> {
            SignatureAlgorithm<NonInitialTransactionRequest<?>> signature = node.getSignatureAlgorithmForRequests();
            PrivateKey privateKey = null; // TODO

            byte[] jar = Base64.getDecoder().decode(request.getJar());
            StorageReference caller = StorageResolver.resolveStorageReference(request.getCaller(), request.getCallerProgressive());
            LocalTransactionReference[] dependencies = StorageResolver.resolveJarDependencies(request.getDependencies());

            return okResponseOf(node.addJarStoreTransaction(new JarStoreTransactionRequest(
                            NonInitialTransactionRequest.Signer.with(signature, privateKey),
                            caller,
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
    public ResponseEntity<Object> addConstructorCallTransaction(ConstructorCallTransactionRequestModel request) {
        return this.map(node -> {
            SignatureAlgorithm<NonInitialTransactionRequest<?>> signature = node.getSignatureAlgorithmForRequests();
            PrivateKey privateKey = null; // TODO

            StorageReference caller = StorageResolver.resolveStorageReference(request.getCaller(), request.getCallerProgressive());
            ConstructorSignature constructor = new ConstructorSignature(request.getClassType(), StorageResolver.resolveStorageTypes(request.getValues()));
            StorageValue[] actuals = StorageResolver.resolveStorageValues(request.getValues());

            return okResponseOf(node.addConstructorCallTransaction(new ConstructorCallTransactionRequest(
                    NonInitialTransactionRequest.Signer.with(signature, privateKey),
                    caller,
                    request.getNonce(),
                    request.getChainId(),
                    request.getGasLimit(),
                    request.getGasPrice(),
                    node.getTakamakaCode(),
                    constructor,
                    actuals
            )));
        });
    }

    @Override
    public ResponseEntity<Object> addInstanceMethodCallTransaction(MethodCallTransactionRequestModel request) {
        return this.map(node -> {
            SignatureAlgorithm<NonInitialTransactionRequest<?>> signature = node.getSignatureAlgorithmForRequests();
            PrivateKey privateKey = null; // TODO

            MethodSignature methodSignature = StorageResolver.resolveMethodSignature(request);
            StorageReference caller = StorageResolver.resolveStorageReference(request.getCaller(), request.getCallerProgressive());
            StorageReference receiver =  StorageResolver.resolveStorageReference(request.getReceiver(), request.getReceiverProgressive());
            StorageValue[] actuals = StorageResolver.resolveStorageValues(request.getValues());

            return okResponseOf(node.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
                    NonInitialTransactionRequest.Signer.with(signature, privateKey),
                    caller,
                    request.getNonce(),
                    request.getChainId(),
                    request.getGasLimit(),
                    request.getGasPrice(),
                    node.getTakamakaCode(),
                    methodSignature,
                    receiver,
                    actuals
            )));
        });
    }

    @Override
    public ResponseEntity<Object> addStaticMethodCallTransaction(MethodCallTransactionRequestModel request) {
        return this.map(node -> {
            SignatureAlgorithm<NonInitialTransactionRequest<?>> signature = node.getSignatureAlgorithmForRequests();
            PrivateKey privateKey = null; // TODO

            MethodSignature methodSignature = StorageResolver.resolveMethodSignature(request);
            StorageReference caller = StorageResolver.resolveStorageReference(request.getCaller(), request.getCallerProgressive());
            StorageValue[] actuals = StorageResolver.resolveStorageValues(request.getValues());

            return okResponseOf(node.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest(
                    NonInitialTransactionRequest.Signer.with(signature, privateKey),
                    caller,
                    request.getNonce(),
                    request.getChainId(),
                    request.getGasLimit(),
                    request.getGasPrice(),
                    node.getTakamakaCode(),
                    methodSignature,
                    actuals
            )));
        });
    }

}
