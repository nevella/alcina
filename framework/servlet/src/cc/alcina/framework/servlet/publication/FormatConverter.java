package cc.alcina.framework.servlet.publication;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.publication.FormatConversionTarget;

/**
 * Implemented by format converters
 *
 * 
 *
 */
@Registration.NonGenericSubtypes(FormatConverter.class)
public interface FormatConverter<T extends FormatConversionTarget> {
	public InputStream convert(PublicationContext ctx,
			FormatConversionModel model) throws Exception;

	public String getFileExtension();

	public String getMimeType();

	/**
	 * During publication, non-passthrough causes the publisher to convert even
	 * when exiting before delivery (e.g. pdf conversion of html/tex)
	 */
	default boolean isPassthrough() {
		return true;
	}

	public static class FormatConversionModel implements Serializable {
		public String html;

		public String footer;

		public byte[] bytes;

		public Object custom;

		public List rows;

		public InputStream stream;

		public String fileExtension;

		public String mimeType;

		public InputStream provideByteStream() {
			return stream != null ? stream : new ByteArrayInputStream(bytes);
		}
	}
}
