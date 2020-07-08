package io.hotmoka.network.internal.services;

import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.*;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.network.internal.models.Error;
import io.hotmoka.network.internal.models.transactions.*;
import io.hotmoka.network.internal.util.StorageResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


@Service
public class NodeAddServiceImpl extends NetworkService implements NodeAddService {


	@Override
	public ResponseEntity<Object> addJarStoreInitialTransaction(JarStoreInitialTransactionRequestModel request) {
		if (request.getJar() == null)
			return badRequestResponseOf(new Error("Transaction rejected: Jar missing"));

		return wrapExceptions(() -> {
            byte[] jar = StorageResolver.decodeBase64(request.getJar());
            LocalTransactionReference[] dependencies = StorageResolver.resolveJarDependencies(request.getDependencies());

			return okResponseOf(getNode().addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(jar, dependencies)));
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
            TransactionReference classpath = StorageResolver.resolveTransactionReference(request.getClasspath());
            getNode().addInitializationTransaction(new InitializationTransactionRequest(classpath, manifest));

            return noContentResponse();
        });
    }

    @Override
    public ResponseEntity<Object> addJarStoreTransaction(JarStoreTransactionRequestModel request) {
        return wrapExceptions(() -> {

            byte[] signature = StorageResolver.decodeBase64(request.getSignature());
            byte[] jar = StorageResolver.decodeBase64(request.getJar());
            StorageReference caller = StorageResolver.resolveStorageReference(request.getCaller().getHash(), request.getCaller().getProgressive());
            LocalTransactionReference[] dependencies = StorageResolver.resolveJarDependencies(request.getDependencies());
            TransactionReference classpath = StorageResolver.resolveTransactionReference(request.getClasspath());

            return okResponseOf(getNode().addJarStoreTransaction(new JarStoreTransactionRequest(
                            signature,
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

            byte[] signature = StorageResolver.decodeBase64(request.getSignature());
            StorageReference caller = StorageResolver.resolveStorageReference(request.getCaller().getHash(), request.getCaller().getProgressive());
            ConstructorSignature constructor = new ConstructorSignature(request.getClassType(), StorageResolver.resolveStorageTypes(request.getValues()));
            StorageValue[] actuals = StorageResolver.resolveStorageValues(request.getValues());
            TransactionReference classpath = StorageResolver.resolveTransactionReference(request.getClasspath());

            return okResponseOf(getNode().addConstructorCallTransaction(new ConstructorCallTransactionRequest(
                    signature,
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

            byte[] signature = StorageResolver.decodeBase64(request.getSignature());
        	MethodSignature methodSignature = StorageResolver.resolveMethodSignature(request);
            StorageReference caller = StorageResolver.resolveStorageReference(request.getCaller().getHash(), request.getCaller().getProgressive());
            StorageReference receiver =  StorageResolver.resolveStorageReference(request.getReceiver(), request.getReceiverProgressive());
            StorageValue[] actuals = StorageResolver.resolveStorageValues(request.getValues());
            TransactionReference classpath = StorageResolver.resolveTransactionReference(request.getClasspath());

            return okResponseOf(getNode().addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
                    signature,
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

            byte[] signature = StorageResolver.decodeBase64(request.getSignature());
            MethodSignature methodSignature = StorageResolver.resolveMethodSignature(request);
            StorageReference caller = StorageResolver.resolveStorageReference(request.getCaller().getHash(), request.getCaller().getProgressive());
            StorageValue[] actuals = StorageResolver.resolveStorageValues(request.getValues());
            TransactionReference classpath = StorageResolver.resolveTransactionReference(request.getClasspath());

            return okResponseOf(getNode().addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest(
                    signature,
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
