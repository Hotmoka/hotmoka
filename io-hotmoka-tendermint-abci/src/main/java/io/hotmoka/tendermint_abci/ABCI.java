/**
 * The name of this package ends in _abci instead of .abci since
 * otherwise Eclipse complains about a conflict with the name of the
 * io.hotmoka.tendermint package inside the io-hotmoka-tendermint module.
 * This is surprising, since subpackages are not split packages.
 */

package io.hotmoka.tendermint_abci;

import io.grpc.stub.StreamObserver;
import tendermint.abci.ABCIApplicationGrpc;
import tendermint.abci.Types.RequestBeginBlock;
import tendermint.abci.Types.RequestCheckTx;
import tendermint.abci.Types.RequestCommit;
import tendermint.abci.Types.RequestDeliverTx;
import tendermint.abci.Types.RequestEcho;
import tendermint.abci.Types.RequestEndBlock;
import tendermint.abci.Types.RequestFlush;
import tendermint.abci.Types.RequestInfo;
import tendermint.abci.Types.RequestInitChain;
import tendermint.abci.Types.RequestQuery;
import tendermint.abci.Types.ResponseBeginBlock;
import tendermint.abci.Types.ResponseCheckTx;
import tendermint.abci.Types.ResponseCommit;
import tendermint.abci.Types.ResponseDeliverTx;
import tendermint.abci.Types.ResponseEcho;
import tendermint.abci.Types.ResponseEndBlock;
import tendermint.abci.Types.ResponseFlush;
import tendermint.abci.Types.ResponseInfo;
import tendermint.abci.Types.ResponseInitChain;
import tendermint.abci.Types.ResponseQuery;

/**
 * The Tendermint interface that links a Tendermint process to a Tendermint application.
 * It implements a set of handlers that Tendermint calls to notify events.
 */
public abstract class ABCI {

	final ABCIApplicationGrpc.ABCIApplicationImplBase service = new ABCIApplication();

	private class ABCIApplication extends ABCIApplicationGrpc.ABCIApplicationImplBase {

		@Override
		public final void initChain(RequestInitChain request, StreamObserver<ResponseInitChain> responseObserver) {
	    	responseObserver.onNext(ABCI.this.initChain(request));
	    	responseObserver.onCompleted();
	    }

		@Override
	    public final void echo(RequestEcho request, StreamObserver<ResponseEcho> responseObserver) {
	        responseObserver.onNext(ABCI.this.echo(request));
	        responseObserver.onCompleted();
	    }

		@Override
	    public final void info(RequestInfo request, StreamObserver<ResponseInfo> responseObserver) {
	        responseObserver.onNext(ABCI.this.info(request));
	        responseObserver.onCompleted();
	    }

		@Override
	    public final void checkTx(RequestCheckTx request, StreamObserver<ResponseCheckTx> responseObserver) {
	        responseObserver.onNext(ABCI.this.checkTx(request));
	        responseObserver.onCompleted();
	    }

		@Override
	    public final void beginBlock(RequestBeginBlock request, StreamObserver<ResponseBeginBlock> responseObserver) {
	        responseObserver.onNext(ABCI.this.beginBlock(request));
	        responseObserver.onCompleted();
	    }

		@Override
	    public final void deliverTx(RequestDeliverTx request, StreamObserver<ResponseDeliverTx> responseObserver) {
	        responseObserver.onNext(ABCI.this.deliverTx(request));
	        responseObserver.onCompleted();
	    }

		@Override
	    public final void endBlock(RequestEndBlock request, StreamObserver<ResponseEndBlock> responseObserver) {
	    	responseObserver.onNext(ABCI.this.endBlock(request));
	        responseObserver.onCompleted();
	    }

		@Override
	    public final void commit(RequestCommit request, StreamObserver<ResponseCommit> responseObserver) {
	        responseObserver.onNext(ABCI.this.commit(request));
	        responseObserver.onCompleted();
	    }

		@Override
	    public final void query(RequestQuery request, StreamObserver<ResponseQuery> responseObserver) {
	        responseObserver.onNext(ABCI.this.query(request));
	        responseObserver.onCompleted();
	    }

		@Override
	    public final void flush(RequestFlush request, StreamObserver<ResponseFlush> responseObserver) {
	        responseObserver.onNext(ABCI.this.flush(request));
	        responseObserver.onCompleted();
	    }
	}

	protected abstract ResponseInitChain initChain(RequestInitChain request);

	protected abstract ResponseEcho echo(RequestEcho request);

	protected abstract ResponseInfo info(RequestInfo request);

	protected abstract ResponseCheckTx checkTx(RequestCheckTx request);

	protected abstract ResponseBeginBlock beginBlock(RequestBeginBlock request);

	protected abstract ResponseDeliverTx deliverTx(RequestDeliverTx request);

	protected abstract ResponseEndBlock endBlock(RequestEndBlock request);

	protected abstract ResponseCommit commit(RequestCommit request);

	protected abstract ResponseQuery query(RequestQuery request);

	protected abstract ResponseFlush flush(RequestFlush request);
}