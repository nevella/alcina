/**
 */
package cc.alcina.framework.gwt.client.entity.export;

import javax.xml.bind.annotation.XmlRootElement;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.publication.ContentDeliveryType;
import cc.alcina.framework.common.client.publication.FormatConversionTarget;
import cc.alcina.framework.common.client.publication.request.ContentRequestBase;
import cc.alcina.framework.common.client.serializer.TypeSerialization;

@XmlRootElement
@TypeSerialization(flatSerializable = false)
@Registration(JaxbContextRegistration.class)
public class RowExportRequest
		extends ContentRequestBase<RowExportContentDefinition> {
	public RowExportRequest() {
		contentDefinition = new RowExportContentDefinition();
		putFormatConversionTarget(FormatConversionTarget.CSV);
		putContentDeliveryType(ContentDeliveryType.DOWNLOAD);
	}

	@Override
	public RowExportContentDefinition getContentDefinition() {
		return this.contentDefinition;
	}

	@Override
	public void
			setContentDefinition(RowExportContentDefinition contentDefinition) {
		this.contentDefinition = contentDefinition;
	}
}
