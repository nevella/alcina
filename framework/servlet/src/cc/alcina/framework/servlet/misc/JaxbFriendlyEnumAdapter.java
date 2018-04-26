package cc.alcina.framework.servlet.misc;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import cc.alcina.framework.common.client.util.Ax;

public class JaxbFriendlyEnumAdapter extends XmlAdapter<String, Enum> {
	@Override
	public String marshal(Enum v) throws Exception {
		return Ax.friendly(v);
	}

	@Override
	public Enum unmarshal(String v) throws Exception {
		throw new UnsupportedOperationException();
	}
}