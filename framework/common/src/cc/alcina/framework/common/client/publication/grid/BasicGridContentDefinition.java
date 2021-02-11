package cc.alcina.framework.common.client.publication.grid;

import javax.xml.bind.annotation.XmlRootElement;

import cc.alcina.framework.common.client.entity.GwtMultiplePersistable;
import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.publication.ContentDefinition;
import cc.alcina.framework.common.client.search.SearchDefinition;

@RegistryLocation(registryPoint = JaxbContextRegistration.class)
@XmlRootElement
public class BasicGridContentDefinition extends WrapperPersistable
		implements ContentDefinition, GwtMultiplePersistable {
	private SearchDefinition searchDefinition;

	@Override
	public String getPublicationType() {
		return "Grid export";
	}

	public SearchDefinition getSearchDefinition() {
		return this.searchDefinition;
	}

	public void setSearchDefinition(SearchDefinition searchDefinition) {
		this.searchDefinition = searchDefinition;
	}
}