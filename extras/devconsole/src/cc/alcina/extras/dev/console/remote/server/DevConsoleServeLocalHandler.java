package cc.alcina.extras.dev.console.remote.server;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import cc.alcina.framework.entity.ResourceUtilities;

public class DevConsoleServeLocalHandler extends AbstractHandler {
    @SuppressWarnings("unused")
    private DevConsoleRemote devConsoleRemote;

    public DevConsoleServeLocalHandler(DevConsoleRemote devConsoleRemote) {
        this.devConsoleRemote = devConsoleRemote;
    }

    @Override
    public void handle(String target, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String path = request.getQueryString().replace("%20", " ");
        File file = new File(path);
        byte[] bytes = ResourceUtilities.readFileToByteArray(file);
        response.setContentType(
                new MimeTypes().getMimeByExtension(file.getPath()));
        ResourceUtilities.writeStreamToStream(new ByteArrayInputStream(bytes),
                response.getOutputStream());
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
    }
}
