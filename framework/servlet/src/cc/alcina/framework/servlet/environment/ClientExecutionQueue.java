package cc.alcina.framework.servlet.environment;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentProtocolServer.MessageToken;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentProtocolServer.MessageToken.Handler;
import cc.alcina.framework.servlet.environment.MessageHandlerServer.FromClientMessageAcceptor;

/*
 * This queue/thread dispatches messages to the client, waiting for its
 * AwaitRemote request
 * 
 * @formatter:off
 * - apart from Startup, only AwaitRemote requests from the client receive messages from the server
 * - asyncEventQueue is "messages from the client to process when available"
 * - syncEventQueue is "single message -  from the server to the client' 
 *  Queries - is it reentrant? can it have multiiple messages?
 *  "
 *  - TODO - add a transport layer which handles message retry (Android sleep/resume)
 * 
 * @formatter:on
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

		AsyncEvent(MessageToken fromClientMessage) {
			this.fromClientMessage = fromClientMessage;
		}

		MessageToken fromClientMessage;

		Runnable runnable;
	}

	boolean finished = false;

	Environment environment;

	ClientExecutionQueue(Environment environment) {
		this.environment = environment;
	}

	void start() {
		String threadName = Ax.format("remcom-env-%s", environment.session.id);
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

	void onLoopException(Exception e) {
		logger.warn("loop exception:\n=====================================",
				e);
	}

	/*
	 * The main client event loop *body* - analagous to the js event loop (note
	 * there are two modes, so there's not a loop per se)
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
	void loop(boolean acceptClientEvents) {
		boolean delta = false;
		synchronized (this) {
			if (fromClientMessageAcceptor != null) {
				Message message = syncEventQueue.poll();
				if (message != null) {
					try {
						fromClientMessageAcceptor.accept(message);
					} catch (Exception e) {
						onLoopException(e);
					}
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
						try {
							asyncEvent.runnable.run();
						} catch (Exception e) {
							onLoopException(e);
						}
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

	void handleFromClientMessage(MessageToken token) {
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
	void handleFromClientMessageSync(MessageToken token) {
		try {
			if (token.request.protocolMessage.sync) {
				environment.runInFrameWithoutSync = true;
			}
			Handler<Environment, Message> messageHandler = (Handler<Environment, Message>) token.messageHandler;
			messageHandler.handle(token, environment,
					token.request.protocolMessage);
			handleFromClientMessageAcceptor(token.messageHandler);
		} catch (Exception e) {
			token.response.putException(e);
			logger.warn(
					"Exception in server queue (in response to invokesync)");
			onLoopException(e);
		} finally {
			environment.runInFrameWithoutSync = false;
		}
		token.latch.countDown();
	}

	FromClientMessageAcceptor fromClientMessageAcceptor;

	void handleFromClientMessageAcceptor(Handler<?, ?> messageHandler) {
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

	void stop() {
		finished = true;
		if (this.fromClientMessageAcceptor != null) {
			this.fromClientMessageAcceptor.accept(null);
		}
		synchronized (this) {
			notifyAll();
		}
	}
}