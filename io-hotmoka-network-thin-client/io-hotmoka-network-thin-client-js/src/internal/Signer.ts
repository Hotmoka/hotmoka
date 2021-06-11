import {Buffer} from "buffer";
import * as crypto from "crypto"
import {KeyObject} from "crypto"
import * as fs from "fs";
import * as path from "path"
import {Algorithm, PrivateKey} from "./PrivateKey";
import {eddsa} from "elliptic"

export class Signer {
    public static readonly INSTANCE = new Signer()
    private privateKey: PrivateKey | null = null
    private privateKeyObject: KeyObject | eddsa.KeyPair | null = null

    private Signer() {
        // private
    }


    /**
     * Signs the data.
     * @param data the data
     * @return the signed data as a base64 string
     */
    public sign(data: Buffer): string {
        if (this.privateKey === null || this.privateKeyObject === null) {
            throw new Error("Private key not loaded")
        }

        if (this.privateKey.algorithm === Algorithm.ED25519) {
            const signature = (this.privateKeyObject as eddsa.KeyPair).sign(data).toBytes()
            return Buffer.from(signature).toString('base64')
        } else {
            return crypto.sign( "sha256", data, this.privateKeyObject as KeyObject).toString('base64')
        }
    }

    /**
     * It initializes the private key.
     * @param privateKey the private key object
     */
    public init(privateKey: PrivateKey): void {
        this.privateKey = privateKey

        if (this.privateKey.algorithm === Algorithm.SHA256DSA) {
            let key: string | Buffer

            if (privateKey.filePath) {
                const absolutePath = path.resolve(privateKey.filePath);
                key = fs.readFileSync(absolutePath, "utf8");
            } else if (privateKey.privateKey) {
                key = privateKey.privateKey
            } else {
                throw new Error("Private key not specified")
            }

            this.privateKeyObject = crypto.createPrivateKey({
                key: key
            })
        } else if (this.privateKey.algorithm == Algorithm.ED25519) {
            let key: string | Buffer

            if (privateKey.filePath) {
                const absolutePath = path.resolve(privateKey.filePath)
                key = fs.readFileSync(absolutePath, "utf8")
                key = key.replace("-----BEGIN PRIVATE KEY-----", "")
                key = key.replace("-----END PRIVATE KEY-----", "").trim()
            } else if (privateKey.privateKey) {
                key = privateKey.privateKey
            } else {
                throw new Error("Private key not specified")
            }

            const ec = new eddsa('ed25519')
            if (key instanceof Buffer) {
                this.privateKeyObject = ec.keyFromSecret(key)
            } else {
                this.privateKeyObject = ec.keyFromSecret(Buffer.from(key as string, 'base64'))
            }

        } else {
            throw new Error("algorithm not recognized")
        }
    }
}
