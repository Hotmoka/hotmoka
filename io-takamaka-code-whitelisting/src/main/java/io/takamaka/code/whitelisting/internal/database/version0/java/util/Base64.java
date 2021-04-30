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

package io.takamaka.code.whitelisting.internal.database.version0.java.util;

public interface Base64 {
	java.util.Base64.Decoder getDecoder();
	java.util.Base64.Encoder getEncoder();
	
	interface Decoder {
		byte[] decode(byte[] src);
		byte[] decode(String src);
		int decode(byte[] src, byte[] dst);
	}

	interface Encoder {
		byte[] encode(byte[] src);
		int encode(byte[] src, byte[] dst);
		String encodeToString(byte[] src);
	}
}