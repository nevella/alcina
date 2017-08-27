package cc.alcina.framework.servlet.publication;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.publication.ContentDefinition;
import cc.alcina.framework.common.client.publication.DeliveryModel;
import cc.alcina.framework.common.client.publication.PublicationContent;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.XmlUtils;
import cc.alcina.framework.entity.util.JaxbUtils;
import cc.alcina.framework.servlet.publication.ContentRenderer.ContentRendererResults;

/**
 * Base for 'wrapper' phase of pipeline - adds headers, footers, does any extra
 * end-of-rendering pipeline work.
 *
 * @author nreddel@barnet.com.au
 *
 * @param <D>
 * @param <M>
 * @param <V>
 */
public abstract class ContentWrapper<D extends ContentDefinition, M extends PublicationContent, V extends DeliveryModel> {
	private static final String XSL_OUTPUT_METHOD_XML = "<xsl:output method=\"xml\"";

	private static final String XSL_OUTPUT_METHOD_HTML = "<xsl:output method=\"html\"";

	private static final String XML_PI = "<?xml version=\"1.0\"?>";

	public static String transform(InputStream trans, Document wrappingDoc,
			String marker, boolean formatRequiresXml) throws Exception {
		String tSrc = ResourceUtilities.readStreamToString(trans);
		if (formatRequiresXml) {
			tSrc = tSrc.replace(XSL_OUTPUT_METHOD_HTML, XSL_OUTPUT_METHOD_XML);
		} else {
			tSrc = tSrc.replace(XSL_OUTPUT_METHOD_XML, XSL_OUTPUT_METHOD_HTML);
		}
		trans = ResourceUtilities.writeStringToInputStream(tSrc);
		Source trSource = XmlUtils.interpolateStreamSource(trans);
		Source dataSource = new DOMSource(wrappingDoc);
		String wrappedContent = XmlUtils.transformDocToString(dataSource,
				trSource, marker);
		wrappedContent = XmlUtils.expandEmptyElements(wrappedContent);
		wrappedContent = XmlUtils.cleanXmlHeaders(wrappedContent);
		if (formatRequiresXml) {
			wrappedContent = XML_PI + "\n" + wrappedContent;
		} else {
			wrappedContent = wrappedContent.replace("<br></br>", "<br>");
		}
		trans.close();
		return wrappedContent;
	}

	protected D contentDefinition;

	protected M publicationContent;

	protected ContentRendererResults rendererResults;

	protected String wrappedContent;

	public void setWrappedContent(String wrappedContent) {
		this.wrappedContent = wrappedContent;
	}

	protected String wrappedFooter;

	protected byte[] wrappedBytes;

	protected V deliveryModel;

	protected Document wrappingDoc;

	protected Map<String, String> replacementParameters = new HashMap<String, String>();

	protected WrapperModel wrapper = createWrapperModel();

	protected WrapperModel createWrapperModel() {
		return new WrapperModel();
	}

	protected String xslPath;

	public Object custom;

	public Long getUserPublicationId() {
		return wrapper.footerModel.publicationLongId;
	}

	// normally xml
	public String getWrappedContent() {
		return this.wrappedContent;
	}

	public void wrapContent(D contentDefinition, M publicationContent,
			V deliveryModel, ContentRendererResults rendererResults,
			long publicationId, long publicationUserId) throws Exception {
		this.contentDefinition = contentDefinition;
		this.publicationContent = publicationContent;
		this.deliveryModel = deliveryModel;
		this.rendererResults = rendererResults;
		wrappingDoc = XmlUtils.createDocument();
		prepareWrapper(publicationId, publicationUserId);
		wrapper.css = CommonUtils.namedFormat(wrapper.css,
				replacementParameters);
		wrapper.printCss = CommonUtils.namedFormat(wrapper.printCss,
				replacementParameters);
		marshallToDoc();
		boolean formatRequiresXml = deliveryModel.provideTargetFormat()
				.requiresXml();
		transform(xslPath, formatRequiresXml);
	}

	protected void marshallToDoc() throws Exception {
		Set<Class> jaxbClasses = new HashSet<Class>(
				Registry.get().lookup(JaxbContextRegistration.class));
		JAXBContext jc = JaxbUtils.getContext(jaxbClasses);
		Marshaller m = jc.createMarshaller();
		m.marshal(wrapper, wrappingDoc);
	}

	protected abstract void prepareWrapper(long publicationId,
			long publicationUserId) throws Exception;

	protected void transform(String xslPath, boolean formatRequiresXml)
			throws Exception {
		InputStream trans = getWrapperTransformClass()
				.getResourceAsStream(xslPath);
		String marker = getWrapperTransformClass().getName() + "/" + xslPath
				+ "-" + formatRequiresXml;
		if (!ResourceUtilities.getBoolean(ContentWrapper.class,
				"cacheTransforms")) {
			marker += Math.random();
		}
		wrappedContent = transform(trans, wrappingDoc, marker,
				formatRequiresXml);
	}

	protected Class getWrapperTransformClass() {
		return getClass();
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@RegistryLocation(registryPoint = JaxbContextRegistration.class)
	public static class FooterModel {
		public Long publicationLongId;

		public String publicationId;

		public String publicationDateString;

		public Date publicationDate;

		public String userName;

		public String unsubscribeHtml;

		public boolean showPublicationInfo = true;
	}

	@XmlRootElement(name = "info")
	@XmlAccessorType(XmlAccessType.FIELD)
	@RegistryLocation(registryPoint = JaxbContextRegistration.class)
	public static class WrapperModel {
		public String css;

		public String printCss;

		public ContentRendererResults rendererResults;

		public String resourceBaseHref;

		public boolean header;

		public String systemMessage;

		public String description;

		public boolean footer;

		public boolean narrow;

		public boolean css31;

		public FooterModel footerModel = new FooterModel();

		public String headerContent;

		public String linkBaseHref;

		public int year;

		public String siteBaseHref;

		public String baseHref;

		public String inlineEmailCss;

		public boolean wrapInDiv;

		public boolean ieCssHacks;

		public boolean noSpecificHeaderContent;

		@XmlTransient
		public List gridRows = null;

		public WrapperModel() {
		}
	}
}
