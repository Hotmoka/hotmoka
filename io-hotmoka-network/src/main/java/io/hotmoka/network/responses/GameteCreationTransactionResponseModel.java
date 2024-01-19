/*
Copyright 2021 Dinu Berinde and Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.network.responses;

import java.util.List;
import java.util.stream.Collectors;

import io.hotmoka.beans.TransactionResponses;
import io.hotmoka.beans.api.responses.GameteCreationTransactionResponse;
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
        this.gamete = new StorageReferenceModel(response.getGamete());
    }

    public GameteCreationTransactionResponseModel() {}

    public GameteCreationTransactionResponse toBean() {
        return TransactionResponses.gameteCreation(updates.stream().map(UpdateModel::toBean), gamete.toBean());
    }
}