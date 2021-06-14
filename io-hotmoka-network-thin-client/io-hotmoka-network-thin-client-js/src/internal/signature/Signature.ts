import {Algorithm} from "./Algorithm";

export interface Signature {
    /**
     * The file path of the stored private key.
     * The key should be in a PEM format.
     */
    filePath?: string

    /**
     * The raw key encoded in base64.
     */
    privateKey?: string

    /**
     * The algorithm of the signatures.
     */
    algorithm: Algorithm
}

