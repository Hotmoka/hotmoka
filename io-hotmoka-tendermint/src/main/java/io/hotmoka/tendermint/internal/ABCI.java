package io.hotmoka.tendermint.internal;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;

import io.grpc.stub.StreamObserver;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import types.ABCIApplicationGrpc;
import types.Types.RequestBeginBlock;
import types.Types.RequestCheckTx;
import types.Types.RequestCommit;
import types.Types.RequestDeliverTx;
import types.Types.RequestEcho;
import types.Types.RequestEndBlock;
import types.Types.RequestFlush;
import types.Types.RequestInfo;
import types.Types.RequestInitChain;
import types.Types.RequestQuery;
import types.Types.RequestSetOption;
import types.Types.ResponseBeginBlock;
import types.Types.ResponseCheckTx;
import types.Types.ResponseCommit;
import types.Types.ResponseDeliverTx;
import types.Types.ResponseEcho;
import types.Types.ResponseEndBlock;
import types.Types.ResponseFlush;
import types.Types.ResponseInfo;
import types.Types.ResponseInitChain;
import types.Types.ResponseQuery;
import types.Types.ResponseQuery.Builder;
import types.Types.ResponseSetOption;

class ABCI extends ABCIApplicationGrpc.ABCIApplicationImplBase {
	private final TendermintBlockchainImpl node;

	private final static Logger logger = LoggerFactory.getLogger(ABCI.class);

	/**
    * The current time of the blockchain, set at the time of creation of each block.
    */
    private long now;

    ABCI(TendermintBlockchainImpl node) {
    	this.node = node;
    }

    @Override
    public void echo(RequestEcho req, StreamObserver<ResponseEcho> responseObserver) {
        ResponseEcho resp = ResponseEcho.newBuilder().build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    @Override
    public void info(RequestInfo req, StreamObserver<ResponseInfo> responseObserver) {
    	//System.out.print("[info");
        ResponseInfo resp = ResponseInfo.newBuilder()
        		//.setLastBlockAppHash(ByteString.copyFrom(new byte[8])) // LastBlockAppHash
        		.setLastBlockHeight(node.state.getNumberOfCommits()).build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
        //System.out.println("]");
    }

    @Override
    public void setOption(RequestSetOption req, StreamObserver<ResponseSetOption> responseObserver) {
    	System.out.print("[setOption");
        ResponseSetOption resp = ResponseSetOption.newBuilder().build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
        System.out.println("]");
    }

    @Override
    public void checkTx(RequestCheckTx tendermintRequest, StreamObserver<ResponseCheckTx> responseObserver) {
    	//System.out.print("[checkTx");
        ByteString tx = tendermintRequest.getTx();
        ResponseCheckTx.Builder responseBuilder = ResponseCheckTx.newBuilder();

        TransactionRequest<?> request = null;
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(tx.toByteArray()))) {
        	request = TransactionRequest.from(ois);
        	node.checkTransaction(request);
        	responseBuilder.setCode(0);
        }
        catch (TransactionRejectedException e) {
        	logger.error("checkTx failed", e);
        	responseBuilder.setCode(1);
        	responseBuilder.setInfo(e.getMessage());
        	if (request != null)
        		node.releaseWhoWasWaitingFor(request);
		}
        catch (Throwable t) {
        	logger.error("checkTx failed", t);
        	responseBuilder.setCode(2);
        	responseBuilder.setInfo(t.toString());
        	if (request != null)
        		node.releaseWhoWasWaitingFor(request);
		}

        ResponseCheckTx resp = responseBuilder
                //.setGasWanted(1)
                .build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
        //System.out.println("]");
    }

    @Override
    public void initChain(RequestInitChain req, StreamObserver<ResponseInitChain> responseObserver) {
    	//System.out.print("[initChain");
        ResponseInitChain resp = ResponseInitChain.newBuilder().build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
        //System.out.println("]");
    }

    @Override
    public void beginBlock(RequestBeginBlock req, StreamObserver<ResponseBeginBlock> responseObserver) {
    	//System.out.print("[beginBlock");
    	node.state.beginTransaction();
    	//System.out.println("committed " + req.getHeader().getNumTxs() + " transactions");
        Timestamp time = req.getHeader().getTime();
    	now = time.getSeconds() * 1_000L + time.getNanos() / 1_000_000L;
        ResponseBeginBlock resp = ResponseBeginBlock.newBuilder().build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
        //System.out.print("]");
    }

    //private long deliverTxTime;

    @Override
    public synchronized void deliverTx(RequestDeliverTx tendermintRequest, StreamObserver<ResponseDeliverTx> responseObserver) {
    	ByteString tx = tendermintRequest.getTx();
        //System.out.print("[deliverTx ");
        //long start = System.currentTimeMillis();
        ResponseDeliverTx.Builder responseBuilder = ResponseDeliverTx.newBuilder();

        TransactionRequest<?> request = null;
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(tx.toByteArray()))) {
        	request = TransactionRequest.from(ois);
        	TransactionReference next = node.nextAndIncrement();
        	node.deliverTransaction(node.checkTransaction(request), next);
        	responseBuilder.setCode(0);
        	responseBuilder.setData(ByteString.copyFrom(next.toByteArray()));
        }
        catch (TransactionRejectedException e) {
        	logger.error("deliverTx failed", e);
        	responseBuilder.setCode(1);
        	responseBuilder.setInfo(e.getMessage());
        }
        catch (Throwable t) {
        	logger.error("deliverTx failed", t);
        	responseBuilder.setCode(2);
        	responseBuilder.setInfo(t.toString());
        }

        ResponseDeliverTx resp = responseBuilder.build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();

        if (request != null)
    		node.releaseWhoWasWaitingFor(request);
        //deliverTxTime += (System.currentTimeMillis() - start);
        //System.out.println(deliverTxTime + "]");
    }

    @Override
    public void endBlock(RequestEndBlock req, StreamObserver<ResponseEndBlock> responseObserver) {
    	//System.out.print("[endBlock");
        ResponseEndBlock resp = ResponseEndBlock.newBuilder().build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
        //System.out.print("]");
    }

    @Override
    public void commit(RequestCommit req, StreamObserver<ResponseCommit> responseObserver) {
    	//System.out.print("[commit");
    	node.state.commitTransaction();
        ResponseCommit resp = ResponseCommit.newBuilder()
                //.setData(ByteString.copyFrom(new byte[8])) // hash of the Merkle root of the application state
                .build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
        //System.out.print("]");
    }

    @Override
    public void query(RequestQuery req, StreamObserver<ResponseQuery> responseObserver) {
    	System.out.print("[query");
        /*byte[] k = req.getData().toByteArray();
        byte[] v = getPersistedValue(k);*/
        Builder builder = ResponseQuery.newBuilder();
        /*if (v == null)
            builder.setLog("does not exist");
        else {*/
            builder.setLog("exists");
            //builder.setKey(ByteString.copyFrom(k));
            //builder.setValue(ByteString.copyFrom(v));
        //}
        ResponseQuery resp = builder.build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
        System.out.println("]");
    }

    @Override
    public void flush(RequestFlush request, StreamObserver<ResponseFlush> responseObserver) {
    	//System.out.println("flush");
    	ResponseFlush resp = ResponseFlush.newBuilder().build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    /**
     * Yields the current time of the blockchain, set at the time of
     * creation of each block.
     * 
     * @return the current time
     */
    long getNow() {
		return now;
	}
}