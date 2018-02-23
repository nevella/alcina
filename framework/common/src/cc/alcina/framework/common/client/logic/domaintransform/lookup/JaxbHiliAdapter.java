package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocator;

public class JaxbHiliAdapter<T extends HasIdAndLocalId>
		extends XmlAdapter<String, T> {
	@Override
	public String marshal(T hili) throws Exception {
		return new HiliLocator(hili).toParseableString();
	}

	@Override
	public T unmarshal(String v) throws Exception {
		HiliLocator locator = HiliLocator.parse(v);
		return locator.find();
	}
}