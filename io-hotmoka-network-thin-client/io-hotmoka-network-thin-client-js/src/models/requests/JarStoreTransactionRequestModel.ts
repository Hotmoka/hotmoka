import {NonInitialTransactionRequestModel} from "./NonInitialTransactionRequestModel";
import {TransactionReferenceModel} from "../values/TransactionReferenceModel";
import {StorageReferenceModel} from "../values/StorageReferenceModel";
import {Signer} from "../../internal/Signer";
import {Buffer} from "buffer";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";
import {Marshallable} from "../../internal/marshalling/Marshallable";
import {Selectors} from "../../internal/marshalling/Selectors";

/**
 * The model of a jar store transaction request.
 */
export class JarStoreTransactionRequestModel extends NonInitialTransactionRequestModel {
    jar: string
    dependencies: Array<TransactionReferenceModel>
    chainId: string
    signature: string

    constructor(caller: StorageReferenceModel,
                nonce: string,
                classpath: TransactionReferenceModel,
                gasLimit: string,
                gasPrice: string,
                jar: string,
                dependencies: Array<TransactionReferenceModel>,
                chainId: string,
                signature: string) {
        super(caller, nonce, classpath, gasLimit, gasPrice)
        this.jar = jar
        this.dependencies = dependencies
        this.chainId = chainId
        this.signature = ""// Signer.sign(Signer.generatePrivateKey(), this.marshall())
    }

    protected into(context: MarshallingContext): void {
       this.intoWithoutSignature(context)
    }

    protected intoWithoutSignature(context: MarshallingContext): void {
        const jarBuffer = Buffer.from(this.jar, 'base64')

        context.writeByte(Selectors.SELECTOR_JAR_STORE);
        context.writeString(this.chainId);
        super.intoWithoutSignature(context);
        context.writeCompactInt(jarBuffer.length);
        context.writeBuffer(jarBuffer);
        Marshallable.intoArray(this.dependencies, context);
    }
}