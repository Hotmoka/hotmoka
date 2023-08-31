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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.logging.LogManager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.crypto.api.SignatureAlgorithm;

public class QTESLA3 {
    private final String data = "HELLO QTESLA SCHEME";

    @Test
    @DisplayName("sign data with qtesla signature")
    void sign() throws Exception {
        SignatureAlgorithm<String> qTesla3 = SignatureAlgorithms.qtesla3(String::getBytes);

        KeyPair keyPair = qTesla3.getKeyPair();
        byte[] signed = qTesla3.sign(data, keyPair.getPrivate());

        boolean isDataVerifiedCorrectly = qTesla3.verify(data, keyPair.getPublic(), signed);
        boolean isCorruptedData = !qTesla3.verify(data + "corrupted", keyPair.getPublic(), signed);

        assertTrue(isDataVerifiedCorrectly, "data is not verified correctly");
        assertTrue(isCorruptedData, "corrupted data is verified");
    }

    @Test
    @DisplayName("create the public key from the encoded public key")
    void testEncodedPublicKey() throws Exception {
        SignatureAlgorithm<String> qTesla3 = SignatureAlgorithms.qtesla3(String::getBytes);

        KeyPair keyPair = qTesla3.getKeyPair();
        byte[] signed = qTesla3.sign(data, keyPair.getPrivate());

        boolean isDataVerifiedCorrectly = qTesla3.verify(data, keyPair.getPublic(), signed);
        boolean isCorruptedData = !qTesla3.verify(data + "corrupted", keyPair.getPublic(), signed);

        PublicKey publicKey = qTesla3.publicKeyFromEncoding(qTesla3.encodingOf(keyPair.getPublic()));
        boolean isDataVerifiedCorrectlyWithEncodedKey = qTesla3.verify(data, publicKey, signed);
        boolean isCorruptedDataWithEncodedKey = !qTesla3.verify(data + "corrupted", publicKey, signed);

        assertTrue(isDataVerifiedCorrectly, "data is not verified correctly");
        assertTrue(isCorruptedData, "corrupted data is verified");
        assertTrue(isDataVerifiedCorrectlyWithEncodedKey, "data is not verified correctly with the encoded key");
        assertTrue(isCorruptedDataWithEncodedKey, "corrupted data is verified with the encoded key");
        assertTrue(keyPair.getPublic().equals(publicKey), "the public keys do not match");
    }

    static {
		String current = System.getProperty("java.util.logging.config.file");
		if (current == null) {
			// if the property is not set, we provide a default (if it exists)
			URL resource = QTESLA3.class.getClassLoader().getResource("logging.properties");
			if (resource != null)
				try (var is = resource.openStream()) {
					LogManager.getLogManager().readConfiguration(is);
				}
			catch (SecurityException | IOException e) {
				throw new RuntimeException("Cannot load logging.properties file", e);
			}
		}
	}
}