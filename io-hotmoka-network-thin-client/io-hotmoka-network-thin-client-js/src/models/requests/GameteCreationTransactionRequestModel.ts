import {TransactionReferenceModel} from "../values/TransactionReferenceModel";
import {InitialTransactionRequestModel} from "./InitialTransactionRequestModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";
import {Selectors} from "../../internal/marshalling/Selectors";

export class GameteCreationTransactionRequestModel extends InitialTransactionRequestModel {
    /**
     * The amount of coin provided to the gamete.
     */
    initialAmount: string

    /**
     * The amount of red coin provided to the gamete.
     */
    redInitialAmount: string;

    /**
     * The Base64-encoded public key that will be assigned to the gamete.
     */
    publicKey: string;

    /**
     * The reference to the jar containing the basic Takamaka classes. This must
     * have been already installed by a previous transaction.
     */
    classpath: TransactionReferenceModel

    constructor(initialAmount: string,
                redInitialAmount: string,
                publicKey: string,
                classpath: TransactionReferenceModel) {
        super()
        this.initialAmount = initialAmount
        this.redInitialAmount = redInitialAmount
        this.publicKey = publicKey
        this.classpath = classpath
    }

    protected into(context: MarshallingContext): void {
        context.writeByte(Selectors.SELECTOR_GAMETE_CREATION)
        this.classpath.into(context)
        context.writeBigInteger(Number(this.initialAmount))
        context.writeBigInteger(Number(this.redInitialAmount))
        context.writeString(this.publicKey)
    }
}