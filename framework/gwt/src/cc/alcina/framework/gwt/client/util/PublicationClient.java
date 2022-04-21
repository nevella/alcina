package cc.alcina.framework.gwt.client.util;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.publication.request.PublicationResult;

@Registration.Singleton(PublicationClient.class)
public interface PublicationClient {
	public static PublicationClient get(){
		return Registry.impl(PublicationClient.class);
	}
	public void downloadPublicationResult(PublicationResult result) ;
	
}
