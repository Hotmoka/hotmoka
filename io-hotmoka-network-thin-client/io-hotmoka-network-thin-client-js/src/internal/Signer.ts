import {Buffer} from "buffer";
import * as crypto from "crypto"
import * as fs from "fs";
import * as path from "path"
import {KeyObject} from "crypto";

export class Signer {
    public static privateKey: KeyObject;


    /**
     * Signs the data.
     * @param data the data
     * @param privateKey the private key
     * @return the signed data as a Buffer
     */
    public static sign(data: Buffer, privateKey: KeyObject): Buffer {
        return crypto.sign(null, data, privateKey);
    }

    /**
     * Signs the data and encodes the result into a base64 string.
     * @param data the data
     * @param privateKey the private key
     * @return the signed data as a base64 string
     */
    public static signAndEncodeToBase64(data: Buffer, privateKey: KeyObject): string {
        return Signer.sign(data, privateKey).toString('base64')
    }

    /**
     * It loads the private key from a path.
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