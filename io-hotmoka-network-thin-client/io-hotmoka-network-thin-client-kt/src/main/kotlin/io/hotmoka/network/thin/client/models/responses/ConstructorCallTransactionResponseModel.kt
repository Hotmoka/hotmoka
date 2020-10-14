package io.hotmoka.network.thin.client.models.responses

import io.hotmoka.network.thin.client.models.updates.UpdateModel

abstract class ConstructorCallTransactionResponseModel(
        updates: List<UpdateModel>,
        gasConsumedForCPU: String,
        gasConsumedForRAM: String,
        gasConsumedForStorage: String
) : CodeExecutionTransactionResponseModel(
        updates,
        gasConsumedForCPU,
        gasConsumedForRAM,
        gasConsumedForStorage
)