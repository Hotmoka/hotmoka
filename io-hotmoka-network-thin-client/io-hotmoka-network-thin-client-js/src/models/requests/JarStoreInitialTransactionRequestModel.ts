import {TransactionReferenceModel} from "../values/TransactionReferenceModel";
import {InitialTransactionRequestModel} from "./InitialTransactionRequestModel";
import {HotmokaException} from "../../internal/HotmokaException";



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
            throw new HotmokaException("jar cannot be null")
        }

        if (!dependencies) {
            throw new HotmokaException("dependencies cannot be null")
        }

        for (let i = 0; i < dependencies.length; i++) {
            if (!dependencies[i])
                throw new HotmokaException("dependencies cannot hold null")
        }

        this.jar = jar
        this.dependencies = dependencies
    }
}