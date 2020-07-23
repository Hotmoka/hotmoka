package io.hotmoka.network.internal.services;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.network.internal.models.function.StorageValueMapper;
import io.hotmoka.network.internal.models.storage.StorageValueModel;
import io.hotmoka.network.internal.models.transactions.MethodCallTransactionRequestModel;
import io.hotmoka.network.internal.util.StorageResolver;
import io.hotmoka.network.json.JSONTransactionReference;

import org.springframework.stereotype.Service;

@Service
public class NodeRunServiceImpl extends AbstractNetworkService implements NodeRunService {


    @Override
    public StorageValueModel runInstanceMethodCallTransaction(MethodCallTransactionRequestModel request) {
        return wrapExceptions(() -> {

            byte[] signature = decodeBase64(request.getSignature());
            MethodSignature methodSignature = StorageResolver.resolveMethodSignature(request);
            StorageReference caller = request.getCaller().toBean();
            StorageReference receiver = request.getReceiver().toBean();
            StorageValue[] actuals = StorageResolver.resolveStorageValues(request.getValues());
            TransactionReference classpath = JSONTransactionReference.fromJSON(request.getClasspath());

            return responseOf(
                    getNode().runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
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
    public StorageValueModel runStaticMethodCallTransaction(MethodCallTransactionRequestModel request) {
        return wrapExceptions(() -> {

            byte[] signature = decodeBase64(request.getSignature());
            MethodSignature methodSignature = StorageResolver.resolveMethodSignature(request);
            StorageReference caller = request.getCaller().toBean();
            StorageValue[] actuals = StorageResolver.resolveStorageValues(request.getValues());
            TransactionReference classpath = JSONTransactionReference.fromJSON(request.getClasspath());

            return responseOf(
                    getNode().runStaticMethodCallTransaction(new StaticMethodCallTransactionRequest(
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
