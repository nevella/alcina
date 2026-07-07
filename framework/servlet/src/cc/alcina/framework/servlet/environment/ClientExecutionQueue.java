package cc.alcina.framework.servlet.environment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AttachId;
import com.google.gwt.dom.client.LocalDom;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.DatePair;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.Client.RenderState;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer.MessageHistory;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer.MessageHistory.ExecutionQueueState;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer.MessageId;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer.SendChannelId;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer.TransportHistory;
import cc.alcina.framework.servlet.component.romcom.protocol.Mutations;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.BeforeHandled;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.ProcessingException;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.WindowStateUpdate;
import cc.alcina.framework.servlet.component.romcom.protocol.StringProtocol;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentProtocolServer.MessageProcessingToken;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentProtocolServer.RequestToken;
import cc.alcina.framework.servlet.environment.Environment.InvokeException;

/*
 * Note :: explain -why- a DOM needs single-threaded access (it's a case of
 * access restriction to a mutable tree, nothing special about a DOM really)
 * 
 * Note :: (transport) document why everything in Environment is private,
 * exception-that-proves for Beans manifesto private rule is that it's a highly
 * accessed package class with complex access rules
 */
class ClientExecutionQueue implements Runnable {
	class AsyncDispatchable {
		/*
		 * either of these [fromClientMessage, message] should be dispatched
		 * asynchronously, in order
		 */
		MessageProcessingToken fromClientMessage;

		Runnable runnable;

		AsyncDispatchable(Runnable runnable) {
			this.runnable = runnable;
		}

		AsyncDispatchable(MessageProcessingToken fromClientMessage) {
			this.fromClientMessage = fromClientMessage;
		}

		@Override
		public String toString() {
			if (fromClientMessage != null) {
				return fromClientMessage.message.toString();
			} else {
				return runnable.toString();
			}
		}
	}

	/*
	 * Used to track mutation send/return, for client optimisation. the
	 * synchronization is almost certainly not needed - all methods should be
	 * called on the execution thread
	 * 
	 * Note that this is a specialisation of the more general tracking of
	 * last-seen-counterpart-id
	 */
	class MutationMessageData {
		int lastMutationIdBuffered;

		int lastMutationIdUpdateHandled;

		synchronized void onMessageBuffered(Message message) {
			if (message instanceof Mutations
					&& message.messageId.sendChannelId == SendChannelId.SERVER_TO_CLIENT) {
				Mutations mutations = (Mutations) message;
				Client.RenderState.Observable.eventOcurred(
						new MutationsView(mutations),
						Client.RenderState.Observable.EventType.mutations_emitted);
				lastMutationIdBuffered = message.messageId.number;
			}
		}

		class MutationsView {
			Mutations mutations;

			MutationsView(Mutations mutations) {
				this.mutations = mutations;
			}

			@Override
			public String toString() {
				List<AttachId> attachIds = mutations.domMutations.stream()
						.flatMap(m -> m.addedNodes.stream())
						.map(n -> n.attachId).toList();
				return Ax.format("[mutations - attachids] :: %s :: %s",
						mutations.messageId, attachIds);
			}
		}

		synchronized void onMessageHandled(Message message) {
			if (message instanceof Message.WindowStateUpdate) {
				WindowStateUpdate update = (WindowStateUpdate) message;
				if (update.counterpartProcessingId != null
						&& lastMutationIdUpdateHandled < update.counterpartProcessingId.number) {
					lastMutationIdUpdateHandled = update.counterpartProcessingId.number;
					Client.RenderState.Observable.eventOcurred(
							Ax.format("mutation id handled :: %s :: %s",
									lastMutationIdUpdateHandled,
									update.windowState.offsetsDelta
											.toChangesIds()),
							Client.RenderState.Observable.EventType.window_state_update);
				}
			}
		}

		void attachListeners() {
			transportLayer.topicMessageBuffered.add(this::onMessageBuffered);
			topicMessageHandled.add(this::onMessageHandled);
		}

		MutationMessageData() {
		}

