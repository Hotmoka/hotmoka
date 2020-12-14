package io.takamaka.code.system;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.Takamaka;
import io.takamaka.code.lang.View;

/**
 * The manifest of a node. It contains information about the node,
 * that can be helpful its users.
 */
public final class Manifest extends Storage {

	/**
	 * The initial chainId of the node having this manifest.
	 */
	private String chainId;

	/**
	 * The current validators of this node. This might be empty.
	 */
	private final Validators validators;

	/**
	 * Creates a manifest.
	 * 
	 * @param chainId the initial chainId of the node having this manifest
	 * @param validators the initial validators of the node having this manifest, as a space-separated
	 *                   string of identifier and publicKey, alternated
	 * @param powers the initial powers of the validators, as a space-separated string of integers
	 * @throws NullPointerException if any parameter is null
	 */
	public Manifest(String chainId, String validators, String powers) {
		if (chainId == null)
			throw new NullPointerException("the chain identifier must be non-null");

		this.chainId = chainId;
		String[] validatorsElements = splitAtSpaces(validators);
		String[] powersElements = splitAtSpaces(powers);
		Takamaka.require(powersElements.length * 2 == validatorsElements.length, () -> "inconsistent length of validators and powers: " + validatorsElements.length + " vs " + powersElements.length);

		Validator[] validatorsArray = new Validator[powersElements.length];
		for (int pos = 0; pos < validatorsElements.length; pos += 2)
			validatorsArray[pos / 2] = new Validator(validatorsElements[pos], validatorsElements[pos + 1]);

		BigInteger[] powersArray = new BigInteger[powersElements.length];
		for (int pos = 0; pos < powersElements.length; pos++)
			powersArray[pos] = new BigInteger(powersElements[pos]);

		this.validators = mkValidators(validatorsArray, powersArray);
	}

	static String[] splitAtSpaces(String s) {
		List<String> list = new ArrayList<>();
		int pos;
		while ((pos = s.indexOf(' ')) >= 0) {
			list.add(s.substring(0, pos));
			s = s.substring(pos + 1);
		}

		if (!s.isEmpty())
			list.add(s);

		return list.toArray(String[]::new);
	}

	/**
	 * Yields the specific implementation of the validators set for this manifest.
	 * Subclasses might redefine.
	 * 
	 * @param validators the initial validators of the node having this manifest. This can be empty
	 *                   but is never {@code null}
	 * @return the validators set
	 */
	protected Validators mkValidators(Validator[] validators, BigInteger[] powers) {
		return new Validators(validators, powers);
	}

	/**
	 * Yields the current chain identifier for the node having this manifest.
	 * 
	 * @return the chain identifier
	 */
	public @View String getChainId() {
		return chainId;
	}

	/**
	 * Yields the set of the current validators of the node having this manifest.
	 * 
	 * @return the set of current validators. This might be empty
	 */
	public @View Validators getValidators() {
		return validators;
	}

	/**
	 * Changes the chain identifier of the node having this manifest.
	 * 
	 * @param newChainId the new chain identifier of the node
	 */
	public void setChainId(String newChainId) {
		throw new UnsupportedOperationException("this manifest does not allow one to change the node's chain identifier");
	}
}