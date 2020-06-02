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

	public Manifest(RedGreenExternallyOwnedAccount gamete) {
		super("");

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