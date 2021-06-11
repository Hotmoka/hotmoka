import {Buffer} from "buffer";

export interface PrivateKey {
    filePath?: string
    privateKey?: Buffer
    algorithm: Algorithm
}

export enum Algorithm {
    SHA256DSA,
    ED25519
}