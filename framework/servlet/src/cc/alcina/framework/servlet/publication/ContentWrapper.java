package cc.alcina.framework.servlet.publication;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.publication.ContentDefinition;
import cc.alcina.framework.common.client.publication.DeliveryModel;
import cc.alcina.framework.common.client.publication.PublicationContent;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.XmlUtils;
import cc.alcina.framework.entity.util.JaxbUtils;
import cc.alcina.framework.servlet.publication.ContentRenderer.ContentRendererResults;

/**
 * Base for 'wrapper' phase of pipeline - adds headers, footers, does any extra
 * end-of-rendering pipeline work.
 *
 * @author nick@alcina.cc
 *
 * @param <D>
 * @param <M>
 * @param <V>
 */
public abstract class ContentWrapper<D extends ContentDefinition, M extends PublicationContent, V extends DeliveryModel> {
	public static final String CONTEXT_NO_CLEAN_XML_HEADERS = ContentWrapper.class
			.getName() + ".CONTEXT_NO_CLEAN_XML_HEADERS";

	private static final String XSL_OUTPUT_METHOD_XML = "<xsl:output method=\"xml\"";

	private static final String XSL_OUTPUT_METHOD_HTML = "<xsl:output method=\"html\"";

	private static final String XML_PI = "<?xml version=\"1.0\"?>";

	public static String transform(InputStream trans, Document wrappingDoc,
			String marker, boolean formatRequiresXml) throws Exception {
		String tSrc = Io.read().fromStream(trans).asString();
		if (formatRequiresXml) {
			tSrc = tSrc.replace(XSL_OUTPUT_METHOD_HTML, XSL_OUTPUT_METHOD_XML);
		} else {
			tSrc = tSrc.replace(XSL_OUTPUT_METHOD_XML, XSL_OUTPUT_METHOD_HTML);
		}
		trans = Io.read().string(tSrc).asInputStream();
		Source trSource = XmlUtils.interpolateStreamSource(trans);
		Source dataSource = new DOMSource(wrappingDoc);
		String wrappedContent = XmlUtils.transformDocToString(dataSource,
				trSource, marker);
		wrappedContent = XmlUtils.expandEmptyElements(wrappedContent);
		if (!LooseContext.is(CONTEXT_NO_CLEAN_XML_HEADERS)) {
			wrappedContent = XmlUtils.cleanXmlHeaders(wrappedContent);
		}
		// we love MsWord/Outlook sooo much
		wrappedContent = wrappedContent.replace("<w:anchorlock></w>",
				"<w:anchorlock />");
		if (formatRequiresXml) {
			wrappedContent = XML_PI + "\n" + wrappedContent;
		} else {
			wrappedContent = XmlUtils.removeSelfClosingHtmlTags(wrappedContent);
		}
		trans.close();
		return wrappedContent;
	}

	protected D contentDefinition;

	protected M publicationContent;

	protected ContentRendererResults rendererResults;

	protected String wrappedContent;

	protected String wrappedFooter;

	protected byte[] wrappedBytes;

	protected V deliveryModel;

	protected Document wrappingDoc;

	protected Map<String, String> replacementParameters = new HashMap<String, String>();

	protected WrapperModel wrapper = createWrapperModel();

	protected String xslPath;

	public Object custom;

	public InputStream stream;

	public Long getUserPublicationId() {
		return wrapper.footerModel.publicationLongId;
	}

	public byte[] getWrappedBytes() {
		return this.wrappedBytes;
	}

	// normally xml
	public String getWrappedContent() {
		return this.wrappedContent;
	}

	public void setWrappedBytes(byte[] wrappedBytes) {
		this.wrappedBytes = wrappedBytes;
	}

	public void setWrappedContent(String wrappedContent) {
		this.wrappedContent = wrappedContent;
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

	protected WrapperModel createWrapperModel() {
		return new WrapperModel();
	}

	protected Class getWrapperTransformClass() {
		return getClass();
	}

	protected void marshallToDoc() throws Exception {
		Set<Class> jaxbClasses = Registry.query(JaxbContextRegistration.class)
				.untypedRegistrations().collect(Collectors.toSet());
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
		if (!Configuration.is("cacheTransforms")) {
			marker += Math.random();
		}
		wrappedContent = transform(trans, wrappingDoc, marker,
				formatRequiresXml);
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@Registration(JaxbContextRegistration.class)
	public static class FooterModel {
		public Long publicationLongId;

		public String publicationId;

		public String publicationDateString;

		public Date publicationDate;

		public String userName;

		public String unsubscribeHtml;

		public boolean showPublicationInfo = true;
	}

	public static class Passthrough<D extends ContentDefinition, M extends PublicationContent, V extends DeliveryModel>
			extends ContentWrapper<D, M, V> {
		@Override
		public void wrapContent(ContentDefinition contentDefinition,
				PublicationContent publicationContent,
				DeliveryModel deliveryModel,
				ContentRendererResults rendererResults, long publicationId,
				long publicationUserId) throws Exception {
			if (rendererResults.bytes == null
					&& rendererResults.htmlContent != null) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				OutputStreamWriter osw = new OutputStreamWriter(baos, "UTF-8");
				osw.write(rendererResults.htmlContent);
				osw.close();
				rendererResults.bytes = baos.toByteArray();
			}
			wrappedBytes = rendererResults.bytes;
			wrappedContent = rendererResults.htmlContent;
		}

		@Override
		protected void prepareWrapper(long publicationId,
				long publicationUserId) throws Exception {
			wrapper.rendererResults = rendererResults;
		}
	}

	@XmlRootElement(name = "info")
	@XmlAccessorType(XmlAccessType.FIELD)
	@Registration(JaxbContextRegistration.class)
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
