package cc.alcina.framework.common.client.publication.grid;

import javax.xml.bind.annotation.XmlRootElement;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.publication.ContentDeliveryType;
import cc.alcina.framework.common.client.publication.FormatConversionTarget;
import cc.alcina.framework.common.client.publication.request.ContentRequestBase;
import cc.alcina.framework.common.client.serializer.TypeSerialization;

@RegistryLocation(registryPoint = JaxbContextRegistration.class)
@XmlRootElement
@TypeSerialization(flatSerializable = false)
public class BasicGridRequest
		extends ContentRequestBase<BasicGridContentDefinition> {

	public BasicGridRequest() {
		putContentDeliveryType(ContentDeliveryType.DOWNLOAD);
		putFormatConversionTarget(FormatConversionTarget.XLSX);
	}

	@Override
	public BasicGridContentDefinition getContentDefinition() {
		return contentDefinition;
	}

	@Override
	public void
			setContentDefinition(BasicGridContentDefinition contentDefinition) {
		this.contentDefinition = contentDefinition;
	}
}