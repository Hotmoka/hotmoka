package io.takamaka.code.tokens;

/*
Copyright 2021 Filippo Fantinato and Fausto Spoto

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

import static io.takamaka.code.lang.Takamaka.require;

import java.math.BigInteger;

import io.takamaka.code.lang.FromContract;

/**
 * An ERC721 non-fungible token collection with a public burn method.
 */
public abstract class ERC721Burnable extends ERC721 {

	/**
	 * Builds a collection of non-fungible tokens that does not generate events.
	 * 
	 * @param name the name of the collection
	 * @param symbol the symbol of the collection
	 */
	public ERC721Burnable(String name, String symbol) {
		super(name, symbol);
	}

	/**
	 * Builds a collection of non-fungible tokens.
	 * 
	 * @param name the name of the collection
	 * @param symbol the symbol of the collection
	 * @param generateEvents true if and only if the collection generates events
	 */
	public ERC721Burnable(String name, String symbol, boolean generateEvents) {
		super(name, symbol, generateEvents);
	}

	/**
	 * Burns {@code tokenId}. The caller of this method must be the owner of
	 * {@code tokenId} or be approved for it.
	 * 
	 * @param tokenId the token to burn
	 */
	@FromContract
	public void burn(BigInteger tokenId) {
		require(_isApprovedOrOwner(caller(), tokenId), "caller is not owner nor approved");

		_burn(tokenId);
	}
}