package io.hotmoka.network.internal.services;

import java.util.Base64;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InitializationTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.network.exception.GenericException;
import io.hotmoka.network.internal.models.function.StorageValueMapper;
import io.hotmoka.network.internal.models.function.TransactionReferenceMapper;
import io.hotmoka.network.internal.models.storage.StorageReferenceModel;
import io.hotmoka.network.internal.models.storage.StorageValueModel;
import io.hotmoka.network.internal.models.transactions.ConstructorCallTransactionRequestModel;
import io.hotmoka.network.internal.models.transactions.GameteCreationTransactionRequestModel;
import io.hotmoka.network.internal.models.transactions.InitializationTransactionRequestModel;
import io.hotmoka.network.internal.models.transactions.JarStoreInitialTransactionRequestModel;
import io.hotmoka.network.internal.models.transactions.JarStoreTransactionRequestModel;
import io.hotmoka.network.internal.models.transactions.MethodCallTransactionRequestModel;
import io.hotmoka.network.internal.models.transactions.RGGameteCreationTransactionRequestModel;
import io.hotmoka.network.internal.models.transactions.TransactionReferenceModel;
import io.hotmoka.network.internal.util.StorageResolver;
import io.hotmoka.network.json.JSONTransactionReference;

@Service
public class NodeAddServiceImpl extends AbstractNetworkService implements NodeAddService {

	@Override
	public TransactionReferenceModel addJarStoreInitialTransaction(JarStoreInitialTransactionRequestModel request) {
		return wrapExceptions(() -> {
		    if (request.getJar() == null)
		        throw new GenericException("Transaction rejected: Jar missing");

            byte[] jar = Base64.getDecoder().decode(request.getJar());
            LocalTransactionReference[] dependencies = StorageResolver.resolveJarDependencies(request.getDependencies());

            return responseOf(
                    getNode().addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(jar, dependencies)),
                    new TransactionReferenceMapper()
            );
		});
	}

    @Override
    public StorageReferenceModel addGameteCreationTransaction(GameteCreationTransactionRequestModel request) {
        return wrapExceptions(() -> new StorageReferenceModel(getNode().addGameteCreationTransaction(request.toBean())));
    }

    @Override
    public StorageReferenceModel addRedGreenGameteCreationTransaction(RGGameteCreationTransactionRequestModel request) {
        return wrapExceptions(() -> new StorageReferenceModel(getNode().addRedGreenGameteCreationTransaction(request.toBean())));
    }

    @Override
    public ResponseEntity<Void> addInitializationTransaction(InitializationTransactionRequestModel request) {
        return wrapExceptions(() -> {
            StorageReference manifest = request.getManifest().toBean();
            TransactionReference classpath = JSONTransactionReference.fromJSON(request.getClasspath());
            getNode().addInitializationTransaction(new InitializationTransactionRequest(classpath, manifest));

            return noContentResponse();
        });
    }

    @Override
    public TransactionReferenceModel addJarStoreTransaction(JarStoreTransactionRequestModel request) {
        return wrapExceptions(() -> {

            byte[] signature = decodeBase64(request.getSignature());
            byte[] jar = decodeBase64(request.getJar());
            StorageReference caller = request.getCaller().toBean();
            LocalTransactionReference[] dependencies = StorageResolver.resolveJarDependencies(request.getDependencies());
            TransactionReference classpath = JSONTransactionReference.fromJSON(request.getClasspath());

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
        return wrapExceptions(() -> new StorageReferenceModel(getNode().addConstructorCallTransaction(request.toBean())));
    }

    @Override
    public StorageValueModel addInstanceMethodCallTransaction(MethodCallTransactionRequestModel request) {
        return wrapExceptions(() -> {
            byte[] signature = decodeBase64(request.getSignature());
            MethodSignature methodSignature = StorageResolver.resolveMethodSignature(request);
            StorageReference caller = request.getCaller().toBean();
            StorageReference receiver = request.getReceiver().toBean();
            StorageValue[] actuals = StorageResolver.resolveStorageValues(request.getValues());
            TransactionReference classpath = JSONTransactionReference.fromJSON(request.getClasspath());

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
        return wrapExceptions(() -> {
            byte[] signature = decodeBase64(request.getSignature());
            MethodSignature methodSignature = StorageResolver.resolveMethodSignature(request);
            StorageReference caller = request.getCaller().toBean();
            StorageValue[] actuals = StorageResolver.resolveStorageValues(request.getValues());
            TransactionReference classpath = JSONTransactionReference.fromJSON(request.getClasspath());

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
