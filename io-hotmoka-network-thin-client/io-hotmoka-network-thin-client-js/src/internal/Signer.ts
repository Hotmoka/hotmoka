import {Buffer} from "buffer";
import * as crypto from "crypto"
import {KeyObject} from "crypto";

export class Signer {

    /**
     * Signs the data.
     * @param privateKey the private key
     * @param data the data
     * @param algorithm the algorithm. It defaults to SHA256
     */
    public static sign(privateKey: KeyObject, data: Buffer, algorithm?: string): string {
        const sign = crypto.sign(algorithm ? algorithm : "SHA256", data, privateKey);
        const signature = sign.toString('base64')
        return signature
    }

    /**
     * Generates a RSA private key.
     * @return the private key
     */
    public static generatePrivateKey(): KeyObject {
        const { privateKey } = crypto.generateKeyPairSync('rsa', {
            modulusLength: 2048,
        });

        return privateKey
    }
}