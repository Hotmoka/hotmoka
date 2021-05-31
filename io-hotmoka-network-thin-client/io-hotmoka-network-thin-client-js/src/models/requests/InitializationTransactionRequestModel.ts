import {StorageReferenceModel} from "../values/StorageReferenceModel";
import {TransactionReferenceModel} from "../values/TransactionReferenceModel";
import {InitialTransactionRequestModel} from "./InitialTransactionRequestModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";
import {Selectors} from "../../internal/marshalling/Selectors";

export class InitializationTransactionRequestModel extends InitialTransactionRequestModel {
    /**
     * The reference to the jar containing the basic Takamaka classes. This must
     * have been already installed by a previous transaction.
     */
    manifest: StorageReferenceModel

    /**
     * The storage reference that must be set as manifest.
     */
    classpath: TransactionReferenceModel

    constructor(manifest: StorageReferenceModel, classpath: TransactionReferenceModel) {
        super()
        this.manifest = manifest
        this.classpath = classpath
    }

    protected into(context: MarshallingContext): void {
        context.writeByte(Selectors.SELECTOR_INITIALIZATION_TRANSACTION)
        this.classpath.into(context)
        this.manifest.intoWithoutSelector(context)
    }
}