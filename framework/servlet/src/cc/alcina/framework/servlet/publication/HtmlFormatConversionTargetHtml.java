package cc.alcina.framework.servlet.publication;

import java.io.InputStream;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.publication.FormatConversionTarget.FormatConversionTarget_HTML;
import cc.alcina.framework.entity.Io;

/**
 * Passthrough interface - does no work
 *
 * @author nick@alcina.cc
 */
@Registration({ FormatConverter.class, FormatConversionTarget_HTML.class })
public class HtmlFormatConversionTargetHtml implements FormatConverter {
	@Override
	public InputStream convert(PublicationContext ctx,
			FormatConversionModel hfcm) throws Exception {
		return Io.read().string(hfcm.html).asInputStream();
	}

	@Override
	public String getFileExtension() {
		return "html";
	}

	@Override
	public String getMimeType() {
		return "text/html";
	}
}
