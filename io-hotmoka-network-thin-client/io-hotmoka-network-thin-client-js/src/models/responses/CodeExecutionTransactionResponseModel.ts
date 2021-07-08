import {TransactionResponseModel} from "./TransactionResponseModel";
import {UpdateModel} from "../updates/UpdateModel";

export abstract class CodeExecutionTransactionResponseModel extends TransactionResponseModel {
    /**
     * The updates resulting from the execution of the transaction.
     */
    updates: Array<UpdateModel>

    /**
     * The amount of gas consumed by the transaction for CPU execution.
     */
    gasConsumedForCPU: string

    /**
     * The amount of gas consumed by the transaction for RAM allocation.
     */
    gasConsumedForRAM: string

    /**
     * The amount of gas consumed by the transaction for storage consumption.
     */
    gasConsumedForStorage: string

    protected constructor(updates: Array<UpdateModel>,
                          gasConsumedForCPU: string,
                          gasConsumedForRAM: string,
                          gasConsumedForStorage: string
    ) {
        super()
        this.updates = updates
        this.gasConsumedForCPU = gasConsumedForCPU
        this.gasConsumedForRAM = gasConsumedForRAM
        this.gasConsumedForStorage = gasConsumedForStorage
    }
}