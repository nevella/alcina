package cc.alcina.framework.gwt.client.service;

import com.totsp.gwittir.client.beans.BeanDescriptor;

public interface BeanDescriptorProvider {
	public BeanDescriptor getDescriptor(Object object);

	default BeanDescriptor getDescriptorOrNull(Object object) {
		return getDescriptor(object);
	}
}
