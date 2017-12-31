package cc.alcina.framework.common.client.publication.excel;

import javax.xml.bind.annotation.XmlRootElement;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.publication.ContentDeliveryType;
import cc.alcina.framework.common.client.publication.request.ContentRequestBase;

@RegistryLocation(registryPoint = JaxbContextRegistration.class)
@XmlRootElement
public class BasicExcelRequest
		extends ContentRequestBase<BasicExcelContentDefinition> {
	static final long serialVersionUID = -1L;

	public BasicExcelRequest() {
		putContentDeliveryType(ContentDeliveryType.DOWNLOAD);
	}

	@Override
	public BasicExcelContentDefinition getContentDefinition() {
		return contentDefinition;
	}

	@Override
	public void setContentDefinition(
			BasicExcelContentDefinition contentDefinition) {
		this.contentDefinition = contentDefinition;
	}
}