import {Buffer} from "buffer";

/**
 * A context used during object marshalling into bytes.
 */
export class MarshallingContext {
    /**
     * The buffer to write to.
     */
    private buffer = Buffer.from("")

    public geBuffer(): Buffer {
        return this.buffer
    }

    public static toBuffer(): Buffer {
        const buff = Buffer.from("")

        return buff
    }

    /**
     * Writes a string.
     * @param str the string
     */
    public write(str: string): void {
        this.buffer.write(str)
    }

    /**
     * Writes a 32 bit int.
     * @param val the value
     */
    public writeInt(val: number): void {
        this.buffer.writeInt32BE(val)
    }

    /**
     * Writes an 8 bit byte.
     * @param val the value
     */
    public writeByte(val: number): void {
        this.buffer.writeInt8(val)
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
     * Writes a buffer.
     * @param buff the buffer
     */
    public writeBuffer(buff: Buffer): void {
        this.buffer = Buffer.concat([this.buffer, buff])
    }

    /**
     * Writes an array of buffers.
     * @param buffers the array of buffers
     */
    public writeBuffers(buffers: Array<Buffer>): void {
        this.writeCompactInt(buffers.length)
        this.buffer = Buffer.concat([this.buffer, ...buffers])
    }
}