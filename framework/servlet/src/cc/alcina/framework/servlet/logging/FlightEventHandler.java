package cc.alcina.framework.servlet.logging;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.util.ZipUtil;

/*
 * Provides common implementation for jetty/servlet flightevent handling
 */
public class FlightEventHandler {
	private static final String USAGE = "Usage: /flight?action=<list|download>&filter=(filter-regex) - \n"
			+ "note params are optional, default action is list\n\n===================\n\n";

	String eventRootPath;

	public FlightEventHandler(String eventRootPath) {
		this.eventRootPath = eventRootPath;
	}

	public synchronized void handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		File tmpFolder = SEUtilities.getChildFile(
				new File(eventRootPath).getParentFile(),
				Ax.format("tmp.flight-event-assembly-%s",
						Ax.timestamp(new Date())));
		Ax.out("tmpfolder: %s", tmpFolder);
		try {
			String actionParam = request.getParameter("action");
			String sessionParam = request.getParameter("session");
			actionParam = Ax.blankTo(actionParam, "list");
			String sessionFolder = Ax.blankTo(sessionParam, ".+");
			Action action = Action.valueOf(actionParam);
			File eventsFolder = new File(eventRootPath);
			Stream<File> stream = Stream.of(eventsFolder.listFiles())
					.sorted(Comparator.comparing(File::lastModified));
			switch (action) {
			case list: {
				FormatBuilder format = new FormatBuilder();
				format.line(USAGE);
				stream.forEach(f -> format.line(
						"<a href='?action=download&session=%s'>%s</a>%s%s",
						f.getName(), f.getName(),
						CommonUtils.padStringRight("",
								24 - f.getName().length(), ' '),
						CommonUtils.padStringLeft(String.valueOf(f.length()),
								14, ' ')));
				String ctr = "<html><body><pre>%s</pre></body></html>";
				writeHtmlResponse(response, Ax.format(ctr, format));
				break;
			}
			case download: {
				tmpFolder.mkdirs();
				SEUtilities.deleteDirectory(tmpFolder, true);
				String path = Ax.format("%s/%s", eventRootPath, sessionFolder);
				Stream.of(new File(path).listFiles())
						.sorted(Comparator.comparing(File::lastModified))
						.forEach(f -> Io.read().file(f).write()
								.toFile(SEUtilities.getChildFile(tmpFolder,
										f.getName())));
				File toZip = File.createTempFile("log-download", "zip");
				new ZipUtil().createZip(toZip, tmpFolder,
						new LinkedHashMap<>());
				byte[] bytes = Io.read().file(toZip).asBytes();
				response.setContentType("application/zip");
				response.setContentLength(bytes.length);
				response.setHeader("Content-Disposition", Ax.format(
						"attachment; filename=\"%s.zip\"", sessionFolder));
				Io.write().bytes(bytes).toStream(response.getOutputStream());
				break;
			}
			}
		} catch (Exception e) {
			FormatBuilder format = new FormatBuilder();
			String message = SEUtilities.getFullExceptionMessage(e);
			format.line(USAGE);
			format.line(message);
			writeTextResponse(response, format.toString());
		} finally {
			SEUtilities.deleteDirectory(tmpFolder, true);
		}
	}

	enum Action {
		download, list
	}

	protected void writeHtmlResponse(HttpServletResponse response,
			String string) throws IOException {
		if (response == null) {
			System.out.println(CommonUtils.trimToWsChars(string, 1000));
		} else {
			response.setContentType("text/html");
			response.getWriter().write(string);
		}
	}

	protected void writeTextResponse(HttpServletResponse response,
			String string) throws IOException {
		if (response == null) {
			System.out.println(CommonUtils.trimToWsChars(string, 1000));
		} else {
			try {
				response.setContentType("text/plain");
				response.getWriter().write(string);
			} catch (Exception e) {
				Ax.out("Writing text response: ", string);
				e.printStackTrace();
			}
		}
	}
}
