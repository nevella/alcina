package cc.alcina.framework.classmeta;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.registry.ClassMetadataCache;
import cc.alcina.framework.entity.util.JaxbUtils;

public class ClassPersistenceScanHandler extends AbstractHandler {
	private ClassMetaHandler metaHandler;

	public ClassPersistenceScanHandler(ClassMetaHandler metaHandler) {
		this.metaHandler = metaHandler;
	}

	@Override
	public void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		LooseContext.runWithKeyValue(JaxbUtils.CONTEXT_CLASSES,
				getInitClasses(), () -> {
					handle0(target, baseRequest, response);
					return null;
				});
	}

	private ClassPersistenceScanData
			calculate(ClassPersistenceScanSchema schema) {
		try {
			ClassPersistenceScanData result = new ClassPersistenceScanData();
			result.schema = schema;
			result.generated = new Date();
			ClassMetaRequest typedRequest = new ClassMetaRequest();
			typedRequest.classPaths = schema.classPathUrls.stream()
					.map(SEUtilities::toURL).collect(Collectors.toList());
			// assume (initially) that called from host, not container - no
			// translation
			ClassMetadataCache classMetadataCache = metaHandler.classpathScannerResolver
					.handle(typedRequest, null, false);
			new PersistenceDeltaScanner(result).scan(classMetadataCache,
					schema.scanClasspathCachePath);
			return result;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private List<Class> getInitClasses() {
		return Arrays.asList(ClassPersistenceScanSchema.class,
				ClassPersistenceScanData.class);
	}

	protected void handle0(String target, Request baseRequest,
			HttpServletResponse response) throws IOException {
		metaHandler.refreshJars();
		String schemaXml = ResourceUtilities.read(
				ClassPersistenceScanHandler.class,
				Ax.format("schema/%s/schema.xml", target.replace("/", "")));
		ClassPersistenceScanSchema schema = JaxbUtils
				.xmlDeserialize(ClassPersistenceScanSchema.class, schemaXml);
		ClassPersistenceScanData data = calculate(schema);
		boolean equivalent = false;
		try {
			String lastXml = ResourceUtilities
					.readFileToString(schema.scanResourcePath);
			ClassPersistenceScanData last = JaxbUtils
					.xmlDeserialize(ClassPersistenceScanData.class, lastXml);
			equivalent = last.equivalentTo(data);
		} catch (Exception e) {
			e.printStackTrace();
		}
		String dataOut = JaxbUtils.xmlSerialize(data);
		ResourceUtilities.writeStringToFile(dataOut, schema.scanResourcePath);
		String message = Ax.format(
				"Calculated persistence meta: (equivalent: %s)\n%s", equivalent,
				data);
		Ax.out(message);
		message = equivalent ? "no-update" : "update";
		if (response != null) {
			response.setContentType("text/plain");
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().write(message);
			baseRequest.setHandled(true);
		}
	}
}
