/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

/**
 * The name of this package ends in _abci instead of .abci since
 * otherwise Eclipse complains about a conflict with the name of the
 * io.hotmoka.tendermint package inside the io-hotmoka-tendermint module.
 * This is surprising, since subpackages are not split packages.
 */

package io.hotmoka.tendermint.abci;

import io.grpc.stub.StreamObserver;
import io.hotmoka.node.api.NodeException;
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
 * A Tendermint interface that links a Tendermint process to a Tendermint application.
 * It implements a set of handlers that Tendermint calls to notify events.
 */
public abstract class ABCI {

	/**
	 * Creates the Tendermint interface.
	 */
	protected ABCI() {}

	/**
	 * The connected application.
	 */
	final ABCIApplicationGrpc.ABCIApplicationImplBase service = new ABCIApplication();

	/**
	 * The implementation of the application. It forwards the calls to the abstract
	 * method of the {@link ABCI}.
	 */
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
			try {
				responseObserver.onNext(ABCI.this.info(request));
				responseObserver.onCompleted();
			}
			catch (NodeException e) {
				responseObserver.onError(e); // TODO: do the same for the other methods of the ABCI
			}
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

	/**
	 * Executes a request to initialize a chain.
	 * 
	 * @param request the request
	 * @return the response
	 */
	protected abstract ResponseInitChain initChain(RequestInitChain request);

	/**
	 * Executes a echo message request.
	 * 
	 * @param request the request
	 * @return the response
	 */
	protected abstract ResponseEcho echo(RequestEcho request);

	/**
	 * Executes a chain info request.
	 * 
	 * @param request the request
	 * @return the response
	 * @throws NodeException 
	 */
	protected abstract ResponseInfo info(RequestInfo request) throws NodeException;

	/**
	 * Executes a preliminary check about the validity of a transaction request.
	 * 
	 * @param request the request
	 * @return the response
	 */
	protected abstract ResponseCheckTx checkTx(RequestCheckTx request);

	/**
	 * Called when the construction of a block begins.
	 * 
	 * @param request the request
	 * @return the response
	 */
	protected abstract ResponseBeginBlock beginBlock(RequestBeginBlock request);

	/**
	 * Executes a transaction request.
	 * 
	 * @param request the request
	 * @return the response
	 */
	protected abstract ResponseDeliverTx deliverTx(RequestDeliverTx request);

	/**
	 * Called when the construction of a block ends.
	 * 
	 * @param request the request
	 * @return the response
	 */
	protected abstract ResponseEndBlock endBlock(RequestEndBlock request);

	/**
	 * Called when a new block gets committed.
	 * 
	 * @param request the request
	 * @return the response
	 */
	protected abstract ResponseCommit commit(RequestCommit request);

	/**
	 * Executes a query request.
	 * 
	 * @param request the request
	 * @return the response
	 */
	protected abstract ResponseQuery query(RequestQuery request);

	/**
	 * Executes a flush request.
	 * 
	 * @param request the request
	 * @return the response
	 */
	protected abstract ResponseFlush flush(RequestFlush request);
}