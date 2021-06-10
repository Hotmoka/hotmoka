import {Buffer} from "buffer";
import * as crypto from "crypto"
import {KeyObject} from "crypto"
import * as fs from "fs";
import * as path from "path"
import {Algorithm, PrivateKey} from "./PrivateKey";

export class Signer {
    public static readonly INSTANCE = new Signer()
    private privateKey: KeyObject | null = null
    private algorithm: Algorithm = Algorithm.SHA256WITHDSA

    private Signer() {
        // private
    }


    /**
     * Signs the data.
     * @param data the data
     * @return the signed data as a base64 string
     */
    public sign(data: Buffer): string {
        if (this.privateKey === null) {
            throw new Error("Private key not loaded")
        }

        return crypto.sign(Algorithm.ED25519 ? null : "sha256", data, this.privateKey).toString('base64');
    }

    /**
     * It loads the private key.
     * @param privateKey the private key object
     */
    public init(privateKey: PrivateKey): void {

        if (privateKey.algorithm !== Algorithm.ED25519 && privateKey.algorithm !== Algorithm.SHA256WITHDSA) {
            throw new Error("algorithm not recognized")
        }

        this.algorithm = privateKey.algorithm
        if (privateKey.filePath) {
            const absolutePath = path.resolve(privateKey.filePath);
            const key = fs.readFileSync(absolutePath, "utf8");
            this.privateKey = crypto.createPrivateKey({
                key: key,
            })
        } else if (privateKey.privateKey) {
            this.privateKey = crypto.createPrivateKey({
                key: privateKey.privateKey,
            })
        } else {
            throw new Error("Private key not specified")
        }
    }
}
