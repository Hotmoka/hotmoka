import {Buffer} from "buffer";
import {FieldSignatureModel} from "../../models/signatures/FieldSignatureModel";
import {StorageReferenceModel} from "../../models/values/StorageReferenceModel";
import {TransactionReferenceModel} from "../../models/values/TransactionReferenceModel";

/**
 * A context used during object marshalling into bytes.
 */
export class MarshallingContext {
    /**
     * Java ObjectOutputStream header signature.
     */
    public readonly STREAM_MAGIC = 44269
    public readonly STREAM_VERSION = 5

    /**
     * Block of optional data. Byte following tag indicates number of bytes in this block data.
     */
    private readonly TC_BLOCKDATA = 119
    /**
     * Long Block data. The long following the tag indicates the number of bytes in this block data.
     */
    private readonly TC_BLOCKDATALONG = 122

    /**
     * The buffer to write to.
     */
    private buffer = Buffer.alloc(1024)

    /**
     * The offset of the block body buffer.
     */
    private offset = 0

    private readonly memoryString = new Map<string, number>()
    private readonly memoryFieldSignature = new Map<string, number>()
    private readonly memoryStorageReference = new Map<string, number>()
    private readonly memoryTransactionReference = new Map<string, number>()


    /**
     * Writes block data header. Data blocks shorter than 256 bytes
     * are prefixed with a 2-byte header; all others start with
     * a 5-byte header.
     * @return the block data header buffer
     */
    private writeBlockHeader(len: number): Buffer {
        let buffer: Buffer;

        if (len <= 255) {
            buffer = Buffer.alloc(2)
            MarshallingContext.writeByte(buffer, this.TC_BLOCKDATA, 0)
            MarshallingContext.writeByte(buffer, len, 1)
        } else {
            buffer = Buffer.alloc(5)
            MarshallingContext.writeByte(buffer, this.TC_BLOCKDATALONG, 0)
            MarshallingContext.writeByte(buffer, len >>> 24, 1)
            MarshallingContext.writeByte(buffer, len >>> 16, 2)
            MarshallingContext.writeByte(buffer, len >>> 8, 3)
            MarshallingContext.writeByte(buffer, len, 4)
        }

        return buffer
    }

    /**
     * It writes the magic number and version to the stream.
     * @return the stream header buffer
     */
    private writeStreamHeader(): Buffer {
        const buffer = Buffer.alloc(4)
        MarshallingContext.writeByte(buffer, this.STREAM_MAGIC >>> 8, 0)
        MarshallingContext.writeByte(buffer, this.STREAM_MAGIC, 1)
        MarshallingContext.writeByte(buffer, this.STREAM_VERSION >>> 8, 2)
        MarshallingContext.writeByte(buffer, this.STREAM_VERSION, 3)
        return buffer
    }

    /**
     * It returns the buffer.
     * @return the buffer
     */
    public getBuffer(): Buffer {
        return this.buffer
    }

    /**
     * It returns the base64 string representation of the buffer.
     * @return the base64 string representation of the buffer
     */
    public toBase64(): string {
        return this.buffer.toString('base64')
    }

    /**
     * Flushes the stream. This will write any buffered output bytes and flush through to the underlying stream.
     */
    public flush(): void {
        const streamHeaderBuffer = this.writeStreamHeader()
        const blockHeaderBuffer = this.writeBlockHeader(this.offset)
        const blockBodyBuffer = Buffer.alloc(this.offset)
        this.buffer.copy(blockBodyBuffer, 0, 0, this.offset)

        this.buffer = Buffer.concat([streamHeaderBuffer, blockHeaderBuffer, blockBodyBuffer])
    }

    /**
     * Writes a string.
     * @param str the string
     */
    public writeString(str: string): void {
        if (str === null || str === undefined) {
            throw new Error("Cannot marshall a null string")
        }

        this.writeShort(str.length)
        const written = this.buffer.write(str, this.offset)
        this.offset += written
    }

    /**
     * Writes 16 bit char.
     * @param val the value
     */
    public writeChar(val: string): void {
        if (val && val.length > 1) {
            throw new Error("Value should have length 1")
        }
        this.writeByte(val.charCodeAt(0) >>> 8)
        this.writeByte(val.charCodeAt(0))
    }

    /**
     * Writes a boolean.
     * @param val the value
     */
    public writeBoolean(val: boolean): void {
        this.writeByte(val ? 1 : 0)
    }

    /**
     * Writes a 16 bit short.
     * @param val the value
     */
    public writeShort(val: number): void {
        this.writeByte(val >>> 8)
        this.writeByte(val)
    }

