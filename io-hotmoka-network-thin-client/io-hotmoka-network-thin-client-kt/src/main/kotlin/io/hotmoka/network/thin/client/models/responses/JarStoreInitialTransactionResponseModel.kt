package io.hotmoka.network.thin.client.models.responses

class JarStoreInitialTransactionResponseModel(
        /**
         * The jar to install, instrumented.
         */
        val instrumentedJar: String,
        /**
         * The dependencies of the jar, previously installed in blockchain.
         * This is a copy of the same information contained in the request.
         */
        val dependencies: List<TransactionResponseModel>
) : TransactionResponseModel()