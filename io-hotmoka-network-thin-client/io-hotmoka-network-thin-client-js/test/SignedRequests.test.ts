import {expect} from "chai";
import {Signer} from "../src/internal/Signer"

// load private key
Signer.loadPrivateKey("./test/keys/ed25519.pri")

describe('Testing the signed requests of the Hotmoka JS objects', () => {

    it('Signed string', async () => {
        const result = Signer.signAndEncodeToBase64(Buffer.from("hello"), Signer.privateKey)
        expect(result).to.be.eq("Zest4OcIbf6LLGkXPw7zOL4WTTSNUyRO/4ipi/UE6bVvdx8hRUl5nmjweF1/7TnIrrgtdhK8gWpu3XAz78H6Bw==")
    })
})


