package cc.alcina.framework.servlet.dom;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.server.MessageHandlers.FromClientMessageAcceptor;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentProtocolServer.MessageHandlerServer;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentProtocolServer.MessageHandlingToken;

/*
 * This queue/thread dispatches messages to the client, waiting for its
 * AwaitRemote request
 * 
 * It also
 */
class ClientExecutionQueue implements Runnable {
	BlockingQueue<Message> syncEventQueue = new LinkedBlockingQueue<>();

	BlockingQueue<AsyncEvent> asyncEventQueue = new LinkedBlockingQueue<>();

	/*
	 * either of these should be dispatched asynchronously, in order
	 */
	class AsyncEvent {
		AsyncEvent(Runnable runnable) {
			this.runnable = runnable;
		}

		AsyncEvent(MessageHandlingToken fromClientMessage) {
			this.fromClientMessage = fromClientMessage;
		}

		MessageHandlingToken fromClientMessage;

		Runnable runnable;
	}

	boolean finished = false;

	Environment environment;

	ClientExecutionQueue(Environment environment) {
		this.environment = environment;
	}

	void start() {
		String threadName = Ax.format("remcom-env-%s",
				environment.credentials.id);
		Thread thread = new Thread(this, threadName);
		thread.setDaemon(true);
		thread.start();
	}

	@Override
	public void run() {
		while (!finished) {
			loop(true);
		}
	}

	/*
	 * The main client event loop - analagous to the js event loop
	 * 
	 * While waiting for a sync response from the clients, this loop will be
	 * called re-entrantly with acceptClientEvents==false
	 */
	void loop(boolean acceptClientEvents) {
		boolean delta = false;
		synchronized (this) {
			if (fromClientMessageAcceptor != null) {
				Message message = syncEventQueue.poll();
				if (message != null) {
					fromClientMessageAcceptor.accept(message);
					fromClientMessageAcceptor = null;
					logger.debug("fromClientMessageAcceptor :: consumed");
					delta = true;
				}
			}
			if (acceptClientEvents) {
				AsyncEvent asyncEvent = asyncEventQueue.poll();
				if (asyncEvent != null) {
					if (asyncEvent.fromClientMessage != null) {
						handleFromClientMessageSync(
								asyncEvent.fromClientMessage);
					} else {
						asyncEvent.runnable.run();
					}
					delta = true;
				}
			}
			if (!delta) {
				try {
					waiting.set(true);
					// FIXME - an infinite wait is problematic here - but when
					// should the lock be released?
					wait(1000);
				} catch (InterruptedException e) {
					Ax.simpleExceptionOut(e);
				} finally {
					waiting.set(false);
				}
			}
		}
	}

	AtomicBoolean waiting = new AtomicBoolean();

	Logger logger = LoggerFactory.getLogger(getClass());

	/*
	 * Does not await receipt
	 */
	void send(Message message) {
		syncEventQueue.add(message);
		if (waiting.get()) {
			synchronized (this) {
				notify();
			}
		}
	}

	void handleFromClientMessage(MessageHandlingToken token) {
		if (token.request.protocolMessage.sync) {
			handleFromClientMessageSync(token);
		} else {
			synchronized (this) {
				asyncEventQueue.add(new AsyncEvent(token));
				notify();
			}
			try {
				token.latch.await();
			} catch (InterruptedException e) {
				Ax.simpleExceptionOut(e);
			}
		}
	}

	/*
	 * Called either from http thread (sync) (which is then accepeted by the
	 * event queue/thread, which is waiting on the sync response ) or from the
	 * environment's event queue/thread. If an http thread is blocking while
	 * waiting for the token to be processed, it will be unblocked by the
	 * token.latch.countDown() call
	 * 
	 * The 'environment.runInFrameWithoutSync' is necessary to prevent deadlocks
	 * when multiple client events are emitted, at least one of which causes a
	 * server->client sync call
	 */
	private void handleFromClientMessageSync(MessageHandlingToken token) {
		try {
			if (token.request.protocolMessage.sync) {
				environment.runInFrameWithoutSync = true;
			}
			token.messageHandler.handle(token.request, token.response,
					environment, token.request.protocolMessage);
			handleFromClientMessageAcceptor(token.messageHandler);
		} finally {
			environment.runInFrameWithoutSync = false;
		}
		token.latch.countDown();
	}

	FromClientMessageAcceptor fromClientMessageAcceptor;

	void acceptServerMessage(Message message) {
		synchronized (this) {
			syncEventQueue.add(message);
			notify();
		}
	}

	void handleFromClientMessageAcceptor(MessageHandlerServer messageHandler) {
		if (messageHandler instanceof FromClientMessageAcceptor) {
			synchronized (this) {
				// flush the existing acceptor, if any
				if (this.fromClientMessageAcceptor != null) {
					this.fromClientMessageAcceptor.accept(null);
				}
				this.fromClientMessageAcceptor = (FromClientMessageAcceptor) messageHandler;
				logger.debug("fromClientMessageAcceptor :: registered");
				notify();
			}
		}
	}

	public void onInvokedSync() {
		while (syncEventQueue.size() > 0) {
			loop(false);
		}
	}

	void submit(Runnable runnable) {
		synchronized (this) {
			asyncEventQueue.add(new AsyncEvent(runnable));
			notify();
		}
	}
}