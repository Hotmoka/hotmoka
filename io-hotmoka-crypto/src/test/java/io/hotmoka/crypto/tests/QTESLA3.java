/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.crypto.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.security.PublicKey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.testing.AbstractLoggedTests;

public class QTESLA3 extends AbstractLoggedTests {
    private final String data = "HELLO QTESLA SCHEME";

    @Test
    @DisplayName("sign data with qtesla signature")
    void sign() throws Exception {
        var qTesla3 = SignatureAlgorithms.qtesla3();

        KeyPair keyPair = qTesla3.getKeyPair();
        byte[] signed = qTesla3.getSigner(keyPair.getPrivate(), (String s) -> s.getBytes()).sign(data);
        var verifier = qTesla3.getVerifier(keyPair.getPublic(), (String s) -> s.getBytes());
        assertTrue(verifier.verify(data, signed), "data is not verified correctly");
        assertFalse(verifier.verify(data + "corrupted", signed), "corrupted data is verified");
    }

    @Test
    @DisplayName("create the public key from the encoded public key")
    void testEncodedPublicKey() throws Exception {
        var qTesla3 = SignatureAlgorithms.qtesla3();

        KeyPair keyPair = qTesla3.getKeyPair();
        byte[] signed = qTesla3.getSigner(keyPair.getPrivate(), (String s) -> s.getBytes()).sign(data);
        var verifier = qTesla3.getVerifier(keyPair.getPublic(), (String s) -> s.getBytes());
        boolean isDataVerifiedCorrectly = verifier.verify(data, signed);
        boolean isCorruptedData = !verifier.verify(data + "corrupted", signed);
        assertTrue(isDataVerifiedCorrectly, "data is not verified correctly");
        assertTrue(isCorruptedData, "corrupted data is verified");

        PublicKey publicKey = qTesla3.publicKeyFromEncoding(qTesla3.encodingOf(keyPair.getPublic()));
        verifier = qTesla3.getVerifier(publicKey, String::getBytes);
        assertTrue(verifier.verify(data, signed), "data is not verified correctly with the encoded key");
        assertFalse(verifier.verify(data + "corrupted", signed), "corrupted data is verified with the encoded key");

        assertTrue(keyPair.getPublic().equals(publicKey), "the public keys do not match");
    }
}