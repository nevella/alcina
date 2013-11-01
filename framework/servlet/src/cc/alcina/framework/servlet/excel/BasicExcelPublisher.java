package cc.alcina.framework.servlet.excel;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.w3c.dom.Document;

import cc.alcina.framework.common.client.csobjects.SearchResultsBase;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.publication.ContentDefinition;
import cc.alcina.framework.common.client.publication.FormatConversionTarget;
import cc.alcina.framework.common.client.publication.PublicationContent;
import cc.alcina.framework.common.client.publication.excel.BasicExcelContentDefinition;
import cc.alcina.framework.common.client.publication.excel.BasicExcelRequest;
import cc.alcina.framework.common.client.publication.request.ContentRequestBase;
import cc.alcina.framework.common.client.search.SingleTableSearchDefinition;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.XmlUtils;
import cc.alcina.framework.servlet.CommonRemoteServletProvider;
import cc.alcina.framework.servlet.publication.ContentModelHandler;
import cc.alcina.framework.servlet.publication.ContentRenderer;
import cc.alcina.framework.servlet.publication.ContentRenderer.RenderTransformWrapper;

public class BasicExcelPublisher {
	public static final int PUB_MAX_RESULTS = 100000;

	@RegistryLocation(registryPoint = ContentModelHandler.class, targetClass = BasicExcelContentDefinition.class)
	public static class BasicExcelPublisherContentHandler
			extends
			ContentModelHandler<BasicExcelContentDefinition, BasicExcelPublicationModel, BasicExcelRequest> {
		@Override
		protected void prepareContent() throws Exception {
			publicationContent = new BasicExcelPublicationModel();
			deliveryModel.putFormatConversionTarget(FormatConversionTarget.XLS);
			deliveryModel.setNoPersistence(true);
			deliveryModel.setFooter(false);
			deliveryModel.setCoverPage(false);
			SingleTableSearchDefinition def = contentDefinition
					.getSearchDefinition();
			def.setResultsPerPage(PUB_MAX_RESULTS);
			deliveryModel.setSuggestedFileName(SEUtilities.sanitiseFileName(def
					.toString().replace(" ", "_")));
			SearchResultsBase results = Registry.impl(CommonRemoteServletProvider.class)
					.getCommonRemoteServiceServlet().search(def, 0);
			publicationContent.searchResults = results;
			hasResults = true;
		}
	}

	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	@RegistryLocation(registryPoint = JaxbContextRegistration.class)
	public static class BasicExcelInfo extends RenderTransformWrapper {
		public BasicExcelContentDefinition cd;

		public String description;

		public BasicExcelPublicationModel pc;

		public ContentRequestBase<ContentDefinition> dm;

		public BasicExcelInfo() {
		}
	}

	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	@RegistryLocation(registryPoint = JaxbContextRegistration.class)
	// Unused
	public static class BasicExcelPublicationModel implements
			PublicationContent {
		@XmlTransient
		public SearchResultsBase searchResults;

		public BasicExcelPublicationModel() {
		}
	}

	@RegistryLocation(registryPoint = ContentRenderer.class, targetClass = BasicExcelPublicationModel.class)
	public static class BasicExcelPublicationModelRenderer
			extends
			ContentRenderer<BasicExcelContentDefinition, BasicExcelPublicationModel, ContentRequestBase> {
		public BasicExcelPublicationModelRenderer() {
		}

		@Override
		@SuppressWarnings("unchecked")
		protected void renderContent(long publicationId, long publicationUserId)
				throws Exception {
			// choose an xsl template based on
			BasicExcelInfo wr = new BasicExcelInfo();
			wr.cd = contentDefinition;
			wr.description = "";// wr.cd.toString();
			wr.pc = publicationContent;
			wr.dm = deliveryModel;
			wrapper = wr;
			ExcelExporter ee = new ExcelExporter();
			Document doc = ee.getTemplate();
			ee.addCollectionToBook(wr.pc.searchResults.getResults(), doc,
					"query_results");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			OutputStreamWriter writer = new OutputStreamWriter(baos, "UTF-8");
			XmlUtils.streamXML(doc, writer);
			results.bytes = baos.toByteArray();
		}
	}
}
