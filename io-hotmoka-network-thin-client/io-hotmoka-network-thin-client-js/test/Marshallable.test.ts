import {expect} from "chai";
import {MarshallingContext} from "../src/internal/marshalling/MarshallingContext";


describe('Testing the marshalling of the JS objects to base64', () => {

    it('writeShort(22) = rO0ABXcCABY=', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeShort(22)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcCABY=')
    })

    it('writeInt(32) = rO0ABXcEAAAAIA==', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeInt(32)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcEAAAAIA==')
    })

    it('writeLong(92) = rO0ABXcIAAAAAAAAAFw=', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeLong(92)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcIAAAAAAAAAFw=')
    })

    it('writeLong(1000129) = rO0ABXcIAAAAAAAPQsE=', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeLong(1000129)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcIAAAAAAAPQsE=')
    })

    it('writeLong(9007199254740991) = rO0ABXcIAB////////8=', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeLong(9007199254740991)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcIAB////////8=')
    })


    it('writeBigInteger(9007199254740991) = rO0ABXcJAgAf////////', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeBigInteger(9007199254740991)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcJAgAf////////')
    })

    it('writeBigInteger(9) = rO0ABXcBDQ==', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeBigInteger(9)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcBDQ==')
    })

    it('writeBigInteger(7654319) = rO0ABXcFAQB0y68=', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeBigInteger(7654319)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcFAQB0y68=')
    })

    /*
    it('writeBigInteger(9007199254740991765896) = rO0ABXcMAwoB6Ef//////G2I', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeBigInteger(9007199254740991765896)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcMAwoB6Ef//////G2I')
    })

     */

    it('writeFloat(33.8) = rO0ABXcEQgczMw==', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeFloat(33.8)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcEQgczMw==')
    })

    it('writeBoolean(true) = rO0ABXcBAQ==', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeBoolean(true)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcBAQ==')
    })

    it('writeChar("d") = rO0ABXcCAGQ=', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeChar("d")
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcCAGQ=')
    })
})

