package cc.alcina.framework.common.client.sync.property;

import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;

public class PropertyModificationLogSupport {
	public static final String CONTEXT_LOG_PROPERTY_MODIFICATIONS = PropertyModificationLogSupport.class
			+ ".CONTEXT_LOG_PROPERTY_MODIFICATIONS";

	public static final String CONTEXT_PROPERTY_MODIFICATION_SOURCE = PropertyModificationLogSupport.class
			+ ".CONTEXT_PROPERTY_MODIFICATION_SOURCE";

	public static PropertyModificationLogSupportSource modificationSource;

	public void fieldUpdated(HasPropertyLog logSource, String propertyName,
			Object old_value, Object value) {
		if (logSource == null
				|| CommonUtils.equalsWithNullEmptyEquality(old_value, value)) {
			return;
		}
		PropertyModificationLogSupportSource source = modificationSource;
		if (LooseContext.containsKey(CONTEXT_PROPERTY_MODIFICATION_SOURCE)) {
			source = LooseContext.get(CONTEXT_PROPERTY_MODIFICATION_SOURCE);
		}
		if (source == null) {
			return;
		}
		if (!TransformManager.get().isRegistered(logSource)
				|| LooseContext.is(CONTEXT_LOG_PROPERTY_MODIFICATIONS)) {
			return;
		}
		PropertyModificationLog log = logSource
				.providePropertyModificationLog();
		PropertyModificationLogItem item = new PropertyModificationLogItem();
		item.setModificationTime(System.currentTimeMillis());
		item.setPropertyName(propertyName);
		item.setValue(CommonUtils.nullSafeToString(value));
		item.setSource(source);
	}
}
