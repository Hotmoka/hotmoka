import {NonInitialTransactionRequestModel} from "./NonInitialTransactionRequestModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";
import {StorageValueModel} from "../values/StorageValueModel";
import {StorageReferenceModel} from "../values/StorageReferenceModel";
import {TransactionReferenceModel} from "../values/TransactionReferenceModel";


export abstract class CodeExecutionTransactionRequestModel extends NonInitialTransactionRequestModel {
    /**
     * The actual arguments passed to the method.
     */
    actuals: Array<StorageValueModel>

    protected constructor(caller: StorageReferenceModel,
                          nonce: string,
                          classpath: TransactionReferenceModel,
                          gasLimit: string,
                          gasPrice: string,
                          actuals: Array<StorageValueModel>) {
        super(caller, nonce, classpath, gasLimit, gasPrice)

        if (actuals == null)
            throw new Error("actuals cannot be null")

        for (let i = 0; i < actuals.length; i++) {
            if (actuals[i] == null) {
                throw new Error("actuals cannot hold null")
            }
        }

        this.actuals = actuals;
    }

    protected intoWithoutSignature(context: MarshallingContext): void {
        super.intoWithoutSignature(context)
        StorageValueModel.intoArray(this.actuals, context)
    }
}