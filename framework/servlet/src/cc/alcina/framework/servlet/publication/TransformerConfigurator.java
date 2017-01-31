package cc.alcina.framework.servlet.publication;

import java.io.InputStream;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import cc.alcina.framework.entity.XmlUtils;
import cc.alcina.framework.entity.XmlUtils.TransformerFactoryConfigurator;

public final class TransformerConfigurator implements
		TransformerFactoryConfigurator, URIResolver {
	@Override
	public void configure(TransformerFactory transformerFactory) {
		transformerFactory.setURIResolver(this);
	}

	@Override
	public Source resolve(String href, String base) throws TransformerException {
		if (href.startsWith("res://")) {
			href = href.substring(5);
			InputStream trans = getClass().getResourceAsStream(href);
			Source source = XmlUtils.interpolateStreamSource(trans);
			return source;
		}
		return null;
	}
}