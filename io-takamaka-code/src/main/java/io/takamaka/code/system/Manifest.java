package io.takamaka.code.system;

import io.takamaka.code.lang.ExternallyOwnedAccount;
import io.takamaka.code.lang.RedGreenExternallyOwnedAccount;
import io.takamaka.code.lang.View;

/**
 * The manifest of a node. It contains information about the node,
 * that can be helpful to users of the node. It is an account,
 * so that it can be used to call @View methods on itself.
 */
public final class Manifest extends ExternallyOwnedAccount {
	public final RedGreenExternallyOwnedAccount gamete;

	/**
	 * Creates a manifest.
	 * 
	 * @param gamete the gamete of the node having this manifest;
	 *               this is an account that holds all initial coins
	 * @param publicKey the public key of the manifest. Since the manifest
	 *                  is not expected to hold coins, this key is probably
	 *                  useless, but must be set as for every account
	 */
	public Manifest(RedGreenExternallyOwnedAccount gamete, String publicKey) {
		super(publicKey);

		this.gamete = gamete;
	}

	/**
	 * Yields an account created during the initialization of the node,
	 * that contains the initial stake of the node.
	 * 
	 * @return the account
	 */
	public @View RedGreenExternallyOwnedAccount getGamete() {
		return gamete;
	}
}