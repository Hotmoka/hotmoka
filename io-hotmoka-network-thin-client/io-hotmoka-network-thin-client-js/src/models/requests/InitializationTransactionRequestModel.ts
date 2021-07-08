import {StorageReferenceModel} from "../values/StorageReferenceModel";
import {TransactionReferenceModel} from "../values/TransactionReferenceModel";
import {InitialTransactionRequestModel} from "./InitialTransactionRequestModel";
import {HotmokaException} from "../../internal/exception/HotmokaException";


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

        if (classpath === null || classpath === undefined) {
            throw new HotmokaException("classpath cannot be null or undefined")
        }

        if (manifest === null || manifest === undefined) {
            throw new HotmokaException("manifest cannot be null or undefined")
        }

        this.manifest = manifest
        this.classpath = classpath
    }
}