package cc.alcina.framework.servlet.publication;

import java.io.InputStream;

import cc.alcina.framework.common.client.publication.request.PublicationResult;
import cc.alcina.framework.servlet.publication.ContentWrapper.WrapperModel;

/**
 * To allow custom behaviour/composition in the publication pipeline
 *
 * @author nick@alcina.cc
 *
 *         p.s. I almost named this 'PublicationAccompanist' - in keeping with
 *         other music-themed naming such as 'Consort' - it's not really a
 *         visitor, it's more a way to encapsulate customisations of a process,
 *         based on the initial state (boundary conditions) - the way an
 *         accompanist will modify the expression of music (but not be the main
 *         focus). Hmmm....that's not quite right either. What would you call
 *         this pattern? Apart from the overused 'Custoomiser', huh?
 *
 */
public class PublicationVisitor {
	public void adjustWrapper(ContentWrapper mainWrapper,
			WrapperModel wrapper) {
	}

	public String adjustWrapperXslPath(String suggested) {
		return suggested;
	}

	public void afterPrepareContent(ContentModelHandler cmh) {
		// TODO Auto-generated method stub
	}

	public void afterPublicationPersistence(long publicationId)
			throws Exception {
	}

	public void afterWrapContent(ContentWrapper cw) {
	}

	public void beforeDelivery() {
	}

	public void beforeRenderContent() {
	}

	public void beforeWrapContent() {
	}

	public PublicationContext context() {
		return PublicationContext.get();
	}

	public void customCss(WrapperModel wrapper) throws Exception {
	}

	public void publicationFinished(PublicationResult result) {
	}

	public InputStream transformConvertedContent(InputStream convertedContent) {
		return convertedContent;
	}
}
