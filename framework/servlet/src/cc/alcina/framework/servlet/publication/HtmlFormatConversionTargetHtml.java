package cc.alcina.framework.servlet.publication;

import java.io.InputStream;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.publication.FormatConversionTarget.FormatConversionTarget_HTML;
import cc.alcina.framework.entity.ResourceUtilities;

/**
 * Passthrough interface - does no work
 *
 * @author nick@alcina.cc
 */
@Registration({ FormatConverter.class, FormatConversionTarget_HTML.class })
public class HtmlFormatConversionTargetHtml implements FormatConverter {
	public InputStream convert(PublicationContext ctx,
			FormatConversionModel hfcm) throws Exception {
		return ResourceUtilities.writeStringToInputStream(hfcm.html);
	}

	public String getFileExtension() {
		return "html";
	}

	public String getMimeType() {
		return "text/html";
	}
}
