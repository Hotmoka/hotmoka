import {Buffer} from "buffer";
import {Signature} from "./Signature";
import {Algorithm} from "./Algorithm";
import {eddsa} from "elliptic"
import {HotmokaException} from "../exception/HotmokaException";

export class Signer {
    public static readonly INSTANCE = new Signer()
    private signature?: Signature
    private privateKey?: eddsa.KeyPair

    private Signer() {
        // private
    }


    /**
     * Signs the data.
     * @param data the data
     * @return the signed data as a base64 string
     */
    public sign(data: Buffer): string {
        if (!this.signature || !this.privateKey) {
            throw new HotmokaException("Private key not loaded")
        }

        if (this.signature.algorithm === Algorithm.ED25519) {
            const signature = (this.privateKey as eddsa.KeyPair).sign(data).toBytes()
            return Buffer.from(signature).toString('base64')
        } else {
            throw new HotmokaException("Signature algorithm not implemented")
        }
    }

    /**
     * It initializes the signature with algorithm and the private key.
     * @param signature the signature object
     */
    public init(signature: Signature): void {

        if (signature.algorithm === Algorithm.SHA256DSA) {
            throw new HotmokaException("Signature algorithm not implemented")
        } else if (signature.algorithm == Algorithm.ED25519) {

            if (!signature.privateKey) {
                throw new HotmokaException("Private key not specified")
            }

            let key = signature.privateKey
            if (Signer.isPemFormat(key)) {
                const splitted = key.split("\n")
                if (splitted.length > 0) {
                    key = splitted[1].trim()
                }
            }

            this.signature = new Signature(signature.algorithm, key)
            const ec = new eddsa('ed25519')
            this.privateKey = ec.keyFromSecret(Buffer.from(key, 'base64'))

        } else {
            throw new HotmokaException("Signature algorithm not recognized")
        }
    }

    private static isPemFormat(key: string): boolean {
        return key.trim().startsWith("-----") && key.trim().endsWith("-----")
    }

    private static wrapToPemFormat(key: string): string {
        let wrapped = "-----BEGIN PRIVATE KEY-----\n"
        wrapped += key
        wrapped += "\n-----END PRIVATE KEY-----"
        return wrapped
    }
}
