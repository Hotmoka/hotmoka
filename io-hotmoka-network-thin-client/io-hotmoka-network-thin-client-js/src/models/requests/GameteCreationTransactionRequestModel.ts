import {TransactionReferenceModel} from "../values/TransactionReferenceModel";
import {InitialTransactionRequestModel} from "./InitialTransactionRequestModel";
import {HotmokaException} from "../../internal/exception/HotmokaException";


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

        if (classpath === null || classpath === undefined) {
            throw new HotmokaException("classpath cannot be null or undefined")
        }

        if (initialAmount === null || initialAmount === undefined) {
            throw new HotmokaException("initialAmount cannot be null")
        }

        if (Number(initialAmount) < 0) {
            throw new HotmokaException("initialAmount cannot be negative")
        }

        if (redInitialAmount === null || redInitialAmount === undefined) {
            throw new HotmokaException("redInitialAmount cannot be null")
        }

        if (Number(redInitialAmount) < 0) {
            throw new HotmokaException("redInitialAmount cannot be negative")
        }

        if (publicKey === null || publicKey === undefined) {
            throw new HotmokaException("publicKey cannot be null")
        }

        this.initialAmount = initialAmount
        this.redInitialAmount = redInitialAmount
        this.publicKey = publicKey
        this.classpath = classpath
    }
}