package cc.alcina.framework.servlet.publication;

import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
/**
 * Implemented by format converters
 * @author nreddel@barnet.com.au
 *
 */
public interface FormatConverter {
	public InputStream convert(PublicationContext ctx, FormatConversionModel model) throws Exception;

	public String getMimeType();

	public String getFileExtension();

	public static class FormatConversionModel implements Serializable{
		public String html;

		public String footer;

		public byte[] bytes;

		public Object custom;

		public List rows;


	}
}
