package cc.alcina.framework.common.client.publication.excel;

import javax.xml.bind.annotation.XmlRootElement;

import cc.alcina.framework.common.client.entity.GwtMultiplePersistable;
import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.publication.ContentDefinition;
import cc.alcina.framework.common.client.search.SingleTableSearchDefinition;

@RegistryLocation(registryPoint = JaxbContextRegistration.class)
@XmlRootElement
public class BasicGridContentDefinition extends WrapperPersistable
		implements ContentDefinition, GwtMultiplePersistable {
	private SingleTableSearchDefinition searchDefinition;

	@Override
	public String getPublicationType() {
		return "Grid export";
	}

	public SingleTableSearchDefinition getSearchDefinition() {
		return this.searchDefinition;
	}

	public void
			setSearchDefinition(SingleTableSearchDefinition searchDefinition) {
		this.searchDefinition = searchDefinition;
	}
}