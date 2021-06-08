import {Buffer} from "buffer";
import * as crypto from "crypto"
import * as fs from "fs";
import * as path from "path"
import {KeyObject} from "crypto";

export class Signer {
    public static privateKey: KeyObject;


    /**
     * Signs the data with a ed25519 key.
     * @param data the data
     * @param privateKey the private key
     * @return the signed data as a base64 string
     */
    public static sign(data: Buffer, privateKey: KeyObject): string {
        return crypto.sign(null, data, privateKey).toString('base64');
    }

    /**
     * It loads the private key from a path. The key must be a valid ed25519 key.
     * @param filePath the path of the private key
     */
    public static loadPrivateKey(filePath: string): void {
        const absolutePath = path.resolve(filePath);
        const privateKey = fs.readFileSync(absolutePath, "utf8");
        Signer.privateKey = crypto.createPrivateKey({
            key: privateKey,
            format: 'pem',
            type: 'pkcs8'
        })
    }
}