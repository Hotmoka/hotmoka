package io.takamaka.code.tokens;

import java.math.BigInteger;

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

import io.takamaka.code.lang.View;

/**
 * The read-only interface of a token collection with the possibility of
 * setting a URI for each token in the collection.
 */
public interface IERC721URIStorageView extends IERC721View {

	/**
	 * Returns the Uniform Resource Identifier (URI) for token {@code tokenId}.
	 * 
	 * @param tokenId the token whose URI must be returned
	 * @return the URI, as a string
	 */
	@View
	String tokenURI(BigInteger tokenId);

	/**
	 * Yields an immutable snapshot of this view.
	 */
	@Override
	IERC721URIStorageView snapshot();
}