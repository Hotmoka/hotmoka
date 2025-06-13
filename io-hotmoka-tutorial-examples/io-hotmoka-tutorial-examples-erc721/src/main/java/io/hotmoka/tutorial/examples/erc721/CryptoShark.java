/*
    Copyright (C) 2021 Fausto Spoto (fausto.spoto@gmail.com)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package io.hotmoka.tutorial.examples.erc721;

import static io.takamaka.code.lang.Takamaka.require;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.tokens.ERC721;

/**
 * An ERC721 token example that allows its creator only to mint and burn tokens.
 */
public class CryptoShark extends ERC721 {
    private final Contract owner;

    /**
     * Sets the initial settings of the coin
     */
    public @FromContract CryptoShark() {
        super("CryptoShark", "SHK");
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