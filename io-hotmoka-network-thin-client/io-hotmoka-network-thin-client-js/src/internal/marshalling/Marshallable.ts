import {MarshallingContext} from "./MarshallingContext";
import {Buffer} from "buffer";

/**
 * An object that can be marshalled into a stream.
 */
export abstract class Marshallable {

    /**
     * Marshalls this object into a buffer.
     * @return the buffer of bytes
     */
    protected marshall(): Buffer {
        const marshallingContext = new MarshallingContext()
        this.into(marshallingContext)
        marshallingContext.flush()

        return marshallingContext.getBuffer()
    }

    /**
     * Marshals this object into a given stream.
     * @param context the context holding the stream
     */
    protected abstract into(context: MarshallingContext): void
}