package io.hotmoka.network.responses;

import java.util.List;
import java.util.stream.Collectors;

import io.hotmoka.beans.responses.GameteCreationTransactionResponse;
import io.hotmoka.network.updates.UpdateModel;
import io.hotmoka.network.values.StorageReferenceModel;

public class GameteCreationTransactionResponseModel extends TransactionResponseModel {
	
    /**
     * The updates resulting from the execution of the transaction.
     */
    public List<UpdateModel> updates;

    /**
     * The created gamete.
     */
    public StorageReferenceModel gamete;

    /**
     * Builds the model from the response
     * @param response the response
     */
    public GameteCreationTransactionResponseModel(GameteCreationTransactionResponse response) {
        this.updates = response.getUpdates().map(UpdateModel::new).collect(Collectors.toList());
        this.gamete = new StorageReferenceModel(response.gamete);
    }

    public GameteCreationTransactionResponseModel() {}

    public GameteCreationTransactionResponse toBean() {
        return new GameteCreationTransactionResponse(updates.stream().map(UpdateModel::toBean), gamete.toBean());
    }
}