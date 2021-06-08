import {Buffer} from "buffer";
import * as crypto from "crypto"
import * as fs from "fs";
import * as path from "path"
import {KeyObject} from "crypto";
import {PrivateKey} from "./PrivateKey";

export class Signer {
    private static privateKey: KeyObject;


    /**
     * Signs the data with a ed25519 key.
     * @param data the data
     * @return the signed data as a base64 string
     */
    public static sign(data: Buffer): string {
        if (Signer.privateKey === null || Signer.privateKey === undefined) {
            throw new Error("Private key not loaded")
        }
        return crypto.sign(null, data, Signer.privateKey).toString('base64');
    }

    /**
     * It loads the private key. The key must be a valid ed25519 key and it must be in a 'pem' format.
     * @param privateKey the private key object
     */
    public static loadPrivateKey(privateKey: PrivateKey): void {

        if (privateKey.filePath) {
            const absolutePath = path.resolve(privateKey.filePath);
            const key = fs.readFileSync(absolutePath, "utf8");
            Signer.privateKey = crypto.createPrivateKey({
                key: key,
                format: 'pem',
                type: 'pkcs8'
            })
        } else if (privateKey.privateKey) {
            Signer.privateKey = crypto.createPrivateKey({
                key: privateKey.privateKey,
                format: 'pem',
                type: 'pkcs8'
            })
        } else {
            throw new Error("Private key not specified")
        }
    }
}
