import {TransactionReferenceModel} from "./TransactionReferenceModel";
import {Marshallable} from "../../internal/marshalling/Marshallable";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";

export class StorageReferenceModel extends Marshallable{
    transaction: TransactionReferenceModel
    progressive: string

    constructor(transaction: TransactionReferenceModel, progressive: string) {
        super()
        this.transaction = transaction
        this.progressive = progressive
    }

    into(context: MarshallingContext): void {
        // TODO
    }

    intoWithoutSelector(context: MarshallingContext): void {
        // TODO
    }
}