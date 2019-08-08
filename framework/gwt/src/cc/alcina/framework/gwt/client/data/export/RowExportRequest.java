/**
 * 
 */
package cc.alcina.framework.gwt.client.data.export;

import javax.xml.bind.annotation.XmlRootElement;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.publication.ContentDeliveryType;
import cc.alcina.framework.common.client.publication.FormatConversionTarget;
import cc.alcina.framework.common.client.publication.request.ContentRequestBase;

@RegistryLocation(registryPoint = JaxbContextRegistration.class)
@XmlRootElement
public class RowExportRequest
		extends ContentRequestBase<RowExportContentDefinition> {
	public RowExportRequest() {
		contentDefinition = new RowExportContentDefinition();
		putFormatConversionTarget(FormatConversionTarget.CSV);
		putContentDeliveryType(ContentDeliveryType.DOWNLOAD);
	}

	public RowExportContentDefinition getContentDefinition() {
		return this.contentDefinition;
	}

	public void
			setContentDefinition(RowExportContentDefinition contentDefinition) {
		this.contentDefinition = contentDefinition;
	}
}