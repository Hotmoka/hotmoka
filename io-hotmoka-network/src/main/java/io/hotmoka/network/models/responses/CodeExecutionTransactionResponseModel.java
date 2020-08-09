package io.hotmoka.network.models.responses;

import java.util.List;
import java.util.stream.Collectors;

import io.hotmoka.beans.responses.CodeExecutionTransactionResponse;
import io.hotmoka.network.models.updates.UpdateModel;

public abstract class CodeExecutionTransactionResponseModel extends TransactionResponseModel {

	/**
     * The updates resulting from the execution of the transaction.
     */
    public List<UpdateModel> updates;

    /**
     * The amount of gas consumed by the transaction for CPU execution.
     */
    public String gasConsumedForCPU;

    /**
     * The amount of gas consumed by the transaction for RAM allocation.
     */
    public String gasConsumedForRAM;

    /**
     * The amount of gas consumed by the transaction for storage consumption.
     */
    public String gasConsumedForStorage;

    protected CodeExecutionTransactionResponseModel(CodeExecutionTransactionResponse response) {
        this.updates = response.getUpdates().map(UpdateModel::new).collect(Collectors.toList());
        this.gasConsumedForCPU = response.gasConsumedForCPU.toString();
        this.gasConsumedForRAM = response.gasConsumedForRAM.toString();
        this.gasConsumedForStorage = response.gasConsumedForStorage.toString();
    }

    protected CodeExecutionTransactionResponseModel() {}
}