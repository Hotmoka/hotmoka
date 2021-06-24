import {TransactionReferenceModel} from "../values/TransactionReferenceModel";
import {InitialTransactionRequestModel} from "./InitialTransactionRequestModel";
import {HotmokaException} from "../../internal/exception/HotmokaException";



/**
 * The model of an initial jar store transaction request.
 */
export class JarStoreInitialTransactionRequestModel extends InitialTransactionRequestModel {
    /**
     * The jar to install.
     */
    jar: string

    /**
     * The dependencies of the jar, already installed in blockchain
     */
    dependencies: Array<TransactionReferenceModel>

    constructor(jar: string,
                dependencies: Array<TransactionReferenceModel>) {
        super()

        if (!jar) {
            throw new HotmokaException("invalid jar")
        }

        if (dependencies === null || dependencies === undefined) {
            throw new HotmokaException("dependencies cannot be null or undefined")
        }

        for (const dependency of dependencies) {
            if (dependency === null || dependency === undefined) {
                throw new HotmokaException("dependencies cannot hold null or undefined")
            }
        }

        this.jar = jar
        this.dependencies = dependencies
    }
}