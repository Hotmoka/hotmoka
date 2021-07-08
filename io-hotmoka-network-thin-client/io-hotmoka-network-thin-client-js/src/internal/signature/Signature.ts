import {Algorithm} from "./Algorithm";
import {eddsa} from "elliptic";
import {HotmokaException} from "../exception/HotmokaException";
import {Buffer} from "buffer";

export class Signature {
    /**
     * The algorithm of the signatures.
     */
    public readonly algorithm: Algorithm

    /**
     * The private key.
     * @private
     */
    public readonly privateKey: eddsa.KeyPair

    /**
     * It constructs a signature object to sign the requests.
     * @param algorithm the algorithm
     * @param privateKey the raw key encoded in base64 or wrapped in a PEM format
     */
    constructor(algorithm: Algorithm, privateKey: string) {

        if (algorithm === Algorithm.SHA256DSA) {
            throw new HotmokaException("Signature algorithm not implemented")
        } else if (algorithm == Algorithm.ED25519) {
            this.algorithm = algorithm

            if (!privateKey) {
                throw new HotmokaException("Private key not initialized")
            }

            let key = privateKey
            if (Signature.isPemFormat(key)) {
                const splitted = key.split("\n")
                if (splitted.length > 1) {
                    key = splitted[1].trim()
                }
            }

            const ec = new eddsa('ed25519')
            this.privateKey = ec.keyFromSecret(Buffer.from(key, 'base64'))

        } else {
            throw new HotmokaException("Signature algorithm not recognized")
        }
    }

    private static isPemFormat(key: string): boolean {
        return key.trim().startsWith("-----") && key.trim().endsWith("-----")
    }
}

