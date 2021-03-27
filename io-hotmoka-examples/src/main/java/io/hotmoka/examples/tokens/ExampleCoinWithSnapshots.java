package io.hotmoka.examples.tokens;

import static io.takamaka.code.lang.Takamaka.require;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.math.UnsignedBigInteger;
import io.takamaka.code.tokens.ERC20;
import io.takamaka.code.tokens.ERC20WithSnapshots;

/**
 * An example of a token implementation that keeps track of the snapshots performed up to now.
 * Only the owner (deployer) of the contract can create a new snapshot, mint or burn tokens.
 */
public class ExampleCoinWithSnapshots extends ERC20WithSnapshots {
    private final Contract owner;

    /**
     * Sets the initial settings of the coin
     */
    public @FromContract ExampleCoinWithSnapshots() {
    	super(new MyCoin());

    	this.owner = caller();

    	UnsignedBigInteger initial_EXCS_supply = new UnsignedBigInteger("200000");
        UnsignedBigInteger multiplier = new UnsignedBigInteger("10").pow(18);
        ((MyCoin) parent)._mint(owner, initial_EXCS_supply.multiply(multiplier)); // 200'000 EXCS = 200'000 * 10 ^ 18 MiniEs
    }

    private static class MyCoin extends ERC20 {

    	private MyCoin() {
    		super("ExampleCoinWithSnapshots", "EXCWS");
            setDecimals((short) 18); // redundant, just for example
    	}

    	@Override
    	protected void _mint(Contract account, UnsignedBigInteger amount) {
    		super._mint(account, amount);
    	}

    	@Override
    	protected void _burn(Contract account, UnsignedBigInteger amount) {
    		super._burn(account, amount);
    	}

    	@Override
    	protected void _transfer(Contract sender, Contract recipient, UnsignedBigInteger amount) {
    		super._transfer(sender, recipient, amount);
    	}

    	@Override
    	protected void _approve(Contract owner, Contract spender, UnsignedBigInteger amount) {
    		super._approve(owner, spender, amount);
    	}

    	private void myTransferFrom(Contract caller, Contract sender, Contract recipient, UnsignedBigInteger amount) {
    		super.transferFrom(caller, sender, recipient, amount);
    	}
    }

    /**
     * Creates a new snapshot and returns its snapshot id.
     * Note: In this example we have chosen to allow only the owner to take snapshots
     *
     * @return snapshot id
     */
    public @FromContract UnsignedBigInteger yieldSnapshot() {
        snapshot();
        return getCurrentSnapshotId();
    }

    @Override @FromContract
	public boolean transfer(Contract recipient, UnsignedBigInteger amount) {
    	((MyCoin) parent)._transfer(caller(), recipient, amount);
        return true;
	}

	@Override @FromContract
	public boolean approve(Contract spender, UnsignedBigInteger amount) {
		((MyCoin) parent)._approve(caller(), spender, amount);
        return true;
	}

	@Override @FromContract
	public boolean transferFrom(Contract sender, Contract recipient, UnsignedBigInteger amount) {
		((MyCoin) parent).myTransferFrom(caller(), sender, recipient, amount);
		return true;
	}

	/**
	 * Mint tokens.
	 *
	 * @param account recipient of the created tokens
	 * @param amount number of tokens to create
	 */
	public @FromContract void mint(Contract account, UnsignedBigInteger amount) {
	    require(caller() == owner, "lack of permission");
	    ((MyCoin) parent)._mint(account, amount);
	}

	/**
	 * Burn tokens
	 *
	 * @param account source of tokens to burn
	 * @param amount number of tokens to burn
	 */
	public @FromContract void burn(Contract account, UnsignedBigInteger amount) {
	    require(caller() == owner, "lack of permission");
	    ((MyCoin) parent)._burn(account, amount);
	}
}