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

/**
 * The Tendermint interface that links a Hotmoka Tendermint node to a Tendermint process.
 * It implements a set of handlers that Tendermint calls to notify events.
 */
class ABCI extends ABCIApplicationGrpc.ABCIApplicationImplBase {

	/**
	 * The Tendermint blockchain linked to Tendermint.
	 */
	private final TendermintBlockchainImpl node;

	private final static Logger logger = LoggerFactory.getLogger(ABCI.class);

    /**
     * Builds the Tendermint ABCI interface that executes Takamaka transactions.
     * 
     * @param node
     */
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
        ResponseInfo resp = ResponseInfo.newBuilder()
        		//.setLastBlockAppHash(ByteString.copyFrom(new byte[8])) // LastBlockAppHash
        		.setLastBlockHeight(node.getNumberOfCommits()).build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    @Override
    public void setOption(RequestSetOption req, StreamObserver<ResponseSetOption> responseObserver) {
        ResponseSetOption resp = ResponseSetOption.newBuilder().build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    @Override
    public void checkTx(RequestCheckTx tendermintRequest, StreamObserver<ResponseCheckTx> responseObserver) {
        ByteString tx = tendermintRequest.getTx();
        ResponseCheckTx.Builder responseBuilder = ResponseCheckTx.newBuilder();

        TransactionRequest<?> request = null;
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(tx.toByteArray()))) {
        	request = TransactionRequest.from(ois);
        	node.checkTransaction(request);
        	responseBuilder.setCode(0);
        }
        catch (TransactionRejectedException e) {
        	logger.info("Failed to check transaction request", e);
        	responseBuilder.setCode(1);
        	responseBuilder.setInfo(e.getMessage());
        	if (request != null)
        		node.releaseWhoWasWaitingFor(request);
		}
        catch (Throwable t) {
        	logger.error("Failed to check transaction request", t);
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
    }

    @Override
    public void initChain(RequestInitChain req, StreamObserver<ResponseInitChain> responseObserver) {
        ResponseInitChain resp = ResponseInitChain.newBuilder().build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    @Override
    public void beginBlock(RequestBeginBlock req, StreamObserver<ResponseBeginBlock> responseObserver) {
    	Timestamp time = req.getHeader().getTime();
    	node.beginBlock(time.getSeconds() * 1_000L + time.getNanos() / 1_000_000L);
        ResponseBeginBlock resp = ResponseBeginBlock.newBuilder().build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    @Override
    public synchronized void deliverTx(RequestDeliverTx tendermintRequest, StreamObserver<ResponseDeliverTx> responseObserver) {
    	ByteString tx = tendermintRequest.getTx();
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
        	logger.info("Failed delivering transaction", e);
        	responseBuilder.setCode(1);
        	responseBuilder.setInfo(e.getMessage());
        }
        catch (Throwable t) {
        	logger.error("Failed delivering transaction", t);
        	responseBuilder.setCode(2);
        	responseBuilder.setInfo(t.toString());
        }

        ResponseDeliverTx resp = responseBuilder.build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();

        if (request != null)
        	node.releaseWhoWasWaitingFor(request);
    }

    @Override
    public void endBlock(RequestEndBlock req, StreamObserver<ResponseEndBlock> responseObserver) {
        ResponseEndBlock resp = ResponseEndBlock.newBuilder().build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    @Override
    public void commit(RequestCommit req, StreamObserver<ResponseCommit> responseObserver) {
    	node.commitBlock();
        ResponseCommit resp = ResponseCommit.newBuilder()
                //.setData(ByteString.copyFrom(new byte[8])) // hash of the Merkle root of the application state
                .build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    @Override
    public void query(RequestQuery req, StreamObserver<ResponseQuery> responseObserver) {
        Builder builder = ResponseQuery.newBuilder().setLog("nop");
        ResponseQuery resp = builder.build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    @Override
    public void flush(RequestFlush request, StreamObserver<ResponseFlush> responseObserver) {
    	ResponseFlush resp = ResponseFlush.newBuilder().build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }
}