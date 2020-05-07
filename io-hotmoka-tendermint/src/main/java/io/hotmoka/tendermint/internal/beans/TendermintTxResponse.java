package io.hotmoka.tendermint.internal.beans;

public class TendermintTxResponse {
	public String jsonrpc;
	public long id;
	public TendermintTopLevelResult result;
	public String error;
}