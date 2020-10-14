package io.hotmoka.network.thin.client.models.requests

class TransactionRestRequestModel<T>(val type: String, val transactionResponseModel: T)