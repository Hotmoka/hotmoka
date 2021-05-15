import {JarStoreTransactionResponseModel} from "./JarStoreTransactionResponseModel";
import {TransactionReferenceModel} from "../values/TransactionReferenceModel";
import {UpdateModel} from "../updates/UpdateModel";

export class JarStoreTransactionSuccessfulResponseModel extends JarStoreTransactionResponseModel {
    /**
     * The jar to install, instrumented.
     */
    instrumentedJar: string

    /**
     * The dependencies of the jar, previously installed in blockchain.
     * This is a copy of the same information contained in the request.
     */
    dependencies: Array<TransactionReferenceModel>

    /**
     * The version of the verification tool involved in the verification process.
     */
    verificationToolVersion: number

    constructor(
        updates: Array<UpdateModel>,
        gasConsumedForCPU: string,
        gasConsumedForRAM: string,
        gasConsumedForStorage: string,
        instrumentedJar: string,
        dependencies: Array<TransactionReferenceModel>,
        verificationToolVersion: number
    ) {
        super(updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage)
        this.instrumentedJar = instrumentedJar
        this.dependencies = dependencies
        this.verificationToolVersion = verificationToolVersion
    }
}