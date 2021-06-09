import {TransactionReferenceModel} from "./TransactionReferenceModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";
import {Selectors} from "../../internal/marshalling/Selectors";

export class StorageReferenceModel {
    /**
     * The transaction that created the object.
     */
    transaction: TransactionReferenceModel

    /**
     * The progressive number of the object among those that have been created
     * during the same transaction.
     */
    progressive: string

    constructor(transaction: TransactionReferenceModel, progressive: string) {
        this.transaction = transaction
        this.progressive = progressive
    }

    public static into(context: MarshallingContext, storageReferenceModel: StorageReferenceModel): void {
        context.writeByte(Selectors.SELECTOR_STORAGE_REFERENCE)
        this.intoWithoutSelector(context, storageReferenceModel)
    }

    public static intoWithoutSelector(context: MarshallingContext, storageReferenceModel: StorageReferenceModel): void {
        context.writeStorageReference(storageReferenceModel)
    }
}