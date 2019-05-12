package takamaka.memory;

import takamaka.blockchain.BlockHeader;

public class MemoryBlockHeader extends BlockHeader {

	private static final long serialVersionUID = 2454510542316494098L;

	/**
	 * Builds block header.
	 * 
	 * @param time the time of creation of the block, as returned by {@link java.lang.System#currentTimeMillis()}
	 */
	public MemoryBlockHeader(long time) {
		super(time);
	}
}