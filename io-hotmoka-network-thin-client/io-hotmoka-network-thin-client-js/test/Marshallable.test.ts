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

})

