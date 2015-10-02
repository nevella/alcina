package cc.alcina.framework.servlet.publication;

import java.io.InputStream;

import cc.alcina.framework.common.client.publication.request.PublicationResult;
import cc.alcina.framework.servlet.publication.ContentWrapper.WrapperModel;

/**
 * To allow custom behaviour/composition in the publication pipeline
 * @author nick@alcina.cc
 *
 */
public class PublicationVisitor {
	public void customCss(WrapperModel wrapper) throws Exception {
	}

	public void adjustWrapper(ContentWrapper mainWrapper, WrapperModel wrapper) { }

	public String adjustWrapperXslPath(String suggested) {
		return suggested;
	}

	public void publicationFinished(PublicationResult result){

	}

	public void beforeDelivery() {
	}

	public void beforeWrapContent() {

	}

	public void afterWrapContent(ContentWrapper cw) {
		
	}

	public InputStream transformConvertedContent(InputStream convertedContent) {
		return convertedContent;
	}
}
