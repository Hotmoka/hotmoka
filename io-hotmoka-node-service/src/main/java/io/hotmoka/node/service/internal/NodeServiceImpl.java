/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.node.service.internal;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.hotmoka.closeables.api.OnCloseHandler;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.Subscription;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UninitializedNodeException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.messages.AddConstructorCallTransactionMessages;
import io.hotmoka.node.messages.AddConstructorCallTransactionResultMessages;
import io.hotmoka.node.messages.AddGameteCreationTransactionMessages;
import io.hotmoka.node.messages.AddGameteCreationTransactionResultMessages;
import io.hotmoka.node.messages.AddInitializationTransactionMessages;
import io.hotmoka.node.messages.AddInitializationTransactionResultMessages;
import io.hotmoka.node.messages.AddInstanceMethodCallTransactionMessages;
import io.hotmoka.node.messages.AddInstanceMethodCallTransactionResultMessages;
import io.hotmoka.node.messages.AddJarStoreInitialTransactionMessages;
import io.hotmoka.node.messages.AddJarStoreInitialTransactionResultMessages;
import io.hotmoka.node.messages.AddJarStoreTransactionMessages;
import io.hotmoka.node.messages.AddJarStoreTransactionResultMessages;
import io.hotmoka.node.messages.AddStaticMethodCallTransactionMessages;
import io.hotmoka.node.messages.AddStaticMethodCallTransactionResultMessages;
import io.hotmoka.node.messages.EventMessages;
import io.hotmoka.node.messages.GetClassTagMessages;
import io.hotmoka.node.messages.GetClassTagResultMessages;
import io.hotmoka.node.messages.GetConfigMessages;
import io.hotmoka.node.messages.GetConfigResultMessages;
import io.hotmoka.node.messages.GetInfoMessages;
import io.hotmoka.node.messages.GetInfoResultMessages;
import io.hotmoka.node.messages.GetManifestMessages;
import io.hotmoka.node.messages.GetManifestResultMessages;
import io.hotmoka.node.messages.GetPolledResponseMessages;
import io.hotmoka.node.messages.GetPolledResponseResultMessages;
import io.hotmoka.node.messages.GetRequestMessages;
import io.hotmoka.node.messages.GetRequestResultMessages;
import io.hotmoka.node.messages.GetResponseMessages;
import io.hotmoka.node.messages.GetResponseResultMessages;
import io.hotmoka.node.messages.GetStateMessages;
import io.hotmoka.node.messages.GetStateResultMessages;
import io.hotmoka.node.messages.GetTakamakaCodeMessages;
import io.hotmoka.node.messages.GetTakamakaCodeResultMessages;
import io.hotmoka.node.messages.PostConstructorCallTransactionMessages;
import io.hotmoka.node.messages.PostConstructorCallTransactionResultMessages;
import io.hotmoka.node.messages.PostInstanceMethodCallTransactionMessages;
import io.hotmoka.node.messages.PostInstanceMethodCallTransactionResultMessages;
import io.hotmoka.node.messages.PostJarStoreTransactionMessages;
import io.hotmoka.node.messages.PostJarStoreTransactionResultMessages;
import io.hotmoka.node.messages.PostStaticMethodCallTransactionMessages;
import io.hotmoka.node.messages.PostStaticMethodCallTransactionResultMessages;
import io.hotmoka.node.messages.RunInstanceMethodCallTransactionMessages;
import io.hotmoka.node.messages.RunInstanceMethodCallTransactionResultMessages;
import io.hotmoka.node.messages.RunStaticMethodCallTransactionMessages;
import io.hotmoka.node.messages.RunStaticMethodCallTransactionResultMessages;
import io.hotmoka.node.messages.api.AddConstructorCallTransactionMessage;
import io.hotmoka.node.messages.api.AddGameteCreationTransactionMessage;
import io.hotmoka.node.messages.api.AddInitializationTransactionMessage;
import io.hotmoka.node.messages.api.AddInstanceMethodCallTransactionMessage;
import io.hotmoka.node.messages.api.AddJarStoreInitialTransactionMessage;
import io.hotmoka.node.messages.api.AddJarStoreTransactionMessage;
import io.hotmoka.node.messages.api.AddStaticMethodCallTransactionMessage;
import io.hotmoka.node.messages.api.GetClassTagMessage;
import io.hotmoka.node.messages.api.GetConfigMessage;
import io.hotmoka.node.messages.api.GetInfoMessage;
import io.hotmoka.node.messages.api.GetManifestMessage;
import io.hotmoka.node.messages.api.GetPolledResponseMessage;
import io.hotmoka.node.messages.api.GetRequestMessage;
import io.hotmoka.node.messages.api.GetResponseMessage;
import io.hotmoka.node.messages.api.GetStateMessage;
import io.hotmoka.node.messages.api.GetTakamakaCodeMessage;
import io.hotmoka.node.messages.api.PostConstructorCallTransactionMessage;
import io.hotmoka.node.messages.api.PostInstanceMethodCallTransactionMessage;
import io.hotmoka.node.messages.api.PostJarStoreTransactionMessage;
import io.hotmoka.node.messages.api.PostStaticMethodCallTransactionMessage;
import io.hotmoka.node.messages.api.RunInstanceMethodCallTransactionMessage;
import io.hotmoka.node.messages.api.RunStaticMethodCallTransactionMessage;
import io.hotmoka.node.service.api.NodeService;
import io.hotmoka.websockets.api.FailedDeploymentException;
import io.hotmoka.websockets.beans.ExceptionMessages;
import io.hotmoka.websockets.beans.api.RpcMessage;
import io.hotmoka.websockets.server.AbstractRPCWebSocketServer;
import io.hotmoka.websockets.server.AbstractServerEndpoint;
import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * A simple web service that exposes some REST APIs to access an instance of a {@link io.hotmoka.node.api.Node}.
 */