    /**
     * Writes a 32 bit int.
     * @param val the value
     */
    public writeInt(val: number): void {
        this.buffer.writeInt32BE(val, this.offset)
        this.offset += 4
    }

    /**
     * Writes a 32 bit float.
     * @param val the value
     */
    public writeFloat(val: number): void {
        this.buffer.writeFloatBE(val, this.offset)
        this.offset += 4
    }

    /**
     * Writes a 64 bit double.
     * @param val the value
     */
    public writeDouble(val: number): void {
        this.buffer.writeDoubleBE(val, this.offset)
        this.offset += 8
    }

    /**
     * Writes a 64 bit long.
     * @param val the value
     */
    public writeLong(val: number): void {
        this.buffer.writeBigInt64BE(BigInt(val), this.offset)
        this.offset += 8
    }

    /**
     * Writes an 8 bit byte.
     * @param val the value
     */
    public writeByte(val: number): void {
        this.buffer.writeInt8(MarshallingContext.toByte(val), this.offset++)
    }

    /**
     * Writes the given integer, in a way that compacts small integers.
     * @param val the integer
     */
    public writeCompactInt(val: number): void {
        if (val < 255) {
            this.writeInt(val)
        } else {
            this.writeByte(255)
            this.writeInt(val)
        }
    }

    /**
     * Writes the given big integer, in a compact way.
     * @param biValue the big integer
     */
    public writeBigInteger(biValue: number): void {
        const small = MarshallingContext.toShort(biValue)

        if (biValue === small) {
            if (0 <= small && small <= 251)
                this.writeByte(4 + small)
            else {
                this.writeByte(0)
                this.writeShort(small)
            }
        } else if (biValue === MarshallingContext.toInt(biValue)) {
            this.writeByte(1)
            this.writeInt(MarshallingContext.toInt(biValue))
        } else if (BigInt(biValue) === MarshallingContext.toBigint(biValue).valueOf()) {
            this.writeByte(2)
            this.writeLong(biValue)
        } else {
            this.writeByte(3)
            // TODO: implement toByteArray of bigInt
            const buff = Buffer.alloc(8)
            buff.writeBigInt64BE(BigInt(biValue))
            this.writeCompactInt(buff.length)
            this.writeBuffer(buff)
        }
    }

    /**
     * Writes a buffer.
     * @param buff the buffer
     */
    public writeBuffer(buff: Buffer): void {
        buff.copy(this.buffer, this.offset, 0, buff.length)
        this.offset += buff.length
    }

    /**
     * Writes the given string into the output stream. It uses a memory
     * to avoid repeated writing of the same string: the second write
     * will refer to the first one.
     * @param str the string to write
     */
    public writeStringShared(str: string): void {
        if (str === null || str === undefined) {
            throw new Error("Cannot marshall a null string")
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
                throw new Error("too many strings in the same context")
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
                throw new Error("too many field signatures in the same context")
            }

            this.memoryFieldSignature.set(key, next)
            this.writeByte(255)
           //TODO: fieldSignature.definingClass.into(this)
            this.writeString(fieldSignature.name)
           //TODO: fieldSignature.type.into(this)
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
                throw new Error("too many storage references in the same context")
            }

            this.memoryStorageReference.set(key, next)
            this.writeByte(255)
            storageReference.transaction.into(this)
            this.writeBigInteger(Number(storageReference.progressive))
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
                throw new Error("too many transaction references in the same context")
            }

            this.memoryTransactionReference.set(transactionReference.hash, next)
            this.writeByte(255)
            this.writeBuffer(Buffer.from(transactionReference.hash, 'hex'))
        }
    }

    /**
     * Writes an 8 bit byte to a buffer.
     * @param buffer the buffer
     * @param val the value
     * @param offset the offset
     */
    public static writeByte(buffer: Buffer, val: number, offset: number): void {
        buffer.writeInt8(MarshallingContext.toByte(val), offset)
    }

    public static toShort(val: number): number {
        const int16 = new Int16Array(1)
        int16[0] = val
        return int16[0]
    }

    public static toByte(val: number): number {
        const int8 = new Int8Array(1)
        int8[0] = val
        return int8[0]
    }

    public static toInt(val: number): number {
        const int32 = new Int32Array(1)
        int32[0] = val
        return int32[0]
    }

    public static toBigint(val: number): bigint {
        const bigInt64 = new BigInt64Array(1)
        bigInt64[0] = BigInt(val)
        return bigInt64[0]
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