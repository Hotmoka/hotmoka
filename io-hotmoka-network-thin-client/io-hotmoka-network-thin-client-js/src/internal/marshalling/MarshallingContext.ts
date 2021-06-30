import {Buffer} from "buffer";
import {FieldSignatureModel} from "../../models/signatures/FieldSignatureModel";
import {StorageReferenceModel} from "../../models/values/StorageReferenceModel";
import {TransactionReferenceModel} from "../../models/values/TransactionReferenceModel";
import {Stream} from "./Stream";
import {HotmokaException} from "../exception/HotmokaException";


/**
 * A context used during object marshalling into bytes.
 */
export class MarshallingContext {
    private readonly stream = new Stream()
    private readonly memoryString = new Map<string, number>()
    private readonly memoryFieldSignature = new Map<string, number>()
    private readonly memoryStorageReference = new Map<string, number>()
    private readonly memoryTransactionReference = new Map<string, number>()


    /**
     * It returns the buffer.
     * @return the buffer
     */
    public getBuffer(): Buffer {
        return this.stream.getBuffer()
    }

    /**
     * It returns the base64 string representation of the buffer.
     * @return the base64 string representation of the buffer
     */
    public toBase64(): string {
        return this.stream.toBase64()
    }

    /**
     * Flushes the stream. This will write any buffered output bytes and flush through to the underlying stream.
     */
    public flush(): void {
        this.stream.flush()
    }

    /**
     * Writes a string.
     * @param str the string
     */
    public writeString(str: string): void {
       this.stream.writeString(str)
    }

    /**
     * Writes 16 bit char.
     * @param val the value
     */
    public writeChar(val: string): void {
       this.stream.writeChar(val)
    }

    /**
     * Writes a boolean.
     * @param val the value
     */
    public writeBoolean(val: boolean): void {
        this.stream.writeBoolean(val)
    }

    /**
     * Writes a 16 bit short.
     * @param val the value
     */
    public writeShort(val: number): void {
       this.stream.writeShort(val)
    }

    /**
     * Writes a 32 bit int.
     * @param val the value
     */
    public writeInt(val: number): void {
       this.stream.writeInt(val)
    }

    /**
     * Writes a 32 bit float.
     * @param val the value
     */
    public writeFloat(val: number): void {
        this.stream.writeFloat(val)
    }

    /**
     * Writes a 64 bit double.
     * @param val the value
     */
    public writeDouble(val: number): void {
        this.stream.writeDouble(val)
    }

    /**
     * Writes a 64 bit long.
     * @param val the value
     */
    public writeLong(val: number): void {
        this.stream.writeLong(val)
    }

    /**
     * Writes an 8 bit byte.
     * @param val the value
     */
    public writeByte(val: number): void {
        this.stream.writeByte(val)
    }

    /**
     * Writes the given integer, in a way that compacts small integers.
     * @param val the integer
     */
    public writeCompactInt(val: number): void {
        if (val < 255) {
            this.writeByte(val)
        } else {
            this.writeByte(255)
            this.writeInt(val)
        }
    }

    /**
     * Writes the given big integer, in a compact way.
     * @param biValue the big integer
     */
    public writeBigInteger(biValue: string): void {
        const value = Number(biValue)
        const small = Stream.toShort(value)

        if (value === small) {
            if (0 <= small && small <= 251)
                this.writeByte(4 + small)
            else {
                this.writeByte(0)
                this.writeShort(small)
            }
        } else if (value === Stream.toInt(value)) {
            this.writeByte(1)
            this.writeInt(Stream.toInt(value))
        } else if (BigInt(biValue) === Stream.toBigint(value).valueOf()) {
            this.writeByte(2)
            this.writeLong(value)
        } else {
            this.writeByte(3)
            const bufferBigInteger = Buffer.from(biValue)
            this.writeCompactInt(bufferBigInteger.length)
            this.writeBuffer(bufferBigInteger)
        }
    }

    /**
     * Writes a buffer.
     * @param buff the buffer
     */
    public writeBuffer(buff: Buffer): void {
        this.stream.writeBuffer(buff)
    }

