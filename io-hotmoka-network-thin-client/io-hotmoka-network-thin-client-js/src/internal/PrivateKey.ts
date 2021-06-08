import {Buffer} from "buffer";

export interface PrivateKey {
    filePath?: string
    privateKey?: string | Buffer
}