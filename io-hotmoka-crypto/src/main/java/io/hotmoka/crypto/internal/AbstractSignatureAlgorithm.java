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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hotmoka.crypto.internal;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.Key;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;

import io.hotmoka.crypto.SignatureAlgorithm;

/**
 * Shared code of signature algorithms.
 */
abstract class AbstractSignatureAlgorithm<T> implements SignatureAlgorithm<T> {

	protected final void writePemFile(byte[] key, String description, String filename) throws IOException {
		PemObject pemObject = new PemObject(description, key);

		try(PemWriter pemWriter = new PemWriter(new OutputStreamWriter(new FileOutputStream(filename)))) {
			pemWriter.writeObject(pemObject);
		}
	}

	protected final void writePemFile(Key key, String description, String filename) throws IOException {
		writePemFile(key.getEncoded(), description, filename);
	}

	protected byte[] getPemFile(String file) throws IOException {
		try (PemReader reader = new PemReader(new FileReader(file))) {
			return reader.readPemObject().getContent();
		}
	}
}