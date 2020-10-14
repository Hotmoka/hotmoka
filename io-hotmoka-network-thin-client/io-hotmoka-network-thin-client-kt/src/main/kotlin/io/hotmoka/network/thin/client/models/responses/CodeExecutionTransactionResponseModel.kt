package io.hotmoka.network.thin.client.models.responses

import io.hotmoka.network.thin.client.models.updates.UpdateModel

abstract class CodeExecutionTransactionResponseModel(
        val updates: List<UpdateModel>,
        val gasConsumedForCPU: String,
        val gasConsumedForRAM: String,
        val gasConsumedForStorage: String
)