public class NodeServiceImpl extends AbstractRPCWebSocketServer implements NodeService {
	private final static Logger LOGGER = Logger.getLogger(NodeServiceImpl.class.getName());

	/**
	 * The node for which the service is created.
	 */
	private final Node node;

	/**
	 * The sessions connected to this server. We take note of them so that
	 * we know whom to inform when the {@link #node} fires an event.
	 */
	private final Set<Session> eventSessions = ConcurrentHashMap.newKeySet();

	/**
	 * The subscription to the events generated by the exposed node.
	 */
	private final Subscription eventSubscription;

	/**
	 * The prefix used in the log messages;
	 */
	private final String logPrefix;

	/**
	 * We need this intermediate definition since two instances of a method reference
	 * are not the same, nor equals.
	 */
	private final OnCloseHandler this_close = this::close;

	/**
	 * Yields an implementation of a network service that exposes an API to a given Hotmoka node.
	 * 
	 * @param node the Hotmoka node
	 * @param port the port where the service should be opened
	 * @throws FailedDeploymentException if the service cannot be deployed
	 */
    public NodeServiceImpl(Node node, int port) throws FailedDeploymentException {
    	this.node = node;
    	this.logPrefix = "node service(ws://localhost:" + port + "): ";

    	// all events (regardless of their creator) get forwarded to the bound remotes
    	try {
			this.eventSubscription = node.subscribeToEvents(null, this::publishEvent);
		}
    	catch (ClosedNodeException e) {
    		throw new FailedDeploymentException(e);
		}

    	startContainer("", port,
    			GetInfoEndpoint.config(this), GetConfigEndpoint.config(this), GetTakamakaCodeEndpoint.config(this),
    			GetManifestEndpoint.config(this), GetClassTagEndpoint.config(this), GetStateEndpoint.config(this),
    			GetRequestEndpoint.config(this), GetResponseEndpoint.config(this), GetPolledResponseEndpoint.config(this),
    			AddGameteCreationTransactionEndpoint.config(this), AddJarStoreInitialTransactionEndpoint.config(this),
    			AddInitializationTransactionEndpoint.config(this),
    			AddJarStoreTransactionEndpoint.config(this), AddConstructorCallTransactionEndpoint.config(this),
    			AddInstanceMethodCallTransactionEndpoint.config(this), AddStaticMethodCallTransactionEndpoint.config(this),
    			PostConstructorCallTransactionEndpoint.config(this), PostJarStoreTransactionEndpoint.config(this),
    			PostInstanceMethodCallTransactionEndpoint.config(this), PostStaticMethodCallTransactionEndpoint.config(this),
    			RunInstanceMethodCallTransactionEndpoint.config(this), RunStaticMethodCallTransactionEndpoint.config(this),
    			EventEndpoint.config(this)
    	);

    	// if the node gets closed, then this service will be closed as well
    	node.addOnCloseHandler(this_close);

    	LOGGER.info(logPrefix + "published");
    }