		synchronized MutationMessageData snapshot() {
			MutationMessageData result = new MutationMessageData();
			result.lastMutationIdBuffered = lastMutationIdBuffered;
			result.lastMutationIdUpdateHandled = lastMutationIdUpdateHandled;
			return result;
		}

		synchronized boolean areAllUpdatesHandled() {
			return lastMutationIdBuffered == lastMutationIdUpdateHandled;
		}

		@Override
		public String toString() {
			return Ax.format(
					"[MutationMessageData] lastMutationIdBuffered: %s, lastMutationIdUpdateHandled: %s",
					lastMutationIdBuffered, lastMutationIdUpdateHandled);
		}
	}

	/**
	 * <p>
	 * This class executes runnables after any dom mutations have been flushed
	 * to the remote *and* the corresponding browser nodes (and eagerly-synced
	 * offsets, if any) have been returned
	 * 
	 * <p>
	 * If there are no pending mutations, this just amounts to waiting on the
	 * return of the last {@link LocalDom#flush()}
	 * 
	 * <p>
	 * Multiple enqueues of the same Runnable (modulo equals) in a given
	 * dispatch cycle will result in only one runnable being queued
	 */
	class RenderStateImpl implements Client.RenderState.RomcomImpl {
		class QueuedRunnable {
			int messageId;

			Runnable runnable;

			boolean awaitNextMutationId;

			QueuedRunnable(int messageId, boolean awaitNextMutationId,
					Runnable runnable) {
				this.messageId = messageId;
				this.awaitNextMutationId = awaitNextMutationId;
				this.runnable = runnable;
			}

			@Override
			public int hashCode() {
				return messageId ^ runnable.hashCode();
			}

			@Override
			public boolean equals(Object obj) {
				if (obj instanceof QueuedRunnable) {
					QueuedRunnable o = (QueuedRunnable) obj;
					return CommonUtils.equals(messageId, o.messageId, runnable,
							o.runnable);
				} else {
					return false;
				}
			}

			@Override
			public String toString() {
				return Ax.format(
						"[queued-runnable] messageId: %s, awaitNextMutationId: %s, runnable: %s]",
						messageId, awaitNextMutationId,
						NestedName.get(runnable));
			}
		}

		Runnable flushLambda = this::flush;

		LinkedHashSet<QueuedRunnable> pending = new LinkedHashSet<>();

		@Override
		public void enqueue(Runnable runnable) {
			int awaitId = mutationMessageData.lastMutationIdBuffered;
			boolean awaitNextMutationId = false;
			if (hasPending()) {
				if (!transportLayer.sendChannel.hasMessagesPendingDispatch()) {
					// await return of the *next* message (which will include
					// mutations)
					awaitNextMutationId = true;
				}
			} else {
				/*
				 * if no intervening mutations occur, the condition (all ofsets
				 * available) will be met during finally().
				 * 
				 * public Runnable reloadSequenceLambda =
				 * InstanceDistinctLambda.of(this, this::reloadSequence);
				 */
				Client.eventBus().queued().lambda(flushLambda)
						// There will be only one lambda container instance per
						// queue, so the distinct runnable test is correct
						.distinct().dispatch();
			}
			QueuedRunnable queuedRunnable = new QueuedRunnable(awaitId,
					awaitNextMutationId, runnable);
			Client.RenderState.Observable.eventOcurred(queuedRunnable,
					Client.RenderState.Observable.EventType.queued_runnable);
			pending.add(queuedRunnable);
			//
		}

		boolean hasPending() {
			return LocalDom.hasPending()
					|| environment.access().hasPendingMutations()
					|| transportLayer.hasPendingMutations();
		}

		int highestAwaiting = -1;

