package cc.alcina.framework.servlet.environment;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.ProcessingException;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentProtocolServer.MessageProcessingToken;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentProtocolServer.RequestToken;
import cc.alcina.framework.servlet.environment.Environment.InvokeException;

/*
 * Note :: explain -why- a DOM needs single-threaded access (it's a case of
 * access restriction to a mutable tree, nothing special about a DOM really)
 * 
 * Note :: (transport) document why everything in Environment is private,
 * exception-that-proves for Beans manfiesto private rule is that it's a highly
 * accessed package class with complex access rules
 */
class ClientExecutionQueue implements Runnable {
	/*
	 * either of these should be dispatched asynchronously, in order
	 */
	class AsyncDispatchable {
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

	// server-generated runnables or from-client messages to process in order,
	// while not awaiting a synchronous client response
	BlockingQueue<AsyncDispatchable> asyncDispatchQueue = new LinkedBlockingQueue<>();

	boolean finished = false;

	Environment environment;

	Logger logger = LoggerFactory.getLogger(getClass());

	Thread executionThread;

	MessageTransportLayerServer transportLayer;

	ClientExecutionQueue(Environment environment) {
		this.environment = environment;
		transportLayer = new MessageTransportLayerServer();
		transportLayer.topicMessageReceived.add(this::onMessageReceived);
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
	}

	void onMessageReceived(Message message) {
		MessageHandlerServer handler = MessageHandlerServer.forMessage(message);
		MessageProcessingToken token = new MessageProcessingToken(message);
		if (handler.isSynchronous()) {
			handler.handle(token, environment.access(), message);
		} else {
			addDispatchable(new AsyncDispatchable(token));
		}
	}

	boolean isRunning() {
		return executionThread != null;
	}

	void onLoopException(Exception e) {
		transportLayer.sendMessage(ProcessingException.wrap(e,
				environment.access().isSendFullExceptionMessage()));
		if (e instanceof InvokeException.PageHide) {
			// unavoidable
		} else {
			logger.warn(
					"loop exception:\n=====================================",
					e);
		}
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
						asyncDispatchQueue.wait(1000);
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
		} catch (Exception e) {
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
	 * TODO - romcom - remove?
	 * 
	 * 
	 */
	void handleFromClientMessageOnThread(MessageProcessingToken token) {
		try {
			MessageHandlerServer messageHandler = MessageHandlerServer
					.forMessage(token.message);
			messageHandler.handle(token, environment.access(), token.message);
		} catch (Exception e) {
			logger.warn(
					"Exception in server queue (in response to invokesync)");
			onLoopException(e);
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