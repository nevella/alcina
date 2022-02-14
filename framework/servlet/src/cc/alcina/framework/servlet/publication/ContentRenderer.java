package cc.alcina.framework.servlet.publication;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

import cc.alcina.framework.common.client.dom.DomDoc;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.publication.ContentDefinition;
import cc.alcina.framework.common.client.publication.DeliveryModel;
import cc.alcina.framework.common.client.publication.PublicationContent;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.XmlUtils;
import cc.alcina.framework.entity.XmlUtils.TransformerFactoryConfigurator;
import cc.alcina.framework.entity.util.JaxbUtils;

/**
 * Base class for the content rendering stage of the publication pipeline.
 * Default implementation runs an xsl transform on incoming (xml-serializable)
 * object graph.
 *
 * @author nick@alcina.cc
 *
 * @param <D>
 * @param <M>
 * @param <V>
 */
public abstract class ContentRenderer<D extends ContentDefinition, M extends PublicationContent, V extends DeliveryModel> {
	public static final String CONTEXT_TRANSFORM_FILENAME = ContentRenderer.class
			.getName() + ".CONTEXT_TRANSFORM_FILENAME";

	protected D contentDefinition;

	protected M publicationContent;

	protected ContentRendererResults results;

	protected V deliveryModel;

	protected Document doc;

	protected RenderTransformWrapper wrapper;

	public ContentRendererResults getResults() {
		return this.results;
	}

	public void renderContent(D contentDefinition, M publicationContent,
			V deliveryModel, long publicationId, long publicationUserId)
			throws Exception {
		this.contentDefinition = contentDefinition;
		this.publicationContent = publicationContent;
		this.deliveryModel = deliveryModel;
		results = new ContentRendererResults();
		doc = XmlUtils.createDocument();
		renderContent(publicationId, publicationUserId);
	}

	protected TransformerFactoryConfigurator
			getTransformerFactoryConfigurator() {
		return new TransformerConfigurator();
	}

	protected void marshallToDoc() throws Exception {
		JAXBContext jc = JaxbUtils.getContext();
		Marshaller m = jc.createMarshaller();
		m.marshal(wrapper, doc);
		boolean logDocXml = false;
		if (logDocXml) {
			new DomDoc(doc).logPretty();
		}
	}

	protected abstract void renderContent(long publicationId,
			long publicationUserId) throws Exception;

	protected void transform(String xslPath) throws Exception {
		InputStream trans = null;
		if (LooseContext.containsKey(CONTEXT_TRANSFORM_FILENAME)) {
			trans = new FileInputStream(
					LooseContext.getString(CONTEXT_TRANSFORM_FILENAME));
		} else {
			trans = getClass().getResourceAsStream(xslPath);
		}
		String marker = getClass().getName() + "/" + xslPath;
		Source trSource = XmlUtils.interpolateStreamSource(trans);
		Source dataSource = new DOMSource(doc);
		results.htmlContent = XmlUtils.transformDocToString(dataSource,
				trSource, marker, getTransformerFactoryConfigurator());
		results.htmlContent = results.htmlContent.replaceFirst("<\\?.+?\\?>",
				"");
		try {
			trans.close();
		} catch (Exception e) {
		}
	}

	
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlRootElement
	@Registration(JaxbContextRegistration.class)
	public static class ContentRendererResults {
		public String htmlContent;

		public String htmlContentDescription;

		public String htmlContentTitle;

		public byte[] bytes;

		public boolean persist;

		public boolean htmlContentDescriptionUnescaped;

		public InputStream stream;
	}

	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	
	@Registration(JaxbContextRegistration.class)
	public static class RenderTransformWrapper implements Serializable {
		// public ContentDefinition cd;
		//
		// public PublicationContent pc;
		//
		// public DeliveryModel dm;
		public RenderTransformWrapper() {
		}
	}
}