    @Override
    protected void closeResources() {
    	try {
			if (eventSubscription != null)
				eventSubscription.close();
		}
		finally {
			try {
				node.removeOnCloseHandler(this_close);
			}
			finally {
				super.closeResources();
			}
		}

    	LOGGER.info(logPrefix + "closed");
    }

    @Override
    protected void processRequest(Session session, RpcMessage message) throws IOException, InterruptedException, TimeoutException {
    	var id = message.getId();

    	try {
    		switch (message) {
    		case GetTakamakaCodeMessage gtcm -> sendObjectAsync(session, GetTakamakaCodeResultMessages.of(node.getTakamakaCode(), id));
    		case GetManifestMessage gmm -> sendObjectAsync(session, GetManifestResultMessages.of(node.getManifest(), id));
    		case GetConfigMessage gcm -> sendObjectAsync(session, GetConfigResultMessages.of(node.getConfig(), id));
    		case GetClassTagMessage gctm -> sendObjectAsync(session, GetClassTagResultMessages.of(node.getClassTag(gctm.getReference()), id));
    		case GetStateMessage gsm -> sendObjectAsync(session, GetStateResultMessages.of(node.getState(gsm.getReference()), id));
    		case GetRequestMessage grm -> sendObjectAsync(session, GetRequestResultMessages.of(node.getRequest(grm.getReference()), id));
    		case GetResponseMessage grm -> sendObjectAsync(session, GetResponseResultMessages.of(node.getResponse(grm.getReference()), id));
    		case GetPolledResponseMessage gprm -> sendObjectAsync(session, GetPolledResponseResultMessages.of(node.getPolledResponse(gprm.getReference()), id));
    		case GetInfoMessage gim -> sendObjectAsync(session, GetInfoResultMessages.of(node.getInfo(), id));
    		case RunInstanceMethodCallTransactionMessage rimctm -> sendObjectAsync(session, RunInstanceMethodCallTransactionResultMessages.of(node.runInstanceMethodCallTransaction(rimctm.getRequest()), id));
    		case RunStaticMethodCallTransactionMessage gsmctm -> sendObjectAsync(session, RunStaticMethodCallTransactionResultMessages.of(node.runStaticMethodCallTransaction(gsmctm.getRequest()), id));
    		case AddInstanceMethodCallTransactionMessage aimctm -> sendObjectAsync(session, AddInstanceMethodCallTransactionResultMessages.of(node.addInstanceMethodCallTransaction(aimctm.getRequest()), id));
    		case AddStaticMethodCallTransactionMessage asmctm -> sendObjectAsync(session, AddStaticMethodCallTransactionResultMessages.of(node.addStaticMethodCallTransaction(asmctm.getRequest()), id));
    		case AddConstructorCallTransactionMessage acctm -> sendObjectAsync(session, AddConstructorCallTransactionResultMessages.of(node.addConstructorCallTransaction(acctm.getRequest()), id));
    		case AddJarStoreTransactionMessage ajstm -> sendObjectAsync(session, AddJarStoreTransactionResultMessages.of(node.addJarStoreTransaction(ajstm.getRequest()), id));
    		case AddGameteCreationTransactionMessage agctm -> sendObjectAsync(session, AddGameteCreationTransactionResultMessages.of(node.addGameteCreationTransaction(agctm.getRequest()), id));
    		case AddJarStoreInitialTransactionMessage ajsitm -> sendObjectAsync(session, AddJarStoreInitialTransactionResultMessages.of(node.addJarStoreInitialTransaction(ajsitm.getRequest()), id));
    		case AddInitializationTransactionMessage aitm -> {
    			node.addInitializationTransaction(aitm.getRequest());
    			sendObjectAsync(session, AddInitializationTransactionResultMessages.of(id));
    		}
    		case PostConstructorCallTransactionMessage pcctm -> sendObjectAsync(session, PostConstructorCallTransactionResultMessages.of(node.postConstructorCallTransaction(pcctm.getRequest()).getReferenceOfRequest(), id));
    		case PostInstanceMethodCallTransactionMessage pimctm -> sendObjectAsync(session, PostInstanceMethodCallTransactionResultMessages.of(node.postInstanceMethodCallTransaction(pimctm.getRequest()).getReferenceOfRequest(), id));
    		case PostStaticMethodCallTransactionMessage psmctm -> sendObjectAsync(session, PostStaticMethodCallTransactionResultMessages.of(node.postStaticMethodCallTransaction(psmctm.getRequest()).getReferenceOfRequest(), id));
    		case PostJarStoreTransactionMessage pjstm -> sendObjectAsync(session, PostJarStoreTransactionResultMessages.of(node.postJarStoreTransaction(pjstm.getRequest()).getReferenceOfRequest(), id));
    		default -> LOGGER.warning(logPrefix + "unexpected message of type " + message.getClass().getName());
    		}
    	}
    	catch (UninitializedNodeException | UnknownReferenceException | TransactionRejectedException | TransactionException | CodeExecutionException e) {
    		sendObjectAsync(session, ExceptionMessages.of(e, id));
    	}
    	catch (ClosedNodeException e) {
    		LOGGER.warning(logPrefix + "request processing failed since the serviced node has been closed: " + e.getMessage());
    	}
    }

