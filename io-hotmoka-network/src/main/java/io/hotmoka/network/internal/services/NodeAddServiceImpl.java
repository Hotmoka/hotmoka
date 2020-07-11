package io.hotmoka.network.internal.services;

import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.*;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.network.exception.GenericException;
import io.hotmoka.network.internal.models.function.StorageReferenceMapper;
import io.hotmoka.network.internal.models.function.StorageValueMapper;
import io.hotmoka.network.internal.models.function.TransactionReferenceMapper;
import io.hotmoka.network.internal.models.storage.StorageReferenceModel;
import io.hotmoka.network.internal.models.storage.StorageValueModel;
import io.hotmoka.network.internal.models.transactions.*;
import io.hotmoka.network.internal.util.StorageResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


@Service
public class NodeAddServiceImpl extends NetworkService implements NodeAddService {


	@Override
	public TransactionReferenceModel addJarStoreInitialTransaction(JarStoreInitialTransactionRequestModel request) {

		return wrapExceptions_(() -> {

		    if (request.getJar() == null)
		        throw new GenericException("Transaction rejected: Jar missing");

            byte[] jar = StorageResolver.decodeBase64(request.getJar());
            LocalTransactionReference[] dependencies = StorageResolver.resolveJarDependencies(request.getDependencies());

            return responseOf(
                    getNode().addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(jar, dependencies)),
                    new TransactionReferenceMapper()
            );
		});
	}

    @Override
    public StorageReferenceModel addGameteCreationTransaction(GameteCreationTransactionRequestModel request) {
        return wrapExceptions_(() -> responseOf(
                getNode().addGameteCreationTransaction(new GameteCreationTransactionRequest(
                        StorageResolver.resolveTransactionReference(request.getClasspath()),
                        request.getAmount(),
                        request.getPublicKey())),
                new StorageReferenceMapper()
        ));
    }

    @Override
    public StorageReferenceModel addRedGreenGameteCreationTransaction(RGGameteCreationTransactionRequestModel request) {
        return wrapExceptions_(() -> responseOf(
                getNode().addRedGreenGameteCreationTransaction(new RedGreenGameteCreationTransactionRequest(
                        StorageResolver.resolveTransactionReference(request.getClasspath()),
                        request.getAmount(),
                        request.getRedAmount(),
                        request.getPublicKey())),
                new StorageReferenceMapper()
        ));
    }

    @Override
    public ResponseEntity<Object> addInitializationTransaction(InitializationTransactionRequestModel request) {
        return wrapExceptions(() -> {
            StorageReference manifest = StorageResolver.resolveStorageReference(request.getManifest());
            TransactionReference classpath = StorageResolver.resolveTransactionReference(request.getClasspath());
            getNode().addInitializationTransaction(new InitializationTransactionRequest(classpath, manifest));

            return noContentResponse();
        });
    }

    @Override
    public TransactionReferenceModel addJarStoreTransaction(JarStoreTransactionRequestModel request) {
        return wrapExceptions_(() -> {

            byte[] signature = StorageResolver.decodeBase64(request.getSignature());
            byte[] jar = StorageResolver.decodeBase64(request.getJar());
            StorageReference caller = StorageResolver.resolveStorageReference(request.getCaller());
            LocalTransactionReference[] dependencies = StorageResolver.resolveJarDependencies(request.getDependencies());
            TransactionReference classpath = StorageResolver.resolveTransactionReference(request.getClasspath());

            return responseOf(
                    getNode().addJarStoreTransaction(new JarStoreTransactionRequest(
                            signature,
                            caller,
                            request.getNonce(),
                            request.getChainId(),
                            request.getGasLimit(),
                            request.getGasPrice(),
                            classpath,
                            jar,
                            dependencies)),
                    new TransactionReferenceMapper()
            );
        });
    }

    @Override
    public StorageReferenceModel addConstructorCallTransaction(ConstructorCallTransactionRequestModel request) {
        return wrapExceptions_(() -> {

            byte[] signature = StorageResolver.decodeBase64(request.getSignature());
            StorageReference caller = StorageResolver.resolveStorageReference(request.getCaller());
            ConstructorSignature constructor = new ConstructorSignature(request.getConstructorType(), StorageResolver.resolveStorageTypes(request.getValues()));
            StorageValue[] actuals = StorageResolver.resolveStorageValues(request.getValues());
            TransactionReference classpath = StorageResolver.resolveTransactionReference(request.getClasspath());

            return responseOf(
                    getNode().addConstructorCallTransaction(new ConstructorCallTransactionRequest(
                            signature,
                            caller,
                            request.getNonce(),
                            request.getChainId(),
                            request.getGasLimit(),
                            request.getGasPrice(),
                            classpath,
                            constructor,
                            actuals)),
                    new StorageReferenceMapper()
            );
        });
    }

    @Override
    public StorageValueModel addInstanceMethodCallTransaction(MethodCallTransactionRequestModel request) {
        return wrapExceptions_(() -> {

            byte[] signature = StorageResolver.decodeBase64(request.getSignature());
            MethodSignature methodSignature = StorageResolver.resolveMethodSignature(request);
            StorageReference caller = StorageResolver.resolveStorageReference(request.getCaller());
            StorageReference receiver = StorageResolver.resolveStorageReference(request.getReceiver());
            StorageValue[] actuals = StorageResolver.resolveStorageValues(request.getValues());
            TransactionReference classpath = StorageResolver.resolveTransactionReference(request.getClasspath());

            return responseOf(
                    getNode().addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
                            signature,
                            caller,
                            request.getNonce(),
                            request.getChainId(),
                            request.getGasLimit(),
                            request.getGasPrice(),
                            classpath,
                            methodSignature,
                            receiver,
                            actuals)),
                    new StorageValueMapper()
            );
        });
    }

    @Override
    public StorageValueModel addStaticMethodCallTransaction(MethodCallTransactionRequestModel request) {
        return wrapExceptions_(() -> {

            byte[] signature = StorageResolver.decodeBase64(request.getSignature());
            MethodSignature methodSignature = StorageResolver.resolveMethodSignature(request);
            StorageReference caller = StorageResolver.resolveStorageReference(request.getCaller());
            StorageValue[] actuals = StorageResolver.resolveStorageValues(request.getValues());
            TransactionReference classpath = StorageResolver.resolveTransactionReference(request.getClasspath());

            return responseOf(
                    getNode().addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest(
                            signature,
                            caller,
                            request.getNonce(),
                            request.getChainId(),
                            request.getGasLimit(),
                            request.getGasPrice(),
                            classpath,
                            methodSignature,
                            actuals)),
                    new StorageValueMapper()
            );
        });
    }

}
