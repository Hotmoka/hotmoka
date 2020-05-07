package io.hotmoka.tendermint.internal.beans;

public class TendermintTopLevelResult {
	public long height;
	public String hash;
	public Object check_tx;
	public Object deliver_tx;
	public TendermintTxResult tx_result;
	public String tx;
}