package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;

public class JaxbEntityAdapter<T extends Entity> extends XmlAdapter<String, T> {
	@Override
	public String marshal(T entity) throws Exception {
		return entity.toLocator().toParseableString();
	}

	@Override
	public T unmarshal(String v) throws Exception {
		EntityLocator locator = EntityLocator.parse(v);
		return locator.find();
	}
}