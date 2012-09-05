package au.com.barnet.common.j2se.server.publication;

import java.io.InputStream;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.publication.FormatConversionTarget.FMT_HTML;
import cc.alcina.framework.entity.ResourceUtilities;

/**
 * Passthrough interface - does no work
 * @author nreddel@barnet.com.au
 *
 */
@RegistryLocation(registryPoint = FormatConverter.class, targetClass = FMT_HTML.class)
public class HtmlFormatConversionTargetHtml implements FormatConverter {
	public InputStream convert(FormatConversionModel hfcm) throws Exception{
		return ResourceUtilities.writeStringToInputStream(hfcm.html);
	}

	public String getMimeType() {
		return "text/html";
	}

	public String getFileExtension() {
		return "html";
	}
}
