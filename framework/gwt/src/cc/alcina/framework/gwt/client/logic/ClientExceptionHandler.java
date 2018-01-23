package cc.alcina.framework.gwt.client.logic;

import java.util.Set;
import java.util.Stack;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.event.shared.UmbrellaException;
import com.google.gwt.user.client.History;

import cc.alcina.framework.common.client.csobjects.WebException;
import cc.alcina.framework.common.client.logic.domain.HiliHelper;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ClientBase;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.browsermod.BrowserMod;

public class ClientExceptionHandler implements UncaughtExceptionHandler {
    public static final String PRE_STACKTRACE_MARKER = "\n\t-----\n";

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

    private Stack<UncaughtExceptionHandler> handlerStack = new Stack<GWT.UncaughtExceptionHandler>();

    public String extraInfoForExceptionText() {
        ClientInstance clientInstance = PermissionsManager.get()
                .getClientInstance();
        long clientInstanceId = HiliHelper.getIdOrZero(clientInstance);
        String extraInfo = "\n\nUser agent: " + BrowserMod.getUserAgent()
                + "\nHistory token: " + History.getToken()
                + "\nPermutation name: " + GWT.getPermutationStrongName()
                + CommonUtils.formatJ("\nUser name/id/cli: [%s/%s/%s]",
                        PermissionsManager.get().getUserName(),
                        PermissionsManager.get().getUserId(), clientInstanceId);
        return extraInfo;
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

    public void onUncaughtException(Throwable e) {
        // TODO - 3.02
        GWT.log("Uncaught exception escaped", e);
        if (GWT.isScript()) {
            Registry.impl(ClientNotifications.class).showError(e);
        }
    }

    public void pop() {
        GWT.setUncaughtExceptionHandler(handlerStack.pop());
    }

    public void push(UncaughtExceptionHandler handler) {
        handlerStack.push(GWT.getUncaughtExceptionHandler());
        GWT.setUncaughtExceptionHandler(handler);
    }

    public Throwable wrapException(Throwable e) {
        StringBuilder errorBuffer = new StringBuilder();
        unrollUmbrella(e, errorBuffer);
        errorBuffer.append(extraInfoForExceptionText());
        return new WebException(
                "(Wrapped GWT exception) : " + errorBuffer.toString());
    }

    protected String getStandardErrorText() {
        return "Sorry for the inconvenience, and we'll fix this problem as soon as possible."
                + ""
                + " If the problem recurs, please try refreshing your browser";
    }
}
