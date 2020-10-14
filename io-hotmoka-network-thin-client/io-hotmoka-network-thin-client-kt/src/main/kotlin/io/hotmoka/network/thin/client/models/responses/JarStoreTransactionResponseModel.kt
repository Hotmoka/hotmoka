package io.hotmoka.network.thin.client.models.responses

import io.hotmoka.network.thin.client.models.updates.UpdateModel

abstract class JarStoreTransactionResponseModel(
        val update: List<UpdateModel>,
        val gasConsumedForCPU: String,
        val gasConsumedForRAM: String,
        val gasConsumedForStorage: String
)