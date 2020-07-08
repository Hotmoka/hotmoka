package io.hotmoka.network.internal.services;

import java.security.PrivateKey;
import java.util.Base64;

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
import io.hotmoka.network.internal.models.transactions.ConstructorCallTransactionRequestModel;
import io.hotmoka.network.internal.models.transactions.GameteCreationTransactionRequestModel;
import io.hotmoka.network.internal.models.transactions.JarStoreInitialTransactionRequestModel;
import io.hotmoka.network.internal.models.transactions.JarStoreTransactionRequestModel;
import io.hotmoka.network.internal.models.transactions.MethodCallTransactionRequestModel;
import io.hotmoka.network.internal.models.transactions.RGGameteCreationTransactionRequestModel;
import io.hotmoka.network.internal.util.StorageResolver;
import io.hotmoka.nodes.Node;


@Service
public class NodeAddServiceImpl extends NetworkService implements NodeAddService {


	@Override
	public ResponseEntity<Object> addJarStoreInitialTransaction(JarStoreInitialTransactionRequestModel request) {
		if (request.getJar() == null)
			return badRequestResponseOf(new Error("Transaction rejected: Jar missing"));

		byte[] jar = Base64.getDecoder().decode(request.getJar());
		return wrapExceptions(() -> {
			Node node = getNode();
			return okResponseOf(node.addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(jar, node.getTakamakaCode()))); // TODO
		});
	}

    @Override
    public ResponseEntity<Object> addGameteCreationTransaction(GameteCreationTransactionRequestModel request) {
        return wrapExceptions(() -> okResponseOf(getNode().addGameteCreationTransaction(new GameteCreationTransactionRequest(
                        getNode().getTakamakaCode(), // TODO
                        request.getAmount(),
                        request.getPublicKey()
                ))
        ));
    }

    @Override
    public ResponseEntity<Object> addRedGreenGameteCreationTransaction(RGGameteCreationTransactionRequestModel request) {
        return wrapExceptions(() -> okResponseOf(getNode().addRedGreenGameteCreationTransaction(new RedGreenGameteCreationTransactionRequest(
                    getNode().getTakamakaCode(), // TODO
                    request.getAmount(),
                    request.getRedAmount(),
                    request.getPublicKey()
                ))
        ));
    }

    @Override
    public ResponseEntity<Object> addInitializationTransaction(StorageModel request) {
        return wrapExceptions(() -> {
            StorageReference manifest = StorageResolver.resolveStorageReference(request.getHash(), request.getProgressive());
            getNode().addInitializationTransaction(new InitializationTransactionRequest(getNode().getTakamakaCode(), manifest)); // TODO
            return noContentResponse();
        });
    }

    @Override
    public ResponseEntity<Object> addJarStoreTransaction(JarStoreTransactionRequestModel request) {
        return wrapExceptions(() -> {
            SignatureAlgorithm<NonInitialTransactionRequest<?>> signature = getNode().getSignatureAlgorithmForRequests();
            PrivateKey privateKey = null; // TODO

            byte[] jar = Base64.getDecoder().decode(request.getJar());
            StorageReference caller = StorageResolver.resolveStorageReference(request.getCaller(), request.getCallerProgressive());
            LocalTransactionReference[] dependencies = StorageResolver.resolveJarDependencies(request.getDependencies());

            return okResponseOf(getNode().addJarStoreTransaction(new JarStoreTransactionRequest(
                            NonInitialTransactionRequest.Signer.with(signature, privateKey),
                            caller,
                            request.getNonce(),
                            request.getChainId(),
                            request.getGasLimit(),
                            request.getGasPrice(),
                            getNode().getTakamakaCode(), // TODO
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
        return wrapExceptions(() -> {
        	Node node = getNode();
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
        return wrapExceptions(() -> {
        	Node node = getNode();
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
