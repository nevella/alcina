package cc.alcina.framework.entity.gwt.reflection.jdk;

import java.util.List;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.DefaultConfigurationProperty;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.SelectionProperty;
import com.google.gwt.core.ext.TreeLogger;

import cc.alcina.framework.entity.gwt.reflection.ClientReflectionGenerator;

class PropertyOracleImpl implements PropertyOracle {
	GeneratorContextImpl generatorContextImpl;

	PropertyOracleImpl(GeneratorContextImpl generatorContextImpl) {
		this.generatorContextImpl = generatorContextImpl;
	}

	@Override
	public ConfigurationProperty getConfigurationProperty(String propertyName)
			throws BadPropertyValueException {
		JdkReflectionGenerator.Attributes attributes = generatorContextImpl.generator.attributes;
		String value = null;
		switch (propertyName) {
		case ClientReflectionGenerator.DATA_FOLDER_CONFIGURATION_KEY:
			value = attributes.generationDataFolder().getAbsolutePath();
			break;
		case ClientReflectionGenerator.FILTER_PEER_CONFIGURATION_KEY:
			value = attributes.filterPeerClass.getName();
			break;
		case ClientReflectionGenerator.LINKER_PEER_CONFIGURATION_KEY:
			value = attributes.linkerPeerClass.getName();
			break;
		default:
			throw new UnsupportedOperationException(
					"Unimplemented configuration property: " + propertyName);
		}
		return new DefaultConfigurationProperty(propertyName, List.of(value));
	}

	@Override
	public SelectionProperty getSelectionProperty(TreeLogger logger,
			String propertyName) throws BadPropertyValueException {
		switch (propertyName) {
		default:
			throw new UnsupportedOperationException(
					"Unimplemented selection property: " + propertyName);
		}
	}
}
