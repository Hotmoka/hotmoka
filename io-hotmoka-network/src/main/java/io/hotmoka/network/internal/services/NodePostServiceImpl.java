package io.hotmoka.network.internal.services;

import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.network.internal.models.function.StorageReferenceMapper;
import io.hotmoka.network.internal.models.function.StorageValueMapper;
import io.hotmoka.network.internal.models.function.TransactionReferenceMapper;
import io.hotmoka.network.internal.models.storage.StorageReferenceModel;
import io.hotmoka.network.internal.models.storage.StorageValueModel;
import io.hotmoka.network.internal.models.transactions.ConstructorCallTransactionRequestModel;
import io.hotmoka.network.internal.models.transactions.JarStoreTransactionRequestModel;
import io.hotmoka.network.internal.models.transactions.MethodCallTransactionRequestModel;
import io.hotmoka.network.internal.models.transactions.TransactionReferenceModel;
import io.hotmoka.network.internal.util.StorageResolver;
import io.hotmoka.network.json.JSONTransactionReference;

import org.springframework.stereotype.Service;

@Service
public class NodePostServiceImpl extends NetworkService implements NodePostService {


    @Override
    public TransactionReferenceModel postJarStoreTransaction(JarStoreTransactionRequestModel request) {
        return wrapExceptions(() -> {

            byte[] signature = StorageResolver.decodeBase64(request.getSignature());
            byte[] jar = StorageResolver.decodeBase64(request.getJar());
            StorageReference caller = StorageResolver.resolveStorageReference(request.getCaller());
            LocalTransactionReference[] dependencies = StorageResolver.resolveJarDependencies(request.getDependencies());
            TransactionReference classpath = JSONTransactionReference.fromJSON(request.getClasspath());

            return responseOf(
                    getNode().postJarStoreTransaction(new JarStoreTransactionRequest(
                            signature,
                            caller,
                            request.getNonce(),
                            request.getChainId(),
                            request.getGasLimit(),
                            request.getGasPrice(),
                            classpath,
                            jar,
                            dependencies)).get(),
                    new TransactionReferenceMapper()
            );
        });
    }

    @Override
    public StorageReferenceModel postConstructorCallTransaction(ConstructorCallTransactionRequestModel request) {
        return wrapExceptions(() -> {

            byte[] signature = StorageResolver.decodeBase64(request.getSignature());
            StorageReference caller = StorageResolver.resolveStorageReference(request.getCaller());
            ConstructorSignature constructor = new ConstructorSignature(request.getConstructorType(), StorageResolver.resolveStorageTypes(request.getValues()));
            StorageValue[] actuals = StorageResolver.resolveStorageValues(request.getValues());
            TransactionReference classpath = JSONTransactionReference.fromJSON(request.getClasspath());

            return responseOf(
                    getNode().postConstructorCallTransaction(new ConstructorCallTransactionRequest(
                            signature,
                            caller,
                            request.getNonce(),
                            request.getChainId(),
                            request.getGasLimit(),
                            request.getGasPrice(),
                            classpath,
                            constructor,
                            actuals)).get(),
                    new StorageReferenceMapper()
            );
        });
    }

    @Override
    public StorageValueModel postInstanceMethodCallTransaction(MethodCallTransactionRequestModel request) {
        return wrapExceptions(() -> {

            byte[] signature = StorageResolver.decodeBase64(request.getSignature());
            MethodSignature methodSignature = StorageResolver.resolveMethodSignature(request);
            StorageReference caller = StorageResolver.resolveStorageReference(request.getCaller());
            StorageReference receiver = StorageResolver.resolveStorageReference(request.getReceiver());
            StorageValue[] actuals = StorageResolver.resolveStorageValues(request.getValues());
            TransactionReference classpath = JSONTransactionReference.fromJSON(request.getClasspath());

            return responseOf(
                    getNode().postInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
                            signature,
                            caller,
                            request.getNonce(),
                            request.getChainId(),
                            request.getGasLimit(),
                            request.getGasPrice(),
                            classpath,
                            methodSignature,
                            receiver,
                            actuals)).get(),
                    new StorageValueMapper()
            );
        });
    }

    @Override
    public StorageValueModel postStaticMethodCallTransaction(MethodCallTransactionRequestModel request) {
        return wrapExceptions(() -> {

            byte[] signature = StorageResolver.decodeBase64(request.getSignature());
            MethodSignature methodSignature = StorageResolver.resolveMethodSignature(request);
            StorageReference caller = StorageResolver.resolveStorageReference(request.getCaller());
            StorageValue[] actuals = StorageResolver.resolveStorageValues(request.getValues());
            TransactionReference classpath = JSONTransactionReference.fromJSON(request.getClasspath());

            return responseOf(
                    getNode().postStaticMethodCallTransaction(new StaticMethodCallTransactionRequest(
                            signature,
                            caller,
                            request.getNonce(),
                            request.getChainId(),
                            request.getGasLimit(),
                            request.getGasPrice(),
                            classpath,
                            methodSignature,
                            actuals)).get(),
                    new StorageValueMapper()
            );
        });
    }
}
