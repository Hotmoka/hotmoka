package io.hotmoka.network.models.responses;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.responses.JarStoreTransactionResponse;
import io.hotmoka.network.models.updates.UpdateModel;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

@Immutable
public abstract class JarStoreTransactionResponseModel extends NonInitialTransactionResponseModel {

    /**
     * The updates resulting from the execution of the transaction.
     */
    public final List<UpdateModel> updates;

    /**
     * The amount of gas consumed by the transaction for CPU execution.
     */
    public final BigInteger gasConsumedForCPU;

    /**
     * The amount of gas consumed by the transaction for RAM allocation.
     */
    public final BigInteger gasConsumedForRAM;

    /**
     * The amount of gas consumed by the transaction for storage consumption.
     */
    public final BigInteger gasConsumedForStorage;


    public JarStoreTransactionResponseModel(JarStoreTransactionResponse response) {
       this.updates = response.getUpdates().map(UpdateModel::new).collect(Collectors.toList());
       this.gasConsumedForCPU = response.gasConsumedForCPU();
       this.gasConsumedForRAM = response.gasConsumedForRAM();
       this.gasConsumedForStorage = response.gasConsumedForStorage();
    }
}
