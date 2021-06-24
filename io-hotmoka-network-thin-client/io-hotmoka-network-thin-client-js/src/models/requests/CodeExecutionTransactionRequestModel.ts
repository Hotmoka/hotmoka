import {NonInitialTransactionRequestModel} from "./NonInitialTransactionRequestModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";
import {StorageValueModel} from "../values/StorageValueModel";
import {StorageReferenceModel} from "../values/StorageReferenceModel";
import {TransactionReferenceModel} from "../values/TransactionReferenceModel";
import {HotmokaException} from "../../internal/exception/HotmokaException";


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

        if (actuals === null || actuals === undefined) {
            throw new HotmokaException("actuals cannot be null or undefined")
        }

        for (const actual of actuals) {
            if (actual === null || actual === undefined) {
                throw new HotmokaException("actuals cannot hold null or undefined")
            }
        }

        this.actuals = actuals;
    }

    protected intoWithoutSignature(context: MarshallingContext): void {
        super.intoWithoutSignature(context)
        StorageValueModel.intoArray(this.actuals, context)
    }
}