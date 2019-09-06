package cc.alcina.framework.servlet.grid;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
import cc.alcina.framework.common.client.publication.excel.BasicGridContentDefinition;
import cc.alcina.framework.common.client.publication.excel.BasicGridRequest;
import cc.alcina.framework.common.client.publication.request.ContentRequestBase;
import cc.alcina.framework.common.client.search.SingleTableSearchDefinition;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.XmlUtils;
import cc.alcina.framework.entity.util.CsvCols;
import cc.alcina.framework.servlet.CommonRemoteServletProvider;
import cc.alcina.framework.servlet.publication.ContentModelHandler;
import cc.alcina.framework.servlet.publication.ContentRenderer;
import cc.alcina.framework.servlet.publication.ContentRenderer.RenderTransformWrapper;

public class BasicGridPublisher {
	public static final int PUB_MAX_RESULTS = 100000;

	@RegistryLocation(registryPoint = ContentModelHandler.class, targetClass = BasicGridContentDefinition.class)
	public static class BasicExcelPublisherContentHandler extends
			ContentModelHandler<BasicGridContentDefinition, BasicGridPublicationModel, BasicGridRequest> {
		@Override
		protected void prepareContent() throws Exception {
			publicationContent = new BasicGridPublicationModel();
			deliveryModel.setNoPersistence(true);
			deliveryModel.setFooter(false);
			deliveryModel.setCoverPage(false);
			SingleTableSearchDefinition def = contentDefinition
					.getSearchDefinition();
			def.setResultsPerPage(PUB_MAX_RESULTS);
			deliveryModel.setSuggestedFileName(SEUtilities
					.sanitiseFileName(def.toString().replace(" ", "_")));
			SearchResultsBase results = Registry
					.impl(CommonRemoteServletProvider.class)
					.getCommonRemoteServiceServlet().search(def, 0);
			publicationContent.searchResults = results;
			hasResults = true;
		}
	}

	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	@RegistryLocation(registryPoint = JaxbContextRegistration.class)
	public static class BasicGridInfo extends RenderTransformWrapper {
		public BasicGridContentDefinition cd;

		public String description;

		public BasicGridPublicationModel pc;

		public ContentRequestBase<ContentDefinition> dm;

		public BasicGridInfo() {
		}
	}

	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	@RegistryLocation(registryPoint = JaxbContextRegistration.class)
	// Unused
	public static class BasicGridPublicationModel
			implements PublicationContent {
		@XmlTransient
		public SearchResultsBase searchResults;

		public BasicGridPublicationModel() {
		}
	}

	@RegistryLocation(registryPoint = ContentRenderer.class, targetClass = BasicGridPublicationModel.class)
	public static class BasicGridPublicationModelRenderer extends
			ContentRenderer<BasicGridContentDefinition, BasicGridPublicationModel, ContentRequestBase> {
		public BasicGridPublicationModelRenderer() {
		}

		@Override
		@SuppressWarnings("unchecked")
		protected void renderContent(long publicationId, long publicationUserId)
				throws Exception {
			// choose an xsl template based on
			BasicGridInfo wr = new BasicGridInfo();
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
			if (deliveryModel
					.provideTargetFormat() == FormatConversionTarget.XLSX) {
				results.bytes = baos.toByteArray();
			} else if (deliveryModel
					.provideTargetFormat() == FormatConversionTarget.HTML) {
				results.bytes = baos.toByteArray();
				List<List> cellList = ee.getCellList();
				FormatBuilder fb = new FormatBuilder();
				fb.format("<table>\n");
				for (List tr : cellList) {
					fb.format("<tr>\n");
					for (Object td : tr) {
						fb.format("<td valign='top'>\n");
						String s = CommonUtils.nullSafeToString(td);
						if (CommonUtils.isNullOrEmpty(s)) {
							s = "&nbsp;";
						}
						fb.format(s);
						fb.format("</td>\n");
					}
					fb.format("</tr>\n");
				}
				fb.format("</table>\n");
				results.htmlContent = fb.toString();
			} else if (deliveryModel
					.provideTargetFormat() == FormatConversionTarget.CSV) {
				CsvCols csvCols = new CsvCols((List) ee.getCellList());
				results.bytes = csvCols.toCsv()
						.getBytes(StandardCharsets.UTF_8);
			} else {
				throw new UnsupportedOperationException();
			}
		}
	}
}
