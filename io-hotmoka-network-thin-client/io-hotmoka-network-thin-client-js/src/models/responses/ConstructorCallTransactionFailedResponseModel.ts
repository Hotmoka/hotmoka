import {ConstructorCallTransactionResponseModel} from "./ConstructorCallTransactionResponseModel";
import {UpdateModel} from "../updates/UpdateModel";

export class ConstructorCallTransactionFailedResponseModel extends ConstructorCallTransactionResponseModel {

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

    /**
     * The program point where the cause exception occurred.
     */
    where: string

    constructor(
        updates: Array<UpdateModel>,
        gasConsumedForCPU: string,
        gasConsumedForRAM: string,
        gasConsumedForStorage: string,
        gasConsumedForPenalty: string,
        classNameOfCause: string,
        messageOfCause: string,
        where: string
    ) {
        super(updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage)
        this.gasConsumedForPenalty = gasConsumedForPenalty
        this.classNameOfCause = classNameOfCause
        this.messageOfCause = messageOfCause
        this.where = where
    }
}