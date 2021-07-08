import {JarStoreTransactionResponseModel} from "./JarStoreTransactionResponseModel";
import {UpdateModel} from "../updates/UpdateModel";

export class JarStoreTransactionFailedResponseModel extends JarStoreTransactionResponseModel {
    /**
     * The amount of gas consumed by the transaction as penalty for the failure.
     */
    gasConsumedForPenalty: string

    /**
     * The fully-qualified class name of the cause exception.
     */
    classNameOfCause: string

    /**
     * The message of the cause exception.
     */
    messageOfCause: string

    constructor(
        updates: Array<UpdateModel>,
        gasConsumedForCPU: string,
        gasConsumedForRAM: string,
        gasConsumedForStorage: string,
        gasConsumedForPenalty: string,
        classNameOfCause: string,
        messageOfCause: string
    ) {
        super(updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage)
        this.gasConsumedForPenalty = gasConsumedForPenalty
        this.classNameOfCause = classNameOfCause
        this.messageOfCause = messageOfCause
    }
}