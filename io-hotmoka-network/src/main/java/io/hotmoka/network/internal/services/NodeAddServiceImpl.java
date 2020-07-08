package io.hotmoka.network.internal.services;

import java.security.PrivateKey;
import java.util.Base64;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.network.internal.models.transactions.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.InitializationTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.crypto.SignatureAlgorithm;
import io.hotmoka.network.internal.models.Error;
import io.hotmoka.network.internal.models.storage.StorageModel;
import io.hotmoka.network.internal.util.StorageResolver;
import io.hotmoka.nodes.Node;


@Service
public class NodeAddServiceImpl extends NetworkService implements NodeAddService {


	@Override
	public ResponseEntity<Object> addJarStoreInitialTransaction(JarStoreInitialTransactionRequestModel request) {
		if (request.getJar() == null)
			return badRequestResponseOf(new Error("Transaction rejected: Jar missing"));

		return wrapExceptions(() -> {
            byte[] jar = Base64.getDecoder().decode(request.getJar());
			Node node = getNode();

			return okResponseOf(node.addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(jar, StorageResolver.resolveJarDependencies(request.getDependencies()))));
		});
	}

    @Override
    public ResponseEntity<Object> addGameteCreationTransaction(GameteCreationTransactionRequestModel request) {
        return wrapExceptions(() -> okResponseOf(getNode().addGameteCreationTransaction(new GameteCreationTransactionRequest(
                        StorageResolver.resolveTransactionReference(request.getClasspath()),
                        request.getAmount(),
                        request.getPublicKey()
                ))
        ));
    }

    @Override
    public ResponseEntity<Object> addRedGreenGameteCreationTransaction(RGGameteCreationTransactionRequestModel request) {
        return wrapExceptions(() -> okResponseOf(getNode().addRedGreenGameteCreationTransaction(new RedGreenGameteCreationTransactionRequest(
                    StorageResolver.resolveTransactionReference(request.getClasspath()),
                    request.getAmount(),
                    request.getRedAmount(),
                    request.getPublicKey()
                ))
        ));
    }

    @Override
    public ResponseEntity<Object> addInitializationTransaction(InitializationTransactionRequestModel request) {
        return wrapExceptions(() -> {
            StorageReference manifest = StorageResolver.resolveStorageReference(request.getManifest().getHash(), request.getManifest().getProgressive());
            getNode().addInitializationTransaction(new InitializationTransactionRequest(StorageResolver.resolveTransactionReference(request.getClasspath()), manifest));
            return noContentResponse();
        });
    }

    @Override
    public ResponseEntity<Object> addJarStoreTransaction(JarStoreTransactionRequestModel request) {
        return wrapExceptions(() -> {
            SignatureAlgorithm<NonInitialTransactionRequest<?>> signature = getNode().getSignatureAlgorithmForRequests();
            PrivateKey privateKey = null; // TODO

            byte[] jar = Base64.getDecoder().decode(request.getJar());
            StorageReference caller = StorageResolver.resolveStorageReference(request.getCaller().getHash(), request.getCaller().getProgressive());
            LocalTransactionReference[] dependencies = StorageResolver.resolveJarDependencies(request.getDependencies());
            TransactionReference classpath = StorageResolver.resolveTransactionReference(request.getClasspath());

            return okResponseOf(getNode().addJarStoreTransaction(new JarStoreTransactionRequest(
                            NonInitialTransactionRequest.Signer.with(signature, privateKey),
                            caller,
                            request.getNonce(),
                            request.getChainId(),
                            request.getGasLimit(),
                            request.getGasPrice(),
                            classpath,
                            jar,
                            dependencies
                    ))
            );
        });
    }

    @Override
    public ResponseEntity<Object> addConstructorCallTransaction(ConstructorCallTransactionRequestModel request) {
        return wrapExceptions(() -> {
        	Node node = getNode();
            SignatureAlgorithm<NonInitialTransactionRequest<?>> signature = node.getSignatureAlgorithmForRequests();
            PrivateKey privateKey = null; // TODO

            StorageReference caller = StorageResolver.resolveStorageReference(request.getCaller().getHash(), request.getCaller().getProgressive());
            ConstructorSignature constructor = new ConstructorSignature(request.getClassType(), StorageResolver.resolveStorageTypes(request.getValues()));
            StorageValue[] actuals = StorageResolver.resolveStorageValues(request.getValues());
            TransactionReference classpath = StorageResolver.resolveTransactionReference(request.getClasspath());

            return okResponseOf(node.addConstructorCallTransaction(new ConstructorCallTransactionRequest(
                    NonInitialTransactionRequest.Signer.with(signature, privateKey),
                    caller,
                    request.getNonce(),
                    request.getChainId(),
                    request.getGasLimit(),
                    request.getGasPrice(),
                    classpath,
                    constructor,
                    actuals
            )));
        });
    }

    @Override
    public ResponseEntity<Object> addInstanceMethodCallTransaction(MethodCallTransactionRequestModel request) {
        return wrapExceptions(() -> {
        	Node node = getNode();
            SignatureAlgorithm<NonInitialTransactionRequest<?>> signature = node.getSignatureAlgorithmForRequests();
            PrivateKey privateKey = null; // TODO

            MethodSignature methodSignature = StorageResolver.resolveMethodSignature(request);
            StorageReference caller = StorageResolver.resolveStorageReference(request.getCaller().getHash(), request.getCaller().getProgressive());
            StorageReference receiver =  StorageResolver.resolveStorageReference(request.getReceiver(), request.getReceiverProgressive());
            StorageValue[] actuals = StorageResolver.resolveStorageValues(request.getValues());
            TransactionReference classpath = StorageResolver.resolveTransactionReference(request.getClasspath());

            return okResponseOf(node.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
                    NonInitialTransactionRequest.Signer.with(signature, privateKey),
                    caller,
                    request.getNonce(),
                    request.getChainId(),
                    request.getGasLimit(),
                    request.getGasPrice(),
                    classpath,
                    methodSignature,
                    receiver,
                    actuals
            )));
        });
    }

    @Override
    public ResponseEntity<Object> addStaticMethodCallTransaction(MethodCallTransactionRequestModel request) {
        return wrapExceptions(() -> {
        	Node node = getNode();
            SignatureAlgorithm<NonInitialTransactionRequest<?>> signature = node.getSignatureAlgorithmForRequests();
            PrivateKey privateKey = null; // TODO

            MethodSignature methodSignature = StorageResolver.resolveMethodSignature(request);
            StorageReference caller = StorageResolver.resolveStorageReference(request.getCaller().getHash(), request.getCaller().getProgressive());
            StorageValue[] actuals = StorageResolver.resolveStorageValues(request.getValues());
            TransactionReference classpath = StorageResolver.resolveTransactionReference(request.getClasspath());

            return okResponseOf(node.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest(
                    NonInitialTransactionRequest.Signer.with(signature, privateKey),
                    caller,
                    request.getNonce(),
                    request.getChainId(),
                    request.getGasLimit(),
                    request.getGasPrice(),
                    classpath,
                    methodSignature,
                    actuals
            )));
        });
    }

}