		/*
		 * queue + remove from pending any runnables which have rendered offset
		 * data available
		 */
		void flush() {
			if (pending.isEmpty()) {
				return;
			}
			MutationMessageData messageDataSnapshot = mutationMessageData
					.snapshot();
			pending.stream().filter(p -> p.awaitNextMutationId == true)
					.forEach(p -> {
						updateHighestAwaiting(p.messageId);
						if (p.messageId < messageDataSnapshot.lastMutationIdBuffered) {
							p.messageId = messageDataSnapshot.lastMutationIdBuffered;
							p.awaitNextMutationId = false;
							Client.RenderState.Observable.eventOcurred(p,
									Client.RenderState.Observable.EventType.runnable_mutation_assigned);
						}
					});
			int awaitingNextMutationIdCount = (int) pending.stream()
					.filter(p -> p.awaitNextMutationId == true).count();
			Iterator<QueuedRunnable> itr = pending.iterator();
			while (itr.hasNext()) {
				QueuedRunnable next = itr.next();
				if (!next.awaitNextMutationId) {
					updateHighestAwaiting(next.messageId);
					if (next.messageId <= messageDataSnapshot.lastMutationIdUpdateHandled) {
						addDispatchable(new AsyncDispatchable(next.runnable));
						Client.RenderState.Observable.eventOcurred(next,
								Client.RenderState.Observable.EventType.emit_dispatchable);
						itr.remove();
					}
				}
			}
		}

		void updateHighestAwaiting(int messageId) {
			if (messageId > highestAwaiting) {
				highestAwaiting = messageId;
			}
		}

		/**
		 * Because rendering is interleaved, it's possible a collating runnable
		 * that requires offsets of X nodes might not have those offsets
		 * available even if it requests that it runs with via
		 * {@link RenderState#queueWithRenderedState}
		 * 
		 * Looping on a check here will proceed until all mutations have
		 * 'settled' - guaranteeing no sync server/client call to
		 * getBoundingClientRects (if the models have the ElementOffsetsRequired
		 * behaviour)
		 */
		@Override
		public boolean areAllOffsetsAccessible() {
			if (hasPending()) {
				return false;
			}
			return mutationMessageData.areAllUpdatesHandled();
		}
	}

	// server-generated runnables or from-client messages to process in order,
	// while not awaiting a synchronous client response
	BlockingQueue<AsyncDispatchable> asyncDispatchQueue = new LinkedBlockingQueue<>();

	boolean finished = false;

	Environment environment;

	Logger logger = LoggerFactory.getLogger(getClass());

	Thread executionThread;

	MessageTransportLayerServer transportLayer;

	/*
	 * not thread-safe (i.e. not necessarily fired from the dispatch thread)
	 */
	Topic<Message> topicMessageHandled = Topic.create();

	MutationMessageData mutationMessageData;

	RenderStateImpl renderStateImpl;

	MessageId highestExecutingCounterpartId;

	Message activeClientMessage;

	class ProcessSnapshot extends Thread {
		ProcessSnapshot() {
			setDaemon(true);
			setPriority(Thread.MAX_PRIORITY);
			setName(executionThread.getName() + "-process-snapshot");
		}

		List<ExecutionQueueState> queueStates = new ArrayList<>();

		synchronized List<ExecutionQueueState> getQueueStates(DatePair range) {
			return queueStates.stream().filter(qs -> range.contains(qs.date))
					.toList();
		}

