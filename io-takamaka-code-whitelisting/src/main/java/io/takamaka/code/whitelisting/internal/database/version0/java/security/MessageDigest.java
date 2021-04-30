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

package io.takamaka.code.whitelisting.internal.database.version0.java.security;

public interface MessageDigest {
	// this is white-listed since it is not possible to register new providers
	// by using white-listed methods only
	java.security.MessageDigest getInstance(java.lang.String algorithm);
	byte[] digest();
	byte[] digest(byte[] input);
	int digest(byte[] buf, int offset, int len);
	void update(byte input);
	void update(byte[] input);
	void update(byte[] input, int offset, int len);
}