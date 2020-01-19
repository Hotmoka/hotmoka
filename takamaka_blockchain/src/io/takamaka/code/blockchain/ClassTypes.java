package io.takamaka.code.blockchain;

import java.math.BigInteger;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.types.ClassType;
import io.takamaka.code.constants.Constants;

/**
 * Constants for most used class types.
 */
@Immutable
public final class ClassTypes {

	private ClassTypes() {}

	/**
	 * The frequently used class type for {@link java.lang.Object}.
	 */
	public final static ClassType OBJECT = new ClassType(Object.class.getName());

	/**
	 * The frequently used class type for {@link java.lang.String}.
	 */
	public final static ClassType STRING = new ClassType(String.class.getName());

	/**
	 * The frequently used class type for {@link java.math.BigInteger}.
	 */
	public final static ClassType BIG_INTEGER = new ClassType(BigInteger.class.getName());

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.ExternallyOwnedAccount}.
	 */
	public final static ClassType EOA = new ClassType(Constants.EOA_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.RedGreenExternallyOwnedAccount}.
	 */
	public final static ClassType RGEOA = new ClassType(Constants.RGEOA_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.TestExternallyOwnedAccount}.
	 */
	public final static ClassType TEOA = new ClassType(Constants.TEOA_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.TestRedGreenExternallyOwnedAccount}.
	 */
	public final static ClassType TRGEOA = new ClassType(Constants.TRGEOA_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Contract}.
	 */
	public final static ClassType CONTRACT = new ClassType(Constants.CONTRACT_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.RedGreenContract}.
	 */
	public final static ClassType RGCONTRACT = new ClassType(Constants.RGCONTRACT_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Storage}.
	 */
	public final static ClassType STORAGE = new ClassType(Constants.STORAGE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Takamaka}.
	 */
	public final static ClassType TAKAMAKA = new ClassType(io.takamaka.code.instrumentation.Constants.TAKAMAKA_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Event}.
	 */
	public final static ClassType EVENT = new ClassType(io.takamaka.code.instrumentation.Constants.EVENT_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.PayableContract}.
	 */
	public final static ClassType PAYABLE_CONTRACT = new ClassType(Constants.PAYABLE_CONTRACT_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.RedGreenPayableContract}.
	 */
	public final static ClassType RGPAYABLE_CONTRACT = new ClassType(Constants.RGPAYABLE_CONTRACT_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Entry}.
	 */
	public final static ClassType ENTRY = new ClassType(Constants.ENTRY_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.View}.
	 */
	public final static ClassType VIEW = new ClassType(io.takamaka.code.instrumentation.Constants.VIEW_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Payable}.
	 */
	public final static ClassType PAYABLE = new ClassType(Constants.PAYABLE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.ThrowsExceptions}.
	 */
	public final static ClassType THROWS_EXCEPTIONS = new ClassType(Constants.THROWS_EXCEPTIONS_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.Bytes32}.
	 */
	public final static ClassType BYTES32 = new ClassType("io.takamaka.code.util.Bytes32");

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageList}.
	 */
	public final static ClassType STORAGE_LIST = new ClassType("io.takamaka.code.util.StorageList");

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageMap}.
	 */
	public final static ClassType STORAGE_MAP = new ClassType("io.takamaka.code.util.StorageMap");

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.RequirementViolationException}.
	 */
	public final static ClassType REQUIREMENT_VIOLATION_EXCEPTION = new ClassType("io.takamaka.code.lang.RequirementViolationException");

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.InsufficientFundsError}.
	 */
	public final static ClassType INSUFFICIENT_FUNDS_ERROR = new ClassType("io.takamaka.code.lang.InsufficientFundsError");
}