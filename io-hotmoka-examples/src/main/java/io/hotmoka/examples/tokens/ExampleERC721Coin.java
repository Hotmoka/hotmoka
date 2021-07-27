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

package io.hotmoka.examples.tokens;

import static io.takamaka.code.lang.Takamaka.require;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.tokens.ERC721;

/**
 * A token example that uses the ERC721 standard implementation.
 */
public class ExampleERC721Coin extends ERC721 {
    private final Contract owner;

    /**
     * Sets the initial settings of the coin
     */
    public @FromContract ExampleERC721Coin() {
        super("CryptoSharks", "SHKS");

        owner = caller();
    }

    /**
     * Mints a token.
     *
     * @param account recipient of the created tokens
     * @param tokenId the identifier of the token to mint
     */
    public @FromContract void mint(Contract account, BigInteger tokenId) {
        require(caller() == owner, "Lack of permission");
        _mint(account, tokenId);
    }

    /**
     * Burns a token.
     *
     * @param tokenId the identifier of the token to burn
     */
    public @FromContract void burn(BigInteger tokenId) {
        require(caller() == owner, "Lack of permission");
        _burn(tokenId);
    }
}