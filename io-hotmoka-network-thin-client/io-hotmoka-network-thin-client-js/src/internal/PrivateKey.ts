import {Buffer} from "buffer";

export interface PrivateKey {
    filePath?: string
    privateKey?: string | Buffer
    algorithm: Algorithm
}

export enum Algorithm {
    SHA256WITHDSA,
    ED25519
}