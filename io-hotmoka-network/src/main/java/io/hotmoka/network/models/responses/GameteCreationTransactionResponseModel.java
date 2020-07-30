package io.hotmoka.network.models.responses;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.responses.GameteCreationTransactionResponse;
import io.hotmoka.network.models.updates.UpdateModel;
import io.hotmoka.network.models.values.StorageReferenceModel;

import java.util.List;
import java.util.stream.Collectors;

@Immutable
public class GameteCreationTransactionResponseModel extends TransactionResponseModel {
	
    /**
     * The updates resulting from the execution of the transaction.
     */
    public final List<UpdateModel> updates;

    /**
     * The created gamete.
     */
    public final StorageReferenceModel gamete;


    /**
     * Builds the model from the response
     * @param response the response
     */
    public GameteCreationTransactionResponseModel(GameteCreationTransactionResponse response) {
        this.updates = response.getUpdates().map(UpdateModel::new).collect(Collectors.toList());
        this.gamete = new StorageReferenceModel(response.gamete);
    }

    public GameteCreationTransactionResponse toBean() {
        return new GameteCreationTransactionResponse(updates.stream().map(UpdateModel::toBean), gamete.toBean());
    }
}