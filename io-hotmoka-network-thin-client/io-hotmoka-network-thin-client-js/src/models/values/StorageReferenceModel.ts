import {TransactionReferenceModel} from "./TransactionReferenceModel";
import {Marshallable} from "../../internal/marshalling/Marshallable";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";
import {Selectors} from "../../internal/marshalling/Selectors";

export class StorageReferenceModel extends Marshallable {
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
        super()
        this.transaction = transaction
        this.progressive = progressive
    }

    public into(context: MarshallingContext): void {
       context.writeByte(Selectors.SELECTOR_STORAGE_REFERENCE)
        this.intoWithoutSelector(context)
    }

    public intoWithoutSelector(context: MarshallingContext): void {
        context.writeStorageReference(this)
    }
}