		@Override
		public void run() {
			try {
				while (!finished) {
					ExecutionQueueState stackshot = new ExecutionQueueState();
					stackshot.trace = SEUtilities
							.dumpStackTrace(executionThread);
					synchronized (this) {
						queueStates.add(stackshot);
					}
					Thread.sleep(50);
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	ProcessSnapshot processSnapshot;

	ClientExecutionQueue(Environment environment) {
		this.environment = environment;
		StringProtocol.Cache cacheFromRegistry = StringProtocol.Cache
				.fromRegistry(environment.getContextPath(),
						environment.getCacheableStringProviderClass());
		transportLayer = new MessageTransportLayerServer(cacheFromRegistry);
		transportLayer.topicMessageReceived.add(this::onMessageReceived);
		mutationMessageData = new MutationMessageData();
		mutationMessageData.attachListeners();
		renderStateImpl = new RenderStateImpl();
	}

	@Override
	public void run() {
		try {
			LooseContext.push();
			environment.fromClientExecutionThreadAccess().beforeEnterContext();
			// this will initialise the outer context (with Document, Window
			// etc). each cycle of loop will execute in a child context
			environment.fromClientExecutionThreadAccess().enterContext();
			while (!finished) {
				pumpMessage();
			}
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			environment.fromClientExecutionThreadAccess().exitContext();
			LooseContext.pop();
		}
	}

	/*
	 * Note the difference in ordering depending on threading - non-queued
	 * execution is required for nested execution order on the execution thread
	 */
	void invoke(Runnable runnable) {
		if (isOnExecutionThread()) {
			runnable.run();
		} else {
			addDispatchable(new AsyncDispatchable(runnable));
		}
	}

	boolean isOnExecutionThread() {
		return Thread.currentThread() == executionThread;
	}

	void start() {
		String threadName = Ax.format("romcom-exec-%s",
				environment.access().getSession().id);
		executionThread = new Thread(this, threadName);
		executionThread.setDaemon(true);
		executionThread.setPriority(Thread.MAX_PRIORITY);
		executionThread.start();
		if (EnvironmentManager.debugRomcomMetrics.is()) {
			processSnapshot = new ProcessSnapshot();
			processSnapshot.start();
		}
	}

	void onMessageReceived(Message message) {
		MessageHandlerServer handler = MessageHandlerServer.forMessage(message);
		MessageProcessingToken token = new MessageProcessingToken(message);
		if (EnvironmentManager.debugRomcomMetrics.is()) {
			message.messageHistory = new MessageHistory();
			message.messageHistory.originatingMessage = message;
			TransportHistory transportHistory = transportLayer.receiveChannel()
					.getTransportHistory(message);
			message.messageHistory.originatingMessageTransportHistory = transportHistory;
		}
		if (handler.isSynchronous()) {
			handleFromClientMessageOnThread(token);
		} else {
			addDispatchable(new AsyncDispatchable(token));
		}
	}

	boolean isRunning() {
		return executionThread != null;
	}

	void onLoopException(Throwable e) {
		if (e instanceof InvokeException.PageHide) {
			// unavoidable
		} else {
			logger.warn(
					"loop exception:\n=====================================",
					e);
		}
		if (isProtocolException(e)) {
			transportLayer.sendMessage(ProcessingException.wrap(e,
					environment.access().isSendFullExceptionMessage()));
		} else {
			GWT.getUncaughtExceptionHandler().onUncaughtException(e);
		}
	}

	/*
	 * 
	 * wip - romcom - in general, exceptions on the execution thread shouldn't
	 * cause the app to stop (they wouldn't, normally, in a js client app). The
	 * exception is when the protocol/comms chain is broken due to a
	 * framework-level comms error, but that's currently something really only
	 * detected on the http/jetty thread - serialization failures, that sorta
	 * whatsit
	 */
	boolean isProtocolException(Throwable e) {
		return false;
	}

	/*
	 * TODO - old docs - The main client event loop *body* - analagous to the js
	 * event loop (note there are two modes, so there's not a loop per se)
	 * 
	 * While waiting for a sync response from the clients, this loop will be
	 * called re-entrantly with acceptClientEvents==false
	 * 
	 * Exceptions are --not-- fatal (message handler exceptions will propagate
	 * to the loop, and are handled in targetted try/catch blocks) - that said,
	 * if the exception occurs during dom mutation (server or client), it might
	 * be better to refresh. WIP
	 * 
	 * TODO - rather than two modes (? two threads?), the logic might be cleaner
	 * if 'acceptClientEvents' is replaced by a check on syncEventQueue
	 * non-empty
	 * 
	 * Note that each dispatchable is processed in an analogue of the GWT Impl
	 * dispatch frame - before processing, any pre-processing tasks are
	 * performed, and post-, any finally tasks (and there may be
	 * many...cascading...) are performed
	 */
	// WIP - replacement for loop
	void pumpMessage() {
		boolean dispatched = false;
		try {
			LooseContext.push();
			AsyncDispatchable dispatchable = asyncDispatchQueue.poll();
			if (dispatchable != null) {
				environment.fromClientExecutionThreadAccess().enter(() -> {
					if (dispatchable.fromClientMessage != null) {
						handleFromClientMessageOnThread(
								dispatchable.fromClientMessage);
					} else {
						runCatchLoopException(dispatchable.runnable);
					}
				});
				dispatched = true;
			}
		} finally {
			LooseContext.pop();
		}
		renderStateImpl.flush();
		if (!dispatched) {
			try {
				// make sure we're waiting on an empty queue
				boolean isFlushing = false;
				synchronized (asyncDispatchQueue) {
					isFlushing = asyncDispatchQueue.isEmpty();
					if (isFlushing) {
						environment.access().flush();
					}
				}
				if (isFlushing) {
					/*
					 * Not in a synchronized block because that can cause
					 * deadlock (conflicting with queueing of incoming messages)
					 */
					transportLayer.flush();
					synchronized (asyncDispatchQueue) {
						if (asyncDispatchQueue.isEmpty()) {
							/*
							 * REVISIT - this ... should be higher? but can
							 * cause ...ahh, maybe render pauses. Clearly, there
							 * shouldn't be a timeout
							 */
							asyncDispatchQueue.wait(20);
						}
					}
				}
			} catch (InterruptedException e) {
				Ax.simpleExceptionOut(e);
			}
		}
	}

	void runCatchLoopException(Runnable runnable) {
		try {
			runnable.run();
		} catch (Throwable e) {
			onLoopException(e);
		}
	}

	/*
	 * Does not await receipt
	 */
	void sendToClient(Message message) {
		transportLayer.sendMessage(message);
	}

	void flush() {
		transportLayer.flush();
	}

	/*
	 * Called from a servlet receiver thread (so not the cl-ex thread).
	 */
	void handleFromClientRequest(RequestToken token) {
		transportLayer.onReceivedToken(token);
		try {
			token.latch.await();
		} catch (InterruptedException e) {
			Ax.simpleExceptionOut(e);
		}
	}

	void addDispatchable(AsyncDispatchable dispatchable) {
		synchronized (asyncDispatchQueue) {
			asyncDispatchQueue.add(dispatchable);
			asyncDispatchQueue.notify();
		}
	}

	/*
	 * Called either from http thread (sync) (which is then accepeted by the
	 * event queue/thread, which is waiting on the sync response ) or from the
	 * environment's event queue/thread. If an http thread is blocking while
	 * waiting for the token to be processed, it will be unblocked by the
	 * token.latch.countDown() call
	 * 
	 * See the similar client block -
	 * cc.alcina.framework.servlet.component.romcom.client.common.logic.
	 * ClientRpc.onMessageReceived(Message message)
	 * 
	 */
	void handleFromClientMessageOnThread(MessageProcessingToken token) {
		try {
			Message message = token.message;
			this.activeClientMessage = message;
			if (!message.sync) {
				highestExecutingCounterpartId = message.messageId;
			}
			BeforeHandled beforeHandled = new Message.BeforeHandled(message);
			beforeHandled.publish();
			if (!beforeHandled.cancelled) {
				MessageHandlerServer messageHandler = MessageHandlerServer
						.forMessage(message);
				messageHandler.handle(token, environment.access(), message);
				topicMessageHandled.publish(token.message);
				new Message.AfterHandled(message).publish();
			}
		} catch (Exception e) {
			logger.warn(
					"Exception in server queue (in response to invokesync)");
			onLoopException(e);
		} finally {
			this.activeClientMessage = null;
		}
		token.messageConsumed();
	}

	void stop() {
		finished = true;
		transportLayer.onFinish();
		synchronized (this) {
			notifyAll();
		}
	}

	public boolean hasPageHideEvent() {
		return asyncDispatchQueue.stream()
				.anyMatch(m -> m.fromClientMessage != null
						&& m.fromClientMessage.message instanceof Message.DomEventMessage
						&& ((Message.DomEventMessage) m.fromClientMessage.message)
								.provideIsPageHide());
	}
}