	private void addSession(Session session) {
		eventSessions.add(session);
		LOGGER.info(logPrefix + "bound a new remote through session " + session.getId());
	}

	private void removeSession(Session session) {
		eventSessions.remove(session);
		LOGGER.info(logPrefix + "unbound the remote at session " + session.getId());
	}

	protected void onEvent(StorageReference creator, StorageReference event, Session session) {
		LOGGER.info(logPrefix + "publishing event " + event + " with creator " + creator);

		try {
			sendObjectAsync(session, EventMessages.of(creator, event));
		}
		catch (IOException e) {
			LOGGER.log(Level.SEVERE, logPrefix + "cannot send to session: it might be closed: " + e.getMessage());
		}
	};

	/**
	 * The endpoint used to propagate the events to the bound remotes.
	 */
	public static class EventEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			getServer().addSession(session);
	    }

	    @Override
		public void onClose(Session session, CloseReason closeReason) {
	    	getServer().removeSession(session);
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, EventEndpoint.class, EVENTS_ENDPOINT, EventMessages.Encoder.class);
		}
	}

	protected void onGetInfo(GetInfoMessage message, Session session) {
		LOGGER.info(logPrefix + "received a " + GET_INFO_ENDPOINT + " request");
		scheduleRequest(session, message);
	};

	public static class GetInfoEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			var server = getServer();
			addMessageHandler(session, (GetInfoMessage message) -> server.onGetInfo(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, GetInfoEndpoint.class, GET_INFO_ENDPOINT, GetInfoMessages.Decoder.class, GetInfoResultMessages.Encoder.class);
		}
	}

	protected void onGetConfig(GetConfigMessage message, Session session) {
		LOGGER.info(logPrefix + "received a " + GET_CONFIG_ENDPOINT + " request");
		scheduleRequest(session, message);
	};

	public static class GetConfigEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			var server = getServer();
			addMessageHandler(session, (GetConfigMessage message) -> server.onGetConfig(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, GetConfigEndpoint.class, GET_CONFIG_ENDPOINT, GetConfigMessages.Decoder.class, GetConfigResultMessages.Encoder.class);
		}
	}

	protected void onGetTakamakaCode(GetTakamakaCodeMessage message, Session session) {
		LOGGER.info(logPrefix + "received a " + GET_TAKAMAKA_CODE_ENDPOINT + " request");
		scheduleRequest(session, message);
	};

	public static class GetTakamakaCodeEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			var server = getServer();
			addMessageHandler(session, (GetTakamakaCodeMessage message) -> server.onGetTakamakaCode(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, GetTakamakaCodeEndpoint.class, GET_TAKAMAKA_CODE_ENDPOINT,
				GetTakamakaCodeMessages.Decoder.class, GetTakamakaCodeResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	protected void onGetManifest(GetManifestMessage message, Session session) {
		LOGGER.info(logPrefix + "received a " + GET_MANIFEST_ENDPOINT + " request");
		scheduleRequest(session, message);
	};

	public static class GetManifestEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			var server = getServer();
			addMessageHandler(session, (GetManifestMessage message) -> server.onGetManifest(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, GetManifestEndpoint.class, GET_MANIFEST_ENDPOINT,
				GetManifestMessages.Decoder.class, GetManifestResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	protected void onGetClassTag(GetClassTagMessage message, Session session) {
		LOGGER.info(logPrefix + "received a " + GET_CLASS_TAG_ENDPOINT + " request");
		scheduleRequest(session, message);
	};

	public static class GetClassTagEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			var server = getServer();
			addMessageHandler(session, (GetClassTagMessage message) -> server.onGetClassTag(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, GetClassTagEndpoint.class, GET_CLASS_TAG_ENDPOINT,
				GetClassTagMessages.Decoder.class, GetClassTagResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	protected void onGetState(GetStateMessage message, Session session) {
		LOGGER.info(logPrefix + "received a " + GET_STATE_ENDPOINT + " request");
		scheduleRequest(session, message);
	};

	public static class GetStateEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			var server = getServer();
			addMessageHandler(session, (GetStateMessage message) -> server.onGetState(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, GetStateEndpoint.class, GET_STATE_ENDPOINT,
				GetStateMessages.Decoder.class, GetStateResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	protected void onGetRequest(GetRequestMessage message, Session session) {
		LOGGER.info(logPrefix + "received a " + GET_REQUEST_ENDPOINT + " request");
		scheduleRequest(session, message);
	};

	public static class GetRequestEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			var server = getServer();
			addMessageHandler(session, (GetRequestMessage message) -> server.onGetRequest(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, GetRequestEndpoint.class, GET_REQUEST_ENDPOINT,
				GetRequestMessages.Decoder.class, GetRequestResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	protected void onGetResponse(GetResponseMessage message, Session session) {
		LOGGER.info(logPrefix + "received a " + GET_RESPONSE_ENDPOINT + " request");
		scheduleRequest(session, message);
	};

	public static class GetResponseEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			var server = getServer();
			addMessageHandler(session, (GetResponseMessage message) -> server.onGetResponse(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, GetResponseEndpoint.class, GET_RESPONSE_ENDPOINT,
				GetResponseMessages.Decoder.class, GetResponseResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	protected void onGetPolledResponse(GetPolledResponseMessage message, Session session) {
		LOGGER.info(logPrefix + "received a " + GET_POLLED_RESPONSE_ENDPOINT + " request");
		scheduleRequest(session, message);
	};

	public static class GetPolledResponseEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			var server = getServer();
			addMessageHandler(session, (GetPolledResponseMessage message) -> server.onGetPolledResponse(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, GetPolledResponseEndpoint.class, GET_POLLED_RESPONSE_ENDPOINT,
				GetPolledResponseMessages.Decoder.class, GetPolledResponseResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	protected void onRunInstanceMethodCallTransaction(RunInstanceMethodCallTransactionMessage message, Session session) {
		LOGGER.info(logPrefix + "received a " + RUN_INSTANCE_METHOD_CALL_TRANSACTION_ENDPOINT + " request");
		scheduleRequest(session, message);
	};

	public static class RunInstanceMethodCallTransactionEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			var server = getServer();
			addMessageHandler(session, (RunInstanceMethodCallTransactionMessage message) -> server.onRunInstanceMethodCallTransaction(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, RunInstanceMethodCallTransactionEndpoint.class, RUN_INSTANCE_METHOD_CALL_TRANSACTION_ENDPOINT,
				RunInstanceMethodCallTransactionMessages.Decoder.class, RunInstanceMethodCallTransactionResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	protected void onRunStaticMethodCallTransaction(RunStaticMethodCallTransactionMessage message, Session session) {
		LOGGER.info(logPrefix + "received a " + RUN_STATIC_METHOD_CALL_TRANSACTION_ENDPOINT + " request");
		scheduleRequest(session, message);
	};

	public static class RunStaticMethodCallTransactionEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			var server = getServer();
			addMessageHandler(session, (RunStaticMethodCallTransactionMessage message) -> server.onRunStaticMethodCallTransaction(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, RunStaticMethodCallTransactionEndpoint.class, RUN_STATIC_METHOD_CALL_TRANSACTION_ENDPOINT,
				RunStaticMethodCallTransactionMessages.Decoder.class, RunStaticMethodCallTransactionResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	protected void onAddInstanceMethodCallTransaction(AddInstanceMethodCallTransactionMessage message, Session session) {
		LOGGER.info(logPrefix + "received an " + ADD_INSTANCE_METHOD_CALL_TRANSACTION_ENDPOINT + " request");
		scheduleRequest(session, message);
	};

	public static class AddInstanceMethodCallTransactionEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			var server = getServer();
			addMessageHandler(session, (AddInstanceMethodCallTransactionMessage message) -> server.onAddInstanceMethodCallTransaction(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, AddInstanceMethodCallTransactionEndpoint.class, ADD_INSTANCE_METHOD_CALL_TRANSACTION_ENDPOINT,
				AddInstanceMethodCallTransactionMessages.Decoder.class, AddInstanceMethodCallTransactionResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	protected void onAddStaticMethodCallTransaction(AddStaticMethodCallTransactionMessage message, Session session) {
		LOGGER.info(logPrefix + "received an " + ADD_STATIC_METHOD_CALL_TRANSACTION_ENDPOINT + " request");
		scheduleRequest(session, message);
	};

	public static class AddStaticMethodCallTransactionEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			var server = getServer();
			addMessageHandler(session, (AddStaticMethodCallTransactionMessage message) -> server.onAddStaticMethodCallTransaction(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, AddStaticMethodCallTransactionEndpoint.class, ADD_STATIC_METHOD_CALL_TRANSACTION_ENDPOINT,
				AddStaticMethodCallTransactionMessages.Decoder.class, AddStaticMethodCallTransactionResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	protected void onAddConstructorCallTransaction(AddConstructorCallTransactionMessage message, Session session) {
		LOGGER.info(logPrefix + "received an " + ADD_CONSTRUCTOR_CALL_TRANSACTION_ENDPOINT + " request");
		scheduleRequest(session, message);
	};

	public static class AddConstructorCallTransactionEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			var server = getServer();
			addMessageHandler(session, (AddConstructorCallTransactionMessage message) -> server.onAddConstructorCallTransaction(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, AddConstructorCallTransactionEndpoint.class, ADD_CONSTRUCTOR_CALL_TRANSACTION_ENDPOINT,
				AddConstructorCallTransactionMessages.Decoder.class, AddConstructorCallTransactionResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	protected void onAddJarStoreTransaction(AddJarStoreTransactionMessage message, Session session) {
		LOGGER.info(logPrefix + "received an " + ADD_JAR_STORE_TRANSACTION_ENDPOINT + " request");
		scheduleRequest(session, message);
	};

	public static class AddJarStoreTransactionEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			var server = getServer();
			addMessageHandler(session, (AddJarStoreTransactionMessage message) -> server.onAddJarStoreTransaction(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, AddJarStoreTransactionEndpoint.class, ADD_JAR_STORE_TRANSACTION_ENDPOINT,
				AddJarStoreTransactionMessages.Decoder.class, AddJarStoreTransactionResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	protected void onAddGameteCreationTransaction(AddGameteCreationTransactionMessage message, Session session) {
		LOGGER.info(logPrefix + "received an " + ADD_JAR_STORE_INITIAL_TRANSACTION_ENDPOINT + " request");
		scheduleRequest(session, message);
	};

	public static class AddGameteCreationTransactionEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			var server = getServer();
			addMessageHandler(session, (AddGameteCreationTransactionMessage message) -> server.onAddGameteCreationTransaction(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, AddGameteCreationTransactionEndpoint.class, ADD_GAMETE_CREATION_TRANSACTION_ENDPOINT,
				AddGameteCreationTransactionMessages.Decoder.class, AddGameteCreationTransactionResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	protected void onAddJarStoreInitialTransaction(AddJarStoreInitialTransactionMessage message, Session session) {
		LOGGER.info(logPrefix + "received an " + ADD_JAR_STORE_INITIAL_TRANSACTION_ENDPOINT + " request");
		scheduleRequest(session, message);
	};

	public static class AddJarStoreInitialTransactionEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			var server = getServer();
			addMessageHandler(session, (AddJarStoreInitialTransactionMessage message) -> server.onAddJarStoreInitialTransaction(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, AddJarStoreInitialTransactionEndpoint.class, ADD_JAR_STORE_INITIAL_TRANSACTION_ENDPOINT,
				AddJarStoreInitialTransactionMessages.Decoder.class, AddJarStoreInitialTransactionResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	protected void onAddInitializationTransaction(AddInitializationTransactionMessage message, Session session) {
		LOGGER.info(logPrefix + "received an " + ADD_INITIALIZATION_TRANSACTION_ENDPOINT + " request");
		scheduleRequest(session, message);
	};

	public static class AddInitializationTransactionEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			var server = getServer();
			addMessageHandler(session, (AddInitializationTransactionMessage message) -> server.onAddInitializationTransaction(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, AddGameteCreationTransactionEndpoint.class, ADD_INITIALIZATION_TRANSACTION_ENDPOINT,
				AddInitializationTransactionMessages.Decoder.class, AddInitializationTransactionResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	protected void onPostConstructorCallTransaction(PostConstructorCallTransactionMessage message, Session session) {
		LOGGER.info(logPrefix + "received a " + POST_CONSTRUCTOR_CALL_TRANSACTION_ENDPOINT + " request");
		scheduleRequest(session, message);
	};

	public static class PostConstructorCallTransactionEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			var server = getServer();
			addMessageHandler(session, (PostConstructorCallTransactionMessage message) -> server.onPostConstructorCallTransaction(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, PostConstructorCallTransactionEndpoint.class, POST_CONSTRUCTOR_CALL_TRANSACTION_ENDPOINT,
				PostConstructorCallTransactionMessages.Decoder.class, PostConstructorCallTransactionResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	protected void onPostInstanceMethodCallTransaction(PostInstanceMethodCallTransactionMessage message, Session session) {
		LOGGER.info(logPrefix + "received a " + POST_INSTANCE_METHOD_CALL_TRANSACTION_ENDPOINT + " request");
		scheduleRequest(session, message);
	};

	public static class PostInstanceMethodCallTransactionEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			var server = getServer();
			addMessageHandler(session, (PostInstanceMethodCallTransactionMessage message) -> server.onPostInstanceMethodCallTransaction(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, PostInstanceMethodCallTransactionEndpoint.class, POST_INSTANCE_METHOD_CALL_TRANSACTION_ENDPOINT,
				PostInstanceMethodCallTransactionMessages.Decoder.class, PostInstanceMethodCallTransactionResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	protected void onPostStaticMethodCallTransaction(PostStaticMethodCallTransactionMessage message, Session session) {
		LOGGER.info(logPrefix + "received a " + POST_STATIC_METHOD_CALL_TRANSACTION_ENDPOINT + " request");
		scheduleRequest(session, message);
	};

	public static class PostStaticMethodCallTransactionEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			var server = getServer();
			addMessageHandler(session, (PostStaticMethodCallTransactionMessage message) -> server.onPostStaticMethodCallTransaction(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, PostStaticMethodCallTransactionEndpoint.class, POST_STATIC_METHOD_CALL_TRANSACTION_ENDPOINT,
				PostStaticMethodCallTransactionMessages.Decoder.class, PostStaticMethodCallTransactionResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

	protected void onPostJarStoreTransaction(PostJarStoreTransactionMessage message, Session session) {
		LOGGER.info(logPrefix + "received a " + POST_JAR_STORE_TRANSACTION_ENDPOINT + " request");
		scheduleRequest(session, message);
	};

	public static class PostJarStoreTransactionEndpoint extends AbstractServerEndpoint<NodeServiceImpl> {

		@Override
	    public void onOpen(Session session, EndpointConfig config) {
			var server = getServer();
			addMessageHandler(session, (PostJarStoreTransactionMessage message) -> server.onPostJarStoreTransaction(message, session));
	    }

		private static ServerEndpointConfig config(NodeServiceImpl server) {
			return simpleConfig(server, PostJarStoreTransactionEndpoint.class, POST_JAR_STORE_TRANSACTION_ENDPOINT,
				PostJarStoreTransactionMessages.Decoder.class, PostJarStoreTransactionResultMessages.Encoder.class, ExceptionMessages.Encoder.class);
		}
	}

    private void publishEvent(StorageReference creator, StorageReference event) {
		eventSessions.forEach(session -> onEvent(creator, event, session));
    }
}