package cc.alcina.framework.gwt.client.logic;

import java.util.Set;
import java.util.Stack;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.event.shared.UmbrellaException;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;

import cc.alcina.framework.common.client.csobjects.WebException;
import cc.alcina.framework.common.client.logic.domain.EntityHelper;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.browsermod.BrowserMod;

public class ClientExceptionHandler
		implements UncaughtExceptionHandler, ClosingHandler {
	public static final String PRE_STACKTRACE_MARKER = "\n\t-----\n";

	public static ClientExceptionHandler get() {
		return Registry.impl(ClientExceptionHandler.class);
	}

	public static void unrollUmbrella(Throwable e, StringBuilder errorBuffer) {
		if (e instanceof UmbrellaException) {
			errorBuffer.append("\nUmbrellaException");
			UmbrellaException umbrella = (UmbrellaException) e;
			Set<Throwable> causes = umbrella.getCauses();
			for (Throwable cause : causes) {
				unrollUmbrella(cause, errorBuffer);
			}
		} else {
			errorBuffer.append("\n" + e.toString());
			errorBuffer.append(PRE_STACKTRACE_MARKER);
			StackTraceElement[] stackTrace = e.getStackTrace();
			for (StackTraceElement stackTraceElement : stackTrace) {
				errorBuffer.append(stackTraceElement);
				errorBuffer.append("\n");
			}
			if (e.getCause() != null && e.getCause() != e) {
				errorBuffer.append("\nCaused by:-----\n");
				unrollUmbrella(e.getCause(), errorBuffer);
			}
		}
	}

	protected boolean windowClosing = false;

	private Stack<UncaughtExceptionHandler> handlerStack = new Stack<GWT.UncaughtExceptionHandler>();

	protected Set<Class<? extends Exception>> muteExceptionsOfClass = AlcinaCollections
			.newUniqueSet();

	public ClientExceptionHandler() {
		Window.addWindowClosingHandler(this);
	}

	public String extraInfoForExceptionText() {
		ClientInstance clientInstance = Permissions.get().getClientInstance();
		long clientInstanceId = EntityHelper.getIdOrZero(clientInstance);
		String extraInfo = "\n\nUser agent: " + BrowserMod.getUserAgent()
				+ "\nHistory token: " + History.getToken()
				+ "\nPermutation name: " + GWT.getPermutationStrongName()
				+ Ax.format("\nUser name/id/cli: [%s/%s/%s]",
						Permissions.get().getUserName(),
						Permissions.get().getUserId(), clientInstanceId);
		return extraInfo;
	}

	protected String getStandardErrorText() {
		return "Sorry for the inconvenience, and we'll fix this problem as soon as possible."
				+ ""
				+ " If the problem recurs, please try refreshing your browser";
	}

	/*
	 * called at points in code where the exception's not actually uncaught, but
	 * might as well give to centralised "arggh" handler
	 */
	public void handleException(Throwable e) {
		onUncaughtException(e);
	}

	public boolean handleNetworkException(Throwable e) {
		return false;
	}

	public boolean isWindowClosing() {
		return this.windowClosing;
	}

	public void muteExceptionsOfClass(Class<? extends Exception> clazz) {
		muteExceptionsOfClass.add(clazz);
	}

	@Override
	public void onUncaughtException(Throwable e) {
		GWT.log("Uncaught exception escaped", e);
		Registry.impl(ClientNotifications.class).showError(e);
	}

	@Override
	public void onWindowClosing(ClosingEvent event) {
		windowClosing = true;
	}

	public void pop() {
		GWT.setUncaughtExceptionHandler(handlerStack.pop());
	}

	public void push(UncaughtExceptionHandler handler) {
		handlerStack.push(GWT.getUncaughtExceptionHandler());
		GWT.setUncaughtExceptionHandler(handler);
	}

	public void setWindowClosing(boolean windowClosing) {
		this.windowClosing = windowClosing;
	}

	public Throwable wrapException(Throwable e) {
		StringBuilder errorBuffer = new StringBuilder();
		unrollUmbrella(e, errorBuffer);
		errorBuffer.append(extraInfoForExceptionText());
		return new WebException(
				"(Wrapped GWT exception) : " + errorBuffer.toString());
	}
}
