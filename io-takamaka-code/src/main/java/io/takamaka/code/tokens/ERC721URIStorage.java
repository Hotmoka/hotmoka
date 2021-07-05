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

import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.View;
import io.takamaka.code.math.UnsignedBigInteger;
import io.takamaka.code.util.StorageMap;
import io.takamaka.code.util.StorageMapView;
import io.takamaka.code.util.StorageTreeMap;

/**
 * A token collection with the possibility of
 * setting the URI for each token in the collection.
 */
public abstract class ERC721URIStorage extends ERC721 implements IERC721URIStorageView {
	private final StorageMap<UnsignedBigInteger, String> tokenURIs = new StorageTreeMap<>();

	/**
	 * Builds a collection of non-fungible tokens that does not generate events.
	 * 
	 * @param name the name of the collection
	 * @param symbol the symbol of the collection
	 */
	public ERC721URIStorage(String name, String symbol) {
		super(name, symbol);
	}

	/**
	 * Builds a collection of non-fungible tokens.
	 * 
	 * @param name the name of the collection
	 * @param symbol the symbol of the collection
	 * @param generateEvents true if and only if the collection generates events
	 */
	public ERC721URIStorage(String name, String symbol, boolean generateEvents) {
		super(name, symbol, generateEvents);
	}

	@View
	public String tokenURI(UnsignedBigInteger tokenId) {
		return computeTokenURI(tokenId, tokenURIs);
	}

	private String computeTokenURI(UnsignedBigInteger tokenId, StorageMapView<UnsignedBigInteger, String> tokenURIs) {
		require(_exists(tokenId), "URI query for non-existent token");

		String tokenURI = tokenURIs.getOrDefault(tokenId, "");
		String base = _baseURI();

		if (base.isEmpty())
			return tokenURI;
		else if (!tokenURI.isEmpty())
			return base + tokenURI;
		else
			return super.tokenURI(tokenId);
	}

	/**
	 * Sets the URI of {@code tokenId}.
	 *
	 * @param tokenId the token whose URI must be set; this must exist
	 * @param tokenURI the URI set for the token; this must not be {@code null}
	 */
	protected void _setTokenURI(UnsignedBigInteger tokenId, String tokenURI) {
		require(_exists(tokenId), "URI set of nonexistent token");
		require(tokenURI != null, "the URI cannot be null");
		tokenURIs.put(tokenId, tokenURI);
	}

	@Override
	protected void _burn(UnsignedBigInteger tokenId) {
		super._burn(tokenId);
		tokenURIs.remove(tokenId);
	}

	@Exported
	protected class ERC721URIStorageSnapshot extends ERC721Snapshot implements IERC721URIStorageView {
		private final StorageMapView<UnsignedBigInteger, String> tokenURIs = ERC721URIStorage.this.tokenURIs;

		@Override @View
		public String tokenURI(UnsignedBigInteger tokenId) {
			return computeTokenURI(tokenId, tokenURIs);
		}

		@Override @View
		public IERC721URIStorageView snapshot() {
			return this;
		}
	}

	@Override @View
	public IERC721URIStorageView snapshot() {
		return new ERC721URIStorageSnapshot();
	}
}