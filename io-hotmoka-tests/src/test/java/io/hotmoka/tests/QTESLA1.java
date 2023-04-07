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

package io.hotmoka.tests;

import java.security.KeyPair;
import java.security.PublicKey;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.crypto.api.SignatureAlgorithm;

public class QTESLA1 {
    private final static String data = "HELLO QTESLA SCHEME";

    @Test
    @DisplayName("sign data with qtesla signature")
    void sign() throws Exception {
        SignatureAlgorithm<String> qTesla1 = SignatureAlgorithms.qtesla1(String::getBytes);

        KeyPair keyPair = qTesla1.getKeyPair();
        byte[] signed = qTesla1.sign(data, keyPair.getPrivate());

        boolean isDataVerifiedCorrectly = qTesla1.verify(data, keyPair.getPublic(), signed);
        boolean isCorruptedData = !qTesla1.verify(data + "corrupted", keyPair.getPublic(), signed);

        Assertions.assertTrue(isDataVerifiedCorrectly, "data is not verified correctly");
        Assertions.assertTrue(isCorruptedData, "corrupted data is verified");
    }

    @Test
    @DisplayName("create the public key from the encoded public key")
    void testEncodedPublicKey() throws Exception {
        SignatureAlgorithm<String> qTesla1 = SignatureAlgorithms.qtesla1(String::getBytes);

        KeyPair keyPair = qTesla1.getKeyPair();
        byte[] signed = qTesla1.sign(data, keyPair.getPrivate());

        boolean isDataVerifiedCorrectly = qTesla1.verify(data, keyPair.getPublic(), signed);
        boolean isCorruptedData = !qTesla1.verify(data + "corrupted", keyPair.getPublic(), signed);

        PublicKey publicKey = qTesla1.publicKeyFromEncoding(qTesla1.encodingOf(keyPair.getPublic()));
        boolean isDataVerifiedCorrectlyWithEncodedKey = qTesla1.verify(data, publicKey, signed);
        boolean isCorruptedDataWithEncodedKey = !qTesla1.verify(data + "corrupted", publicKey, signed);

        Assertions.assertTrue(isDataVerifiedCorrectly, "data is not verified correctly");
        Assertions.assertTrue(isCorruptedData, "corrupted data is verified");
        Assertions.assertTrue(isDataVerifiedCorrectlyWithEncodedKey, "data is not verified correctly with the encoded key");
        Assertions.assertTrue(isCorruptedDataWithEncodedKey, "corrupted data is verified with the encoded key");
        Assertions.assertTrue(keyPair.getPublic().equals(publicKey), "the public keys do not match");
    }
}