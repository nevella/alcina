package cc.alcina.framework.servlet.misc;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class JaxbShortDateAdapter extends XmlAdapter<String, Date> {
	private SimpleDateFormat dateFormat = new SimpleDateFormat(
			"dd-MM-yyyy");

	@Override
	public String marshal(Date v) throws Exception {
		return dateFormat.format(v);
	}

	@Override
	public Date unmarshal(String v) throws Exception {
		return dateFormat.parse(v);
	}
}