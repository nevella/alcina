package cc.alcina.framework.servlet.servlet;

import cc.alcina.framework.common.client.csobjects.WebException;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.publication.ContentDefinition;
import cc.alcina.framework.common.client.publication.request.ContentRequestBase;
import cc.alcina.framework.common.client.publication.request.PublicationResult;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.servlet.publication.Publisher;

@Registration(PublicationRequestHandler.class)
public class PublicationRequestHandler {
	public static PublicationRequestHandler get() {
		return Registry.impl(PublicationRequestHandler.class);
	}

	public PublicationResult
			publish(ContentRequestBase<? extends ContentDefinition> cr)
					throws WebException {
		try {
			LooseContext.push();
			cr.getContentDefinition().initialiseContext();
			return new Publisher().publish(cr.getContentDefinition(), cr);
		} catch (Exception e) {
			throw new WebException(e.getMessage());
		} finally {
			LooseContext.pop();
		}
	}
}
