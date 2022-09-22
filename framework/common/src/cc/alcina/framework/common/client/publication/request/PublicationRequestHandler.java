package cc.alcina.framework.common.client.publication.request;

import cc.alcina.framework.common.client.csobjects.WebException;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.publication.ContentDefinition;
import cc.alcina.framework.common.client.publication.request.ContentRequestBase;
import cc.alcina.framework.common.client.publication.request.PublicationResult;

@Registration(PublicationRequestHandler.class)
public interface PublicationRequestHandler {
	public static PublicationRequestHandler get() {
		return Registry.impl(PublicationRequestHandler.class);
	}

	PublicationResult
			publish(ContentRequestBase<? extends ContentDefinition> cr)
					throws WebException;
}
