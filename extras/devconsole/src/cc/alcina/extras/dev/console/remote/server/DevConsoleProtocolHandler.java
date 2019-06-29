package cc.alcina.extras.dev.console.remote.server;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import cc.alcina.extras.dev.console.remote.protocol.RemoteConsoleConsoleChanges;
import cc.alcina.extras.dev.console.remote.protocol.RemoteConsoleRequest;
import cc.alcina.extras.dev.console.remote.protocol.RemoteConsoleRequest.RemoteConsoleRequestType;
import cc.alcina.extras.dev.console.remote.protocol.RemoteConsoleResponse;
import cc.alcina.extras.dev.console.remote.protocol.RemoteConsoleStartupModel;
import cc.alcina.extras.dev.console.remote.server.DevConsoleRemote.ConsoleRecord;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaBeanSerializer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.ResourceUtilities;

public class DevConsoleProtocolHandler extends AbstractHandler {
    private DevConsoleRemote devConsoleRemote;

    public DevConsoleProtocolHandler(DevConsoleRemote devConsoleRemote) {
        this.devConsoleRemote = devConsoleRemote;
    }

    @Override
    public void handle(String target, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        try {
            LooseContext.push();
            response.setContentType("application/json");
            String requestJson = ResourceUtilities
                    .readStreamToString(request.getInputStream());
            if (requestJson.length() > 0) {
                RemoteConsoleRequest consoleRequest = AlcinaBeanSerializer
                        .deserializeHolder(requestJson);
                LooseContext.set(
                        DevConsoleRemote.CONTEXT_CALLER_CLIENT_INSTANCE_UID,
                        consoleRequest.getClientInstanceUid());
                MethodHandler methodHandler = Registry.get()
                        .lookupImplementation(MethodHandler.class,
                                consoleRequest.getType(), "type");
                RemoteConsoleResponse consoleResponse = methodHandler
                        .handle(consoleRequest, this);
                response.getWriter().write(
                        AlcinaBeanSerializer.serializeHolder(consoleResponse));
            }
            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);
        } finally {
            LooseContext.pop();
        }
    }

    @RegistryLocation(registryPoint = MethodHandler.class)
    public static abstract class MethodHandler {
        public abstract RemoteConsoleRequestType getType();

        protected abstract RemoteConsoleResponse handle(
                RemoteConsoleRequest consoleRequest,
                DevConsoleProtocolHandler devConsoleProtocolHandler);
    }

    public static class MethodHandler_ARROW_DOWN extends MethodHandler {
        @Override
        public RemoteConsoleRequestType getType() {
            return RemoteConsoleRequestType.ARROW_DOWN;
        }

        @Override
        protected RemoteConsoleResponse handle(
                RemoteConsoleRequest consoleRequest,
                DevConsoleProtocolHandler devConsoleProtocolHandler) {
            devConsoleProtocolHandler.devConsoleRemote.doCommandHistoryDelta(1);
            RemoteConsoleResponse response = new RemoteConsoleResponse();
            return response;
        }
    }

    public static class MethodHandler_ARROW_UP extends MethodHandler {
        @Override
        public RemoteConsoleRequestType getType() {
            return RemoteConsoleRequestType.ARROW_UP;
        }

        @Override
        protected RemoteConsoleResponse handle(
                RemoteConsoleRequest consoleRequest,
                DevConsoleProtocolHandler devConsoleProtocolHandler) {
            devConsoleProtocolHandler.devConsoleRemote
                    .doCommandHistoryDelta(-1);
            RemoteConsoleResponse response = new RemoteConsoleResponse();
            return response;
        }
    }

    public static class MethodHandler_DO_COMMAND extends MethodHandler {
        @Override
        public RemoteConsoleRequestType getType() {
            return RemoteConsoleRequestType.DO_COMMAND;
        }

        @Override
        protected RemoteConsoleResponse handle(
                RemoteConsoleRequest consoleRequest,
                DevConsoleProtocolHandler devConsoleProtocolHandler) {
            devConsoleProtocolHandler.devConsoleRemote
                    .performCommand(consoleRequest.getCommandString());
            RemoteConsoleResponse response = new RemoteConsoleResponse();
            return response;
        }
    }

    public static class MethodHandler_GET_RECORDS extends MethodHandler {
        @Override
        public RemoteConsoleRequestType getType() {
            return RemoteConsoleRequestType.GET_RECORDS;
        }

        @Override
        protected RemoteConsoleResponse handle(
                RemoteConsoleRequest consoleRequest,
                DevConsoleProtocolHandler devConsoleProtocolHandler) {
            RemoteConsoleResponse response = new RemoteConsoleResponse();
            Object outputReadyNotifier = devConsoleProtocolHandler.devConsoleRemote.outputReadyNotifier;
            synchronized (outputReadyNotifier) {
                try {
                    if (!devConsoleProtocolHandler.devConsoleRemote.hasRecords(
                            consoleRequest.getClientInstanceUid())) {
                        outputReadyNotifier.wait(1 * TimeConstants.ONE_HOUR_MS);
                    }
                } catch (InterruptedException e) {
                    //
                }
                List<ConsoleRecord> records = devConsoleProtocolHandler.devConsoleRemote
                        .takeRecords(consoleRequest.getClientInstanceUid());
                RemoteConsoleConsoleChanges changes = new RemoteConsoleConsoleChanges();
                response.setChanges(changes);
                StringBuilder builder = new StringBuilder();
                for (ConsoleRecord consoleRecord : records) {
                    String text = consoleRecord.text;
                    if (Ax.notBlank(text)) {
                        text = text.replaceAll(
                                "(?:^|\\s)(/(?:tmp|Users|~).+?)(?:\n|\t|$)",
                                "<a href='/serve-local.do?$1' target='_blank'>$1</a>");
                        String escaped = text.contains("<a href=") ? text
                                : StringEscapeUtils.escapeHtml(text);
                        String span = Ax.format("<span class='%s'>%s</span>",
                                consoleRecord.style.toString().toLowerCase(),
                                escaped);
                        builder.append(span);
                    }
                    if (consoleRecord.clear) {
                        changes.setClearOutput(true);
                        builder.setLength(0);
                    }
                    if (consoleRecord.commandText != null) {
                        changes.setCommandLine(consoleRecord.commandText);
                    }
                }
                changes.setOutputHtml(builder.toString());
            }
            return response;
        }
    }

    public static class MethodHandler_STARTUP extends MethodHandler {
        @Override
        public RemoteConsoleRequestType getType() {
            return RemoteConsoleRequestType.STARTUP;
        }

        @Override
        protected RemoteConsoleResponse handle(
                RemoteConsoleRequest consoleRequest,
                DevConsoleProtocolHandler devConsoleProtocolHandler) {
            RemoteConsoleResponse response = new RemoteConsoleResponse();
            RemoteConsoleStartupModel startupModel = new RemoteConsoleStartupModel();
            String appName = devConsoleProtocolHandler.devConsoleRemote
                    .getAppName();
            startupModel.setAppName(appName);
            response.setStartupModel(startupModel);
            return response;
        }
    }
}
