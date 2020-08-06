package io.hotmoka.network.models.responses;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.responses.CodeExecutionTransactionResponse;
import io.hotmoka.network.models.updates.UpdateModel;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

@Immutable
public abstract class CodeExecutionTransactionResponseModel extends TransactionResponseModel {

	/**
     * The updates resulting from the execution of the transaction.
     */
    public final List<UpdateModel> updates;

    /**
     * The amount of gas consumed by the transaction for CPU execution.
     */
    public final String gasConsumedForCPU;

    /**
     * The amount of gas consumed by the transaction for RAM allocation.
     */
    public final String gasConsumedForRAM;

    /**
     * The amount of gas consumed by the transaction for storage consumption.
     */
    public final String gasConsumedForStorage;

    public CodeExecutionTransactionResponseModel(CodeExecutionTransactionResponse response) {
        this.updates = response.getUpdates().map(UpdateModel::new).collect(Collectors.toList());
        this.gasConsumedForCPU = response.gasConsumedForCPU.toString();
        this.gasConsumedForRAM = response.gasConsumedForRAM.toString();
        this.gasConsumedForStorage = response.gasConsumedForStorage.toString();
    }
}