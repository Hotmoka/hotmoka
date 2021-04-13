package io.hotmoka.crypto.internal;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Arrays;

import io.hotmoka.crypto.SignatureAlgorithm;

/**
 * A dummy signature algorithm that signs everything with an empty array of bytes.
 * 
 * @param <T> the type of values that get signed
 */
public class EMPTY<T> implements SignatureAlgorithm<T> {
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
	public String getName() {
		return "empty";
	}
}