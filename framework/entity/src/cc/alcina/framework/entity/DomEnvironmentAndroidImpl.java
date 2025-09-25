package cc.alcina.framework.entity;

import java.io.StringWriter;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.dom.DomNode;

public class DomEnvironmentAndroidImpl extends DomEnvironmentJvmBase {
	@Override
	public String toPrettyMarkup(Document doc) {
		return streamXml(doc);
	}

	@Override
	public String toPrettyMarkup(DomNode xmlNode) {
		Node node = xmlNode.w3cNode();
		return streamXml(node);
	}

	String streamXml(Node n) {
		try {
			TransformerFactory transformerFactory = TransformerFactory
					.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			StringWriter stringWriter = new StringWriter();
			StreamResult streamResult = new StreamResult(stringWriter);
			transformer.transform(new DOMSource(n), streamResult);
			return stringWriter.toString();
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}

	@Override
	public String toMarkup(Node node) {
		return streamXml(node);
	}
}
