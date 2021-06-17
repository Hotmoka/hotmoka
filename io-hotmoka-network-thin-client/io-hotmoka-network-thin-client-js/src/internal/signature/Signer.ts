import {Buffer} from "buffer";
import * as crypto from "crypto"
import {KeyObject} from "crypto"
import {Signature} from "./Signature";
import {Algorithm} from "./Algorithm";
import {eddsa} from "elliptic"

export class Signer {
    public static readonly INSTANCE = new Signer()
    private signature: Signature | null = null
    private privateKey: KeyObject | eddsa.KeyPair | null = null

    private Signer() {
        // private
    }


    /**
     * Signs the data.
     * @param data the data
     * @return the signed data as a base64 string
     */
    public sign(data: Buffer): string {
        if (this.signature === null || this.privateKey === null) {
            throw new Error("Private key not loaded")
        }

        if (this.signature.algorithm === Algorithm.ED25519) {
            const signature = (this.privateKey as eddsa.KeyPair).sign(data).toBytes()
            return Buffer.from(signature).toString('base64')
        } else {
            return crypto.sign( "sha256", data, this.privateKey as KeyObject).toString('base64')
        }
    }

    /**
     * It initializes the signature with algorithm and the private key.
     * @param signature the signature object
     */
    public init(signature: Signature): void {

        if (signature.algorithm === Algorithm.SHA256DSA) {

            if (!signature.privateKey) {
                throw new Error("Private key not specified")
            }

            let key = signature.privateKey
            if (!Signer.isPemFormat(key)) {
                key = Signer.wrapToPemFormat(key)
            }

            this.signature = new Signature(signature.algorithm, key)
            this.privateKey = crypto.createPrivateKey({
                key: key
            })
        } else if (signature.algorithm == Algorithm.ED25519) {

            if (!signature.privateKey) {
                throw new Error("Private key not specified")
            }

            let key = signature.privateKey
            if (Signer.isPemFormat(key)) {
                key = key.replace("-----BEGIN PRIVATE KEY-----", "")
                key = key.replace("-----END PRIVATE KEY-----", "").trim()
            }

            this.signature = new Signature(signature.algorithm, key)
            const ec = new eddsa('ed25519')
            this.privateKey = ec.keyFromSecret(Buffer.from(key, 'base64'))

        } else {
            throw new Error("algorithm not recognized")
        }
    }

    private static isPemFormat(key: string): boolean {
        return key.trim().startsWith("-----BEGIN PRIVATE KEY-----") && key.trim().endsWith("-----END PRIVATE KEY-----")
    }

    private static wrapToPemFormat(key: string): string {
        let wrapped = "-----BEGIN PRIVATE KEY-----\n"
        wrapped += key
        wrapped += "\n-----END PRIVATE KEY-----"
        return wrapped
    }
}
