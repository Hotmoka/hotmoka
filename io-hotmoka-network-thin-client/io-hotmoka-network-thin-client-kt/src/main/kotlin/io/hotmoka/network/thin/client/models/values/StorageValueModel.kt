package io.hotmoka.network.thin.client.models.values

class StorageValueModel(
        val value: String,
        val reference: StorageReferenceModel,
        val type: String,
        val enumElementName: String
)