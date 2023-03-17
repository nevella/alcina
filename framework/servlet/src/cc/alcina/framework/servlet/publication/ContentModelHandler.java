package cc.alcina.framework.servlet.publication;

import cc.alcina.framework.common.client.publication.ContentDefinition;
import cc.alcina.framework.common.client.publication.DeliveryModel;
import cc.alcina.framework.common.client.publication.PublicationContent;

/**
 * Base class for the 'content handling' part of the publication pipeline
 *
 * @see Publisher
 * @author nick@alcina.cc
 *
 * @param <D>
 * @param <M>
 * @param <V>
 */
public abstract class ContentModelHandler<D extends ContentDefinition, M extends PublicationContent, V extends DeliveryModel> {
	protected D contentDefinition;

	protected M publicationContent;

	protected boolean hasResults;

	protected V deliveryModel;

	public M getPublicationContent() {
		return publicationContent;
	}

	public PublicationVisitor getVisitor() {
		return PublicationContext.get().getVisitor();
	}

	public boolean isHasResults() {
		return this.hasResults;
	}

	public boolean prepareContent(D contentDefinition, V deliveryModel)
			throws Exception {
		this.contentDefinition = contentDefinition;
		this.deliveryModel = deliveryModel;
		prepareContent();
		return hasResults;
	}

	public void setHasResults(boolean hasResults) {
		this.hasResults = hasResults;
	}

	protected abstract void prepareContent() throws Exception;
}
