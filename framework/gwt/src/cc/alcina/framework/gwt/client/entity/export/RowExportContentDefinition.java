package cc.alcina.framework.gwt.client.entity.export;

import javax.xml.bind.annotation.XmlRootElement;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.domain.search.EntitySearchDefinition;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.publication.ContentDefinition;
import cc.alcina.framework.common.client.publication.FormatConversionTarget;
import cc.alcina.framework.common.client.serializer.TypeSerialization;

@RegistryLocation(registryPoint = JaxbContextRegistration.class)
@XmlRootElement
@Bean
@ObjectPermissions(read = @Permission(access = AccessLevel.ADMIN), write = @Permission(access = AccessLevel.ADMIN))
@TypeSerialization(flatSerializable = false)
public class RowExportContentDefinition extends Bindable
		implements ContentDefinition {
	private EntitySearchDefinition searchDefinition;

	private RowExportFormat format = RowExportFormat.CSV;

	public RowExportContentDefinition() {
	}

	@Display(name = "Format", orderingHint = 100)
	public RowExportFormat getFormat() {
		return this.format;
	}

	@Override
	public String getPublicationType() {
		return "Row Export";
	}

	public EntitySearchDefinition getSearchDefinition() {
		return this.searchDefinition;
	}

	public void setFormat(RowExportFormat format) {
		RowExportFormat old_format = this.format;
		this.format = format;
		propertyChangeSupport().firePropertyChange("format", old_format,
				format);
	}

	public void setSearchDefinition(EntitySearchDefinition def) {
		this.searchDefinition = def;
	}

	public enum RowExportFormat {
		CSV, HTML;

		public FormatConversionTarget toConversionTarget() {
			switch (this) {
			case CSV:
				return FormatConversionTarget.CSV;
			case HTML:
				return FormatConversionTarget.HTML;
			default:
				throw new UnsupportedOperationException();
			}
		}
	}
}
