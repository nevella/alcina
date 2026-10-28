package cc.alcina.framework.servlet.transform.fragment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.dom.Measure.Token.Order;
import cc.alcina.framework.common.client.process.TreeProcess;
import cc.alcina.framework.common.client.traversal.DocumentTransformationTraversal;
import cc.alcina.framework.common.client.traversal.HasMarkup;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.TraversalContext;
import cc.alcina.framework.common.client.traversal.layer.SelectionMarkup;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocument;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocumentRoot;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocumentVersion;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.GenericDocument;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.MarkupSchema;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.FragmentDocumentTokenOrder;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens.RootToken.HasFragmentDocumentRoot;
import cc.alcina.framework.servlet.component.traversal.SelectionMarkupFull;
import cc.alcina.framework.servlet.job.JobContext;
import cc.alcina.framework.servlet.transform.fragment.html.ImageLayer;
import cc.alcina.framework.servlet.transform.fragment.html.ImageLayer.Peer;
import cc.alcina.framework.servlet.transform.fragment.html.LinkLayer;
import cc.alcina.framework.servlet.transform.fragment.html.OutputCleanupLayer;
import cc.alcina.framework.servlet.transform.fragment.html.ParaLayer;
import cc.alcina.framework.servlet.transform.fragment.html.StyleAttributeLayer;
import cc.alcina.framework.servlet.transform.fragment.html.TableLayer;
import cc.alcina.framework.servlet.transform.fragment.output.MeasureSplitAndHandleTextOverlaps;
import cc.alcina.framework.servlet.transform.fragment.pos.BlockIndentLayer;
import cc.alcina.framework.servlet.transform.fragment.pos.ListItemLabelLayer;
import cc.alcina.framework.servlet.transform.fragment.pos.MergedIndentLayer;
import cc.alcina.framework.servlet.transform.fragment.pos.OutputIndentNormalisation;
import cc.alcina.framework.servlet.transform.fragment.pos.PositioningLayer;
import cc.alcina.framework.servlet.transform.fragment.structure.SectionHeadingLayer;

/**
 * <p>
 * This class provides the basis for a transformation from an input document (in
 * theory arbitrary HTML, but mostly specialised to MSWord html) to a
 * FragmentDocument document
 * <p>
 * Customisation of each layer is controlled by the Peer fields - e.g.
 * styledDocumentPeer, listItemLabelPeer etc. To customise a layer, set the peer
 * field to a process-specific subclass in the {@link #createPeers} method of
 * the {@link DocumentTransformerPeer} subclass. } for an extensive example
 * 
 */
public abstract class DocumentTransformerPeer
		implements HasFragmentDocumentRoot, HasMarkup, TraversalContext,
		Measure.Token.Order.Has, StyleAttributeLayer.Peer.Has,
		SectionHeadingLayer.Peer.Has, ListItemLabelLayer.Peer.Has,
		OutputIndentNormalisation.Peer.Has, BlockIndentLayer.Peer.Has,
		TableLayer.Peer.Has, ParaLayer.Peer.Has, LinkLayer.Peer.Has,
		PositioningLayer.Peer.Has, SelectionMarkup.Has,
		StyledNodeLayer.Peer.Has, OutputCleanupLayer.Peer.Has,
		MeasureSplitAndHandleTextOverlaps.Peer.Has,
		FragmentDocument.HasOutputDocument, ImageLayer.Peer.Has,
		MergedIndentLayer.Peer.Has {
	public static void logOutputDocument(String path) {
		String docMl = SelectionTraversal.contextTraversal()
				.context(FragmentDocument.HasOutputDocument.class)
				.getOutputDocument().getDomDocument().toPrettyMarkup();
		Io.write().string(docMl).toPath(path);
	}

	protected SelectionTraversal traversal;

	protected DocumentTransformer transformer;

	protected FragmentDocument outputDocument;

	protected StyledNodeLayer.Peer styledNodeLayerPeer;

	protected StyledDocumentPeer styledDocumentPeer;

	protected StyleAttributeLayer.Peer styleAttributeLayerPeer;

	protected SectionHeadingLayer.Peer sectionHeadingLayerPeer;

	protected ListItemLabelLayer.Peer listItemLabelPeer;

	protected OutputIndentNormalisation.Peer outputIndentNormalisationPeer;

	protected BlockIndentLayer.Peer blockIndentNormalisationPeer;

	protected TableLayer.Peer tableLayerPeer;

	protected ParaLayer.Peer paraLayerPeer;

	protected LinkLayer.Peer linkLayerPeer;

	protected PositioningLayer.Peer indentLayerPeer;

	protected OutputCleanupLayer.Peer outputCleanupLayerPeer;

	protected MeasureSplitAndHandleTextOverlaps.Peer measureSplitAndHandleTextOverlapsPeer;

	protected ImageLayer.Peer imageLayerPeer;

	protected MergedIndentLayer.Peer mergedIndentLayerPeer;

	public MergedIndentLayer.Peer getMergedIndentLayerPeer() {
		return mergedIndentLayerPeer;
	}

	public DocumentTransformerPeer() {
		init();
	}

	public MeasureSplitAndHandleTextOverlaps.Peer
			getMeasureSplitAndHandleTextOverlapsPeer() {
		return measureSplitAndHandleTextOverlapsPeer;
	}

	public OutputCleanupLayer.Peer getOutputCleanupLayerPeer() {
		return outputCleanupLayerPeer;
	}

	public PositioningLayer.Peer getIndentLayerPeer() {
		return indentLayerPeer;
	}

	public LinkLayer.Peer getLinkLayerPeer() {
		return linkLayerPeer;
	}

	public ParaLayer.Peer getParaLayerPeer() {
		return paraLayerPeer;
	}

	public StyledNodeLayer.Peer getStyledNodeLayerPeer() {
		return styledNodeLayerPeer;
	}

	public TableLayer.Peer getTableLayerPeer() {
		return tableLayerPeer;
	}

	public OutputIndentNormalisation.Peer getOutputIndentNormalisationPeer() {
		return outputIndentNormalisationPeer;
	}

	@Override
	public BlockIndentLayer.Peer getBlockIndentNormalisationPeer() {
		return blockIndentNormalisationPeer;
	}

	@Override
	public StyleAttributeLayer.Peer getStyleAttributeLayerPeer() {
		return styleAttributeLayerPeer;
	}

	@Override
	public SectionHeadingLayer.Peer getSectionHeadingLayerPeer() {
		return sectionHeadingLayerPeer;
	}

	@Override
	public ListItemLabelLayer.Peer getListItemLabelLayerPeer() {
		return listItemLabelPeer;
	}

	@Override
	public Order getOrder() {
		return new FragmentDocumentTokenOrder();
	}

	@Override
	public String provideMarkup() {
		return transformer.styledDocument.sourceDocument.toPrettyMarkup();
	}

	// apply logic which requires all resources
	public void afterTraversal() {
	}

	public void enterContext() {
	}

	public FragmentDocument<?> getOutputDocument() {
		return this.outputDocument;
	}

	@Override
	public Peer getImageLayerPeer() {
		return imageLayerPeer;
	}

	@Override
	public FragmentDocumentRoot provideRoot() {
		return (FragmentDocumentRoot) outputDocument.getRoot();
	}

	@Override
	public SelectionMarkup getSelectionMarkup() {
		getOutputDocument().getDomDocument().setReadonly(true);
		getOutputDocument().getDomDocument().invalidateLocations();
		return new SelectionMarkupFull(traversal);
	}

	protected final FragmentDocument createOutputDocument() {
		return FragmentDocument.builder()
				.withDocumentClass(getOutputDocumentClass())
				.withMarkupSchema(getOutputDocumentMarkupSchema())
				.withPersistentId(getPersistentId())
				.withVersion(getOutputDocumentMarkupVersion())
				.withAsGwtDoc(true).forOutput().build().link();
	}

	public String getPersistentId() {
		return null;
	}

	protected Class<? extends FragmentDocument> getOutputDocumentClass() {
		return GenericDocument.class;
	}

	protected MarkupSchema getOutputDocumentMarkupSchema() {
		return MarkupSchema.FragmentDocumentClass.TYPE;
	}

	protected FragmentDocumentVersion getOutputDocumentMarkupVersion() {
		return null;
	}

	protected abstract void initialiseTraversal();

	protected void init() {
		createPeers();
		traversal = new SelectionTraversal(this);
		TreeProcess.Node parentNode = JobContext.getSelectedProcessNode();
		traversal.select(new DocumentTransformerSelection(parentNode, this));
		this.outputDocument = createOutputDocument();
		traversal.outputContainer = new OutputContainer(outputDocument,
				getOutputDocumentCss(), getMarkupContainerClassnames());
	}

	protected String getOutputDocumentCss() {
		return "";
	}

	protected String getMarkupContainerClassnames() {
		return "fragment-document";
	}

	public static class OutputContainer
			implements DocumentTransformationTraversal.HasCss {
		FragmentDocument outputDocument;

		String css;

		String markupContainerClassNames;

		protected OutputContainer(FragmentDocument outputDocument, String css,
				String markupContainerClassNames) {
			this.outputDocument = outputDocument;
			this.css = css;
			this.markupContainerClassNames = markupContainerClassNames;
		}

		@Override
		public String provideCss() {
			return css;
		}

		@Override
		public String provideMarkupContainerClassNames() {
			return markupContainerClassNames;
		}
	}

	/*
	 * Creates peers, objects which customise the behaviour of various layers.
	 * Subclasses should call super.init() first, then create replacements
	 */
	protected void createPeers() {
		styledNodeLayerPeer = new StyledNodeLayer.Peer();
		styledDocumentPeer = new StyledDocumentPeer();
		styleAttributeLayerPeer = new StyleAttributeLayer.Peer();
		sectionHeadingLayerPeer = new SectionHeadingLayer.Peer();
		listItemLabelPeer = new ListItemLabelLayer.Peer();
		outputIndentNormalisationPeer = new OutputIndentNormalisation.Peer();
		blockIndentNormalisationPeer = new BlockIndentLayer.Peer();
		tableLayerPeer = new TableLayer.Peer();
		paraLayerPeer = new ParaLayer.Peer();
		linkLayerPeer = new LinkLayer.Peer();
		indentLayerPeer = new PositioningLayer.Peer();
		outputCleanupLayerPeer = new OutputCleanupLayer.Peer();
		measureSplitAndHandleTextOverlapsPeer = new MeasureSplitAndHandleTextOverlaps.Peer();
		imageLayerPeer = new ImageLayer.Peer();
		mergedIndentLayerPeer = new MergedIndentLayer.Peer();
	}

	/*
	 * Remove invalid table markup
	 */
	protected void removeInvalidMarkup(DomDocument doc) {
		new MarkupCleanup(doc).run();
	}

	public static class MarkupCleanup {
		static Logger logger = LoggerFactory.getLogger(MarkupCleanup.class);

		DomDocument doc;

		public MarkupCleanup(DomDocument doc) {
			this.doc = doc;
		}

		int changes = 0;

		int pass = 0;

		DomNode currentCheck;

		public void run() {
			do {
				changes = 0;
				pass++;
				doc.xpath("//table").forEach(n -> removeIfNot(n, "caption",
						"col", "colgroup", "tbody", "tfoot", "tr", "thead"));
				doc.xpath("//tbody").forEach(n -> removeIfNot(n, "tr"));
				doc.xpath("//tfoot").forEach(n -> removeIfNot(n, "tr"));
				doc.xpath("//thead").forEach(n -> removeIfNot(n, "tr"));
				doc.xpath("//table").forEach(this::validateTable);
				if (changes != 0) {
					logger.info("Pass {} - removed {} invalid markup nodes",
							pass, changes);
				}
			} while (changes != 0);
		}

		/*@formatter:off
		 * 
		 * W3C DTD:
		  <!ELEMENT TABLE - -
     (CAPTION?, (COL*|COLGROUP*), THEAD?, TFOOT?, TBODY+)>

	 	*@formatter:on

		 */
		/*
		 * This only validates thead/tfoot/tbody structure - comlaw markup
		 * (ITAA97) is double-headed...!
		 */
		void validateTable(DomNode table) {
			currentCheck = table;
			boolean seenBody = false;
			boolean seenHead = false;
			boolean seenFoot = false;
			for (DomNode child : table.children.elements()) {
				boolean remove = false;
				switch (child.name().toLowerCase()) {
				case "tr":
				case "tbody":
					seenBody = true;
					break;
				case "thead":
					if (seenBody || seenHead || seenFoot) {
						remove = true;
					}
					seenHead = true;
					break;
				case "tfoot":
					if (seenBody || seenFoot) {
						remove = true;
					}
					seenFoot = true;
					break;
				}
				if (remove) {
					stripOrRemove(child);
				}
			}
		}

		void removeIfNot(DomNode node, String... tagNames) {
			this.currentCheck = node;
			node.children.nodes().stream().filter(n -> {
				if (n.isText()) {
					return !n.isWhitespaceOrEmptyTextContent();
				} else if (n.isElement()) {
					return !n.tagIsOneOf(tagNames);
				} else {
					return false;
				}
			}).forEach(this::stripOrRemove);
		}

		void stripOrRemove(DomNode node) {
			logger.warn("Removing invalid markup from {} :: {}",
					currentCheck.name(), node.name());
			if (node.isText()) {
				node.removeFromParent();
			} else {
				node.strip();
			}
			changes++;
		}
	}

	public boolean isPdf2html() {
		return transformer.pdf2Html;
	}

	/**
	 * for subclasses to trim off navigation elements etc
	 * 
	 * @param sourceDocument
	 */
	public void removeExtraneousMarkup(DomDocument sourceDocument) {
	}
}
