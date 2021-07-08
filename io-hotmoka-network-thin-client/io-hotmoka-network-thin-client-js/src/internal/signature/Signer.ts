import {Buffer} from "buffer";
import {Signature} from "./Signature";
import {Algorithm} from "./Algorithm";
import {HotmokaException} from "../exception/HotmokaException";

export class Signer {
    public static readonly INSTANCE = new Signer()

    private constructor() {
        // private
    }

    /**
     * Signs the data.
     * @param signature the signature with which to sign the data
     * @param data the data
     * @return the signed data as a base64 string
     */
    public sign(signature: Signature, data: Buffer): string {

        if (!signature) {
            throw new HotmokaException("Signature not initialized")
        }

        if (!signature.privateKey) {
            throw new HotmokaException("Private key not initialized")
        }

        if (signature.algorithm === Algorithm.ED25519) {
            const signedBytes = signature.privateKey.sign(data).toBytes()
            return Buffer.from(signedBytes).toString('base64')
        } else {
            throw new HotmokaException("Signature algorithm not implemented")
        }
    }
}
