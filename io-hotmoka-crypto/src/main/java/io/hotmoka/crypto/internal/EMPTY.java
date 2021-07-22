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

package io.hotmoka.crypto.internal;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

/**
 * A dummy signature algorithm that signs everything with an empty array of bytes.
 * 
 * @param <T> the type of values that get signed
 */
public class EMPTY<T> extends AbstractSignatureAlgorithm<T> {
	private final static byte[] EMPTY = new byte[0];

	private final KeyPair dummyKeys = new KeyPair(
	new PublicKey() {
		private static final long serialVersionUID = 1L;
		
		@Override
		public String getFormat() {
			return "empty";
		}
		
		@Override
		public byte[] getEncoded() {
			return EMPTY;
		}
		
		@Override
		public String getAlgorithm() {
			return "empty";
		}
	},
	new PrivateKey() {
		private static final long serialVersionUID = 1L;
		
		@Override
		public String getFormat() {
			return "empty";
		}
		
		@Override
		public byte[] getEncoded() {
			return EMPTY;
		}
		
		@Override
		public String getAlgorithm() {
			return "empty";
		}
	});

	public EMPTY() {
	}

	@Override
	public KeyPair getKeyPair() {
		return dummyKeys;
	}

	@Override
	public byte[] sign(T what, PrivateKey privateKey) {
		return EMPTY;
	}

	@Override
	public boolean verify(T what, PublicKey publicKey, byte[] signature) {
		return Arrays.equals(EMPTY, signature);
	}

	@Override
	public PublicKey publicKeyFromEncoded(byte[] encoded) {
		return new PublicKey() {
			private static final long serialVersionUID = 1L;

			@Override
			public String getFormat() {
				return "empty";
			}
			
			@Override
			public byte[] getEncoded() {
				return EMPTY;
			}
			
			@Override
			public String getAlgorithm() {
				return "empty";
			}
		};
	}

	@Override
	public PrivateKey privateKeyFromEncoded(byte[] encoded) throws InvalidKeySpecException {
		return new PrivateKey() {
			private static final long serialVersionUID = 1L;

			@Override
			public String getAlgorithm() {
				return "empty";
			}

			@Override
			public String getFormat() {
				return "empty";
			}

			@Override
			public byte[] getEncoded() {
				return EMPTY;
			}
		};
	}

	@Override
	public String getName() {
		return "empty";
	}

	@Override
	public void dumpAsPem(String filePrefix, KeyPair keys) throws IOException {
		byte[] nothing = new byte[0];
		writePemFile(nothing, "PRIVATE KEY", filePrefix + ".pri");
		writePemFile(nothing, "PUBLIC KEY", filePrefix + ".pub");
	}

	@Override
	public KeyPair readKeys(String filePrefix) throws IOException, InvalidKeySpecException {
		return dummyKeys;
	}
}