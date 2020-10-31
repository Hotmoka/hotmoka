package io.hotmoka.network.thin.client.models.values

class StorageReferenceModel(val transaction: TransactionReferenceModel, val progressive: String) {


    override fun equals(other: Any?): Boolean {
        return other is StorageReferenceModel &&
                other.transaction == transaction && other.progressive == progressive
    }

    override fun hashCode(): Int {
        return progressive.hashCode() xor transaction.hashCode()
    }

}