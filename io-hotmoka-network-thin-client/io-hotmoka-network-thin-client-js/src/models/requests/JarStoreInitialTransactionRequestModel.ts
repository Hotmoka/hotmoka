import {TransactionReferenceModel} from "../values/TransactionReferenceModel";
import {InitialTransactionRequestModel} from "./InitialTransactionRequestModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";
import {Selectors} from "../../internal/marshalling/Selectors";
import {Marshallable} from "../../internal/marshalling/Marshallable";
import {Buffer} from "buffer";

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
        const jarBuffer = Buffer.from(this.jar, 'base64')

        context.writeByte(Selectors.SELECTOR_JAR_STORE_INITIAL)
        context.writeInt(jarBuffer.length)
        context.writeBuffer(jarBuffer)
        Marshallable.intoArray(this.dependencies, context)
    }
}