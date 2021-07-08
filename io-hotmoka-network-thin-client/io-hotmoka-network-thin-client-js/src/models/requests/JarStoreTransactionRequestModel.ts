import {NonInitialTransactionRequestModel} from "./NonInitialTransactionRequestModel";
import {TransactionReferenceModel} from "../values/TransactionReferenceModel";
import {StorageReferenceModel} from "../values/StorageReferenceModel";
import {Signer} from "../../internal/signature/Signer";
import {Buffer} from "buffer";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";
import {Selectors} from "../../internal/marshalling/Selectors";
import {HotmokaException} from "../../internal/exception/HotmokaException";
import {Signature} from "../../internal/signature/Signature";


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
                signature?: Signature
    ) {
        super(caller, nonce, classpath, gasLimit, gasPrice)

        if (!jar) {
            throw new HotmokaException("invalid jar")
        }

        if (dependencies === null || dependencies === undefined) {
            throw new HotmokaException("dependencies cannot be null or undefined")
        }

        for (const dependency of dependencies) {
            if (dependency === null || dependency === undefined) {
                throw new HotmokaException("dependencies cannot hold null or undefined")
            }
        }

        if (chainId === null || chainId === undefined) {
            throw new HotmokaException("chainId cannot be null or undefined")
        }

        this.jar = jar
        this.dependencies = dependencies
        this.chainId = chainId
        this.signature = signature ? Signer.INSTANCE.sign(signature, this.marshall()) : ''
    }

    public into(context: MarshallingContext): void {
       this.intoWithoutSignature(context)
    }

    protected intoWithoutSignature(context: MarshallingContext): void {
        const jarBuffer = Buffer.from(this.jar, 'base64')

        context.writeByte(Selectors.SELECTOR_JAR_STORE)
        context.writeString(this.chainId)
        super.intoWithoutSignature(context)
        context.writeCompactInt(jarBuffer.length)
        context.writeBuffer(jarBuffer)
        TransactionReferenceModel.intoArray(this.dependencies, context)
    }
}