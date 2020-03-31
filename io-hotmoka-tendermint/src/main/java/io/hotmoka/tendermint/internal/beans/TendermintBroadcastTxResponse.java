package io.hotmoka.tendermint.internal.beans;

public class TendermintBroadcastTxResponse {
	public String jsonrpc;
	public long id;
	public TendermintTopLevelResult result;
	public TxError error;
}