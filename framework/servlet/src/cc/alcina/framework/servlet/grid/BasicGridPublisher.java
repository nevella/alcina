package cc.alcina.framework.servlet.grid;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Date;
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
import cc.alcina.framework.common.client.publication.grid.BasicGridContentDefinition;
import cc.alcina.framework.common.client.publication.grid.BasicGridRequest;
import cc.alcina.framework.common.client.publication.request.ContentRequestBase;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.search.SingleTableSearchDefinition;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CommonUtils.DateStyle;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.XmlUtils;
import cc.alcina.framework.entity.util.CsvCols;
import cc.alcina.framework.entity.util.JacksonUtils;
import cc.alcina.framework.gwt.client.entity.search.BindableSearchDefinition;
import cc.alcina.framework.gwt.client.entity.search.ModelSearchResults;
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
			SearchDefinition def = contentDefinition.getSearchDefinition();
			def.setResultsPerPage(PUB_MAX_RESULTS);
			String defName = def.toString();
			defName = Ax.blankTo(defName, () -> Ax.format("%s-%s",
					def.getClass().getSimpleName(),
					CommonUtils.formatDate(new Date(), DateStyle.TIMESTAMP)));
			if (def instanceof SingleTableSearchDefinition) {
				deliveryModel.setSuggestedFileName(SEUtilities
						.sanitiseFileName(defName.replace(" ", "_")));
				SearchResultsBase results = Registry
						.impl(CommonRemoteServletProvider.class)
						.getCommonRemoteServiceServlet().search(def, 0);
				publicationContent.resultRows = results.getResults();
			} else if (def instanceof BindableSearchDefinition) {
				BindableSearchDefinition bdef = (BindableSearchDefinition) def;
				deliveryModel.setSuggestedFileName(SEUtilities
						.sanitiseFileName(defName.replace(" ", "_")));
				defName = Ax.blankTo(defName, () -> Ax.format("%s-%s",
						bdef.entityClass().getSimpleName(), CommonUtils
								.formatDate(new Date(), DateStyle.TIMESTAMP)));
				ModelSearchResults modelSearchResults = Registry
						.impl(CommonRemoteServletProvider.class)
						.getCommonRemoteServiceServlet().searchModel(bdef);
				publicationContent.resultRows = modelSearchResults.queriedResultObjects;
			} else {
				throw new UnsupportedOperationException();
			}
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
		public List resultRows;

		public BasicGridPublicationModel() {
		}
	}

	@RegistryLocation(registryPoint = ContentRenderer.class, targetClass = BasicGridPublicationModel.class)
	public static class BasicGridPublicationModelRenderer extends
			ContentRenderer<BasicGridContentDefinition, BasicGridPublicationModel, ContentRequestBase> {
		public BasicGridPublicationModelRenderer() {
		}

		@Override
		protected void renderContent(long publicationId, long publicationUserId)
				throws Exception {
			// choose an xsl template based on
			BasicGridInfo wr = new BasicGridInfo();
			wr.cd = contentDefinition;
			wr.description = "";// wr.cd.toString();
			wr.pc = publicationContent;
			wr.dm = deliveryModel;
			wrapper = wr;
			ExcelExporter exporter = new ExcelExporter();
			Document doc = exporter.getTemplate();
			exporter.addCollectionToBook(wr.pc.resultRows, doc,
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
				List<List> cellList = exporter.getCellList();
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
				CsvCols csvCols = new CsvCols((List) exporter.getCellList());
				results.bytes = csvCols.toCsv()
						.getBytes(StandardCharsets.UTF_8);
			} else if (deliveryModel
					.provideTargetFormat() == FormatConversionTarget.JSON) {
				List<List> cellList = exporter.getCellList();
				String json = JacksonUtils.toNestedJsonList(cellList);
				results.bytes = json.getBytes(StandardCharsets.UTF_8);
			} else {
				throw new UnsupportedOperationException();
			}
		}
	}
}