    /**
     * Writes the given string into the output stream. It uses a memory
     * to avoid repeated writing of the same string: the second write
     * will refer to the first one.
     * @param str the string to write
     */
    public writeStringShared(str: string): void {
        if (str === null || str === undefined) {
            throw new HotmokaException("Cannot marshall a null string")
        }

        const index = this.memoryString.get(str)
        if (index !== undefined) {
            if (index < 254) {
                this.writeByte(index)
            } else {
                this.writeByte(254)
                this.writeInt(index)
            }
        } else {
            const next = this.memoryString.size
            if (next === Number.MAX_SAFE_INTEGER) {
                throw new HotmokaException("too many strings in the same context")
            }

            this.memoryString.set(str, next)
            this.writeByte(255)
            this.writeString(str)
        }
    }

    /**
     * Writes the given field signature into the output stream. It uses
     * a memory to recycle field signatures already written with this context
     * and compress them by using their progressive number instead.
     * @param fieldSignature the field signature to write
     */
    public writeFieldSignature(fieldSignature: FieldSignatureModel): void {
        const key = MarshallingContext.fieldSignatureToBase64Key(fieldSignature)
        const index = this.memoryFieldSignature.get(key)

        if (index !== undefined) {
            if (index < 254) {
                this.writeByte(index)
            } else {
                this.writeByte(254)
                this.writeInt(index)
            }
        } else {
            const next = this.memoryFieldSignature.size
            if (next === Number.MAX_SAFE_INTEGER) {
                throw new HotmokaException("too many field signatures in the same context")
            }

            this.memoryFieldSignature.set(key, next)
            this.writeByte(255)
            fieldSignature.into(this)
        }
    }

    /**
     * Writes the given storage reference into the output stream. It uses
     * a memory to recycle storage references already written with this context
     * and compress them by using their progressive number instead.
     * @param storageReference the storage reference to write
     */
    public writeStorageReference(storageReference: StorageReferenceModel): void {
        const key = MarshallingContext.storageReferenceToBase64Key(storageReference)
        const index = this.memoryStorageReference.get(key)

        if (index !== undefined) {
            if (index < 254) {
                this.writeByte(index)
            } else {
                this.writeByte(254)
                this.writeInt(index)
            }
        } else {
            const next = this.memoryStorageReference.size
            if (next === Number.MAX_SAFE_INTEGER) {
                throw new HotmokaException("too many storage references in the same context")
            }

            this.memoryStorageReference.set(key, next)
            this.writeByte(255)
            TransactionReferenceModel.into(this, storageReference.transaction)
            this.writeBigInteger(storageReference.progressive)
        }
    }

    /**
     * Writes the given transaction reference into the output stream. It uses
     * a memory to recycle transaction references already written with this context
     * and compress them by using their progressive number instead.
     *
     * @param transactionReference the transaction reference to write
     */
    public writeTransactionReference(transactionReference: TransactionReferenceModel): void {
        const index = this.memoryTransactionReference.get(transactionReference.hash)

        if (index !== undefined) {
            if (index < 254) {
                this.writeByte(index)
            } else {
                this.writeByte(254)
                this.writeInt(index)
            }
        } else {
            const next = this.memoryTransactionReference.size
            if (next === Number.MAX_SAFE_INTEGER) {
                throw new HotmokaException("too many transaction references in the same context")
            }

            this.memoryTransactionReference.set(transactionReference.hash, next)
            this.writeByte(255)
            this.writeBuffer(Buffer.from(transactionReference.hash, 'hex'))
        }
    }

    private static fieldSignatureToBase64Key(fieldSignature: FieldSignatureModel): string {
        const key = fieldSignature.type + fieldSignature.name + fieldSignature.definingClass
        return Buffer.from(key).toString('base64')
    }

    private static storageReferenceToBase64Key(storageReference: StorageReferenceModel): string {
        const key = storageReference.progressive + storageReference.transaction.hash
        return Buffer.from(key).toString('base64')
    }
}