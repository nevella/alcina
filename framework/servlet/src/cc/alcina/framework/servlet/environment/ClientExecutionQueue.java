package cc.alcina.framework.servlet.environment;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentProtocolServer.MessageToken;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentProtocolServer.MessageToken.Handler;
import cc.alcina.framework.servlet.environment.MessageHandlerServer.ToClientMessageAcceptor;
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
 * 
 * @formatter:on
 * 
 * - Terminology - 'execution thread' - analagous to the single thread of js dispatch in a 
 * browser, the only thread with access to the (local) DOM. The thread is stored in #executionThread
 */

/*
 * Note :: explain -why- a DOM needs single-threaded access (it's a case of
 * access restriction to a mutable tree, nothing special about a DOM really)
 * 
 * Note :: (transport) document why everything in Environment is private,
 * exception-that-proves for Beans manfiesto private rule is that it's a highly
 * accessed package class with complex access rules
 */
class ClientExecutionQueue implements Runnable {
	BlockingQueue<Message> toClientQueue = new LinkedBlockingQueue<>();

	// server-generated runnables or from-client messages to process in order,
	// while not awaiting a synchronous client response
	BlockingQueue<AsyncDispatchable> asyncDispatchQueue = new LinkedBlockingQueue<>();

	/*
	 * either of these should be dispatched asynchronously, in order
	 */
	class AsyncDispatchable {
		AsyncDispatchable(Runnable runnable) {
			this.runnable = runnable;
		}

		AsyncDispatchable(MessageToken fromClientMessage) {
			this.fromClientMessage = fromClientMessage;
		}

		MessageToken fromClientMessage;

		Runnable runnable;
	}

	boolean finished = false;

	Environment environment;

	MessageTransport messageTransport;

	ClientExecutionQueue(Environment environment) {
		this.environment = environment;
		this.messageTransport = new MessageTransport();
	}

	/*
	 * Note the difference in ordering depending on threading - non-queued
	 * execution is required for nested execution order on the execution thread
	 */
	void invoke(Runnable runnable) {
		if (inOnExecutionThread()) {
			runnable.run();
		} else {
			addDispatchable(new AsyncDispatchable(runnable));
		}
	}

	boolean inOnExecutionThread() {
		return Thread.currentThread() == executionThread;
	}

	void start() {
		String threadName = Ax.format("romcom-exec-%s",
				environment.access().getSession().id);
		executionThread = new Thread(this, threadName);
		executionThread.setDaemon(true);
		executionThread.start();
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

	boolean isRunning() {
		return executionThread != null;
	}

	void onLoopException(Exception e) {
		logger.warn("loop exception:\n=====================================",
				e);
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
		boolean delta = false;
		try {
			LooseContext.push();
			AsyncDispatchable dispatchable = asyncDispatchQueue.poll();
			if (dispatchable != null) {
				environment.fromClientExecutionThreadAccess().enter(() -> {
					if (dispatchable.fromClientMessage != null) {
						handleFromClientMessageOnThread(
								dispatchable.fromClientMessage);
					} else {
						try {
							dispatchable.runnable.run();
						} catch (Exception e) {
							onLoopException(e);
						}
					}
				});
				delta = true;
			}
		} finally {
			LooseContext.pop();
		}
		if (!delta) {
			try {
				// make sure we're waiting on an empty queue
				synchronized (asyncDispatchQueue) {
					if (asyncDispatchQueue.isEmpty()) {
						asyncDispatchQueue.wait(1000);
					}
				}
			} catch (InterruptedException e) {
				Ax.simpleExceptionOut(e);
			}
		}
	}

	Logger logger = LoggerFactory.getLogger(getClass());

	/*
	 * Does not await receipt
	 */
	void sendToClient(Message message) {
		toClientQueue.add(message);
		messageTransport.conditionallySendToClient();
	}

	/*
	 * Called from a servlet receiver thread (so not the cl-ex thread).
	 */
	void handleFromClientMessage(MessageToken token) {
		Message protocolMessage = token.request.protocolMessage;
		if (protocolMessage.sync) {
			handleFromClientMessageOnThread(token);
		} else {
			addDispatchable(new AsyncDispatchable(token));
			try {
				token.latch.await();
			} catch (InterruptedException e) {
				Ax.simpleExceptionOut(e);
			}
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
	 */
	void handleFromClientMessageOnThread(MessageToken token) {
		try {
			Handler<Environment.Access, Message> messageHandler = (Handler<Environment.Access, Message>) token.messageHandler;
			messageHandler.handle(token, environment.access(),
					token.request.protocolMessage);
		} catch (Exception e) {
			token.response.putException(e);
			logger.warn(
					"Exception in server queue (in response to invokesync)");
			onLoopException(e);
		}
		token.messageConsumed();
	}

	private Thread executionThread;

	void stop() {
		finished = true;
		messageTransport.flushAcceptor();
		// FIXME - transport - flush all handlers
		synchronized (this) {
			notifyAll();
		}
	}

	void registerToClientMessageAcceptor(ToClientMessageAcceptor acceptor) {
		messageTransport.registerAcceptor(acceptor);
	}

	class MessageTransport {
		ToClientMessageAcceptor acceptor;

		synchronized void registerAcceptor(ToClientMessageAcceptor acceptor) {
			flushAcceptor();
			this.acceptor = acceptor;
			conditionallySendToClient();
		}

		synchronized void conditionallySendToClient() {
			if (acceptor != null) {
				Message message = toClientQueue.poll();
				if (message != null) {
					this.acceptor.accept(message);
					this.acceptor = null;
				}
			}
		}

		synchronized void flushAcceptor() {
			if (this.acceptor != null) {
				this.acceptor.accept(null);
				this.acceptor = null;
			}
		}
	}
}