import {TransactionReferenceModel} from "../values/TransactionReferenceModel";
import {InitialTransactionRequestModel} from "./InitialTransactionRequestModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";


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
        this.jar = jar
        this.dependencies = dependencies
    }

    protected into(context: MarshallingContext): void {
       // empty
    }
}