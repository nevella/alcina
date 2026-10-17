package cc.alcina.framework.gwt.client.dirndl.model.fragment.doc;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.mutations.MutationRecord;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.traversal.HasMarkup;
import cc.alcina.framework.common.client.traversal.layer.diff.MeasureDiff;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.Events.EmissionStyle;
import cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents.Mutation;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Rendered;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentModel.ModelMutation;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentNodeOps;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.MappingResolver.ResolvedMapping;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.MarkupSchema.FragmentDocumentClass;

/**
 * <p>
 * Models a FragmentNode document. Instances should be constructed using the
 * static Builder, which handles interdependencies between the domDocument, root
 * and markupSchema fields
 *
 * <p>
 * The domDocument (and backing org.w3c.xml.Document) remain in sync with this
 * object model via the magic of Dirndl bidi transformation
 * 
 * <p>
 * Note that the document is *not* present in the Dirndl node hierarchy (the
 * topmost model is {@link #root}) - so it does not (directly) receive dirndl
 * events. Mutations are routed to it from the root instance.
 *
 * <p>
 * Todo - validation - which will be implemented on FragmentNode since it's a
 * descent function. For an older discontinuted approach, which may have some
 * value at implementation time, see the nested Parser class in commit history
 *
 * @param <R>
 *            the root node type
 *
 */
/*
 * Debugging text node detach (using xerces/w3c dom events);
 * @formatter:off
 * 
 
 DocumentTransformerPeer.java
 
 protected void init() {
		createPeers();
		traversal = new SelectionTraversal(this);
		TreeProcess.FragmentDocumentNode parentNode = JobContext.getSelectedProcessNode();
		traversal.select(new DocumentTransformerSelection(parentNode, this));
		this.outputDocument = createOutputDocument();
		EventListener listener = new EventListener() {
			@Override
			public void handleEvent(Event evt) {
				throw new UnsupportedOperationException(
						"Unimplemented method 'handleEvent'");
			}
		};
		((EventTarget) outputDocument.getDomDocument().w3cNode())
				.addEventListener("DOMNodeRemovedFromDocument", listener, true);
		traversal.outputContainer = outputDocument;
	}


	
 * @formatter:on
 * 
 * 
 */
@Registration.Self
public abstract class FragmentDocument<R extends FragmentDocumentRoot> extends
		FragmentDocumentNode implements ModelMutation.Handler, HasMarkup {
	public static class TransformationOptions {
		public boolean insertBeginEndElementMarkers;

		public boolean insertComputedInserts;

		public ComputedMarkerType computedMarkerType;
	}

	public interface HasOutputDocument {
		FragmentDocument<?> getOutputDocument();
	}

	public static class ExceptionContext {
		public RuntimeException exception;

		public FragmentDocumentNode node;

		public ParserContext parserContext;

		public DomNode domNode;
	}

	public static class ParserContext {
		public interface ErrorHandler {
			public static ErrorHandler DEFAULT = new ErrorHandler() {
				@Override
				public void handleException(ExceptionContext exceptionContext) {
					throw exceptionContext.exception;
				}
			};

			void handleException(ExceptionContext exceptionContext);
		}

		private ErrorHandler errorHandler = ErrorHandler.DEFAULT;

		public ErrorHandler getErrorHandler() {
			return this.errorHandler;
		}

		public void onException(ParserException exception) {
			ExceptionContext exceptionContext = new ExceptionContext();
			exceptionContext.exception = exception;
			exceptionContext.domNode = exception.domNode;
			exceptionContext.parserContext = this;
			errorHandler.handleException(exceptionContext);
		}

		public void setErrorHandler(ErrorHandler errorHandler) {
			this.errorHandler = errorHandler;
		}
	}

	public static class ParserException extends IllegalStateException {
		private DomNode domNode;

		private FragmentDocumentNode node;

		public ParserException(String s, DomNode domNode) {
			super(s);
			this.domNode = domNode;
		}

		public ParserException(String s, FragmentDocumentNode node) {
			super(s);
			this.node = node;
		}

		public DomNode getDomNode() {
			return this.domNode;
		}

		public FragmentDocumentNode getNode() {
			return this.node;
		}
	}

	/**
	 * <p>
	 * Implementing classes are part of the non-general fragmentDocument schema
	 * (so should be ignored by parsers)
	 *
	 * <p>
	 * For the moment, I'm not marking new node types (e.g.
	 * LegislativeInstrument.DocumentTitle) as SpecialisedDescendants, since I
	 * think reachability tracing to work out what nodes are valid where will
	 * work, in which case this marker interface will be unnecessary. But
	 * maintaining a watching brief...
	 *
	 */
	public interface SpecialisedDescendant {
	}

	public static class Builder {
		Class<? extends FragmentDocument> documentClass;

		MarkupSchema markupSchema = FragmentDocumentClass.TYPE;;

		FragmentDocumentRoot root;

		FragmentDocumentVersion version;

		DomDocument fromDocument;

		boolean forOutput;

		Class<? extends FragmentDocumentRoot> nodeType;

		// a unique string per persistent document (such as an EntityLocator
		// serialized form), if any. Used to namespace per-document styles
		// in a multi-doc context (multiple extracts, etc)
		String persistentId = "non-persistent";

		boolean asGwtDoc;

		public Builder withAsGwtDoc(boolean asGwtDoc) {
			this.asGwtDoc = asGwtDoc;
			return this;
		}

		public Builder withVersion(FragmentDocumentVersion version) {
			this.version = version;
			return this;
		}

		public Builder withPersistentId(String persistentId) {
			this.persistentId = persistentId;
			return this;
		}

		public Builder withDocumentClass(
				Class<? extends FragmentDocument> documentClass) {
			this.documentClass = documentClass;
			return this;
		}

		public Builder withMarkupSchema(MarkupSchema markupSchema) {
			this.markupSchema = markupSchema;
			return this;
		}

		public Builder withRoot(FragmentDocumentRoot root) {
			this.root = root;
			return this;
		}

		public Builder withFromDocument(DomDocument fromDocument) {
			this.fromDocument = fromDocument;
			computeDocumentClass();
			return this;
		}

		void computeDocumentClass() {
			if (documentClass != null) {
				return;
			}
			/*
			 * Attempt to compute the documentClass using the markupSchema
			 */
			DomNode documentElement = fromDocument.getDocumentElementNode();
			Class<? extends FragmentDocumentRoot> rootClass = Registry
					.query(FragmentDocumentRoot.class).registrations()
					.filter(rootType -> new MappingResolver(markupSchema, null,
							rootType).getResolvedMapping()
									.matches(documentElement))
					.findFirst().orElse(GenericDocumentRoot.class);
			try {
				documentClass = Registry.query(FragmentDocument.class)
						.registrations()
						.filter(clazz -> clazz != FragmentDocument.class)
						.filter(docClass -> (Class<?>) Reflections.at(docClass)
								.firstGenericBound() == rootClass)
						.findFirst().get();
			} catch (RuntimeException e) {
				Ax.sysLogHigh("Unable to compute documentClass - rootClass: %s",
						rootClass);
				e.printStackTrace();
				throw e;
			}
		}

		public Builder
				withNodeType(Class<? extends FragmentDocumentRoot> nodeType) {
			this.nodeType = nodeType;
			return this;
		}

		public <AD extends FragmentDocument> AD build() {
			FragmentDocument document = Reflections.newInstance(documentClass);
			document.init(this);
			return (AD) document;
		}

		public Builder forOutput() {
			this.forOutput = true;
			return this;
		}
	}

	public class DocumentBinding implements ModelMutation.Handler {
		Map<FragmentNode, FragmentNode> bindings = new LinkedHashMap<>();

		public void bindTo(FragmentDocument other) {
			bindings.put(other.getRoot(), getRoot());
			other.getRoot().stream().forEach(fn -> {
				if (!bindings.containsKey(fn)) {
					FragmentNode localParent = bindings.get(fn.parent());
					FragmentNode localClone = fragmentModel.copyExternal(fn);
					localParent.nodes().append(localClone);
					bindings.put(fn, localClone);
				}
			});
			// (initially r/o)
			//
			// descend other
			//
			// create in this (which generates mutations)
			//
			//
			// later, call pump events [what this means, I think, is that we
			// could also use an environment/event pump. But since the code
			// needs to pump events before continuing, imperative is probably
			// better - i.e. I don't think any changes are needed]
		}

		public void pumpEvents() {
			fragmentModel().events.pump();
		}

		@Override
		public void onModelMutation(ModelMutation event) {
			/*
			 * Ensure the internals of each FragmentNode match the css-like
			 * computedNodes
			 */
			Set<FragmentNodeOps> affectedNodes = event.getData()
					.provideAffectedFragmentNodes();
			affectedNodes.forEach(FragmentNodeOps::ensureComputedNodes);
		}
	}

	static class TexAttributes {
		static String texAttributeRepresentation(FragmentNode node) {
			StringMap map = node.getDirectedPropertyBindingValues();
			AttributeMappingType.Ref ref = Reflections.at(node)
					.annotation(AttributeMappingType.Ref.class);
			if (ref == null) {
				return "";
			}
			AttributeMappingType mappingType = ref.value();
			switch (mappingType) {
			case NONE:
				Preconditions.checkState(map.isEmpty());
				return "";
			case SINGLE: {
				Preconditions.checkState(map.keySet().size() <= 1);
				String repr = map.keySet().stream().sorted()
						.map(k -> Ax.format("%s", map.get(k)))
						.collect(Collectors.joining("|"));
				return Ax.format("[%s]", repr);
			}
			case MULTIPLE: {
				String repr = map.keySet().stream().sorted()
						.map(k -> Ax.format("%s=%s", k, map.get(k)))
						.collect(Collectors.joining("|"));
				return Ax.format("[%s]", repr);
			}
			default:
				throw new UnsupportedOperationException();
			}
		}
	}

	public static class Diff {
		public static void stripRightInserts(DomNode node) {
			new MeasureDiff.Peer().stripRightInserts(node);
		}
	}

	public static boolean isFragmentDocument(DomDocument document) {
		// todo - a schema ref in the root node would be much, much better
		DomNode root = document.getDocumentElementNode();
		return root.tagIsOneOf("judgment", "doc");
	}

	public static Builder builder() {
		return new Builder();
	}

	protected DomDocument domDocument;

	Map<DomNode, FragmentDocumentNode> domNodeNode = AlcinaCollections
			.newUnqiueMap();

	protected R root;

	private ParserContext parserContext = new ParserContext();

	public MarkupSchema markupSchema;

	protected Class<R> nodeType;

	private Footnotes footnotes;

	private Sections sections;

	DocumentBinding documentBinding = new DocumentBinding();

	boolean asGwtDoc;

	@Override
	public String provideMarkup() {
		return domDocument.toPrettyMarkup();
	}

	/*
	 * Delegate, since there's no dirndl node corresponding to this doc
	 */
	@Override
	public <N extends FragmentNode> Stream<N> byType(Class<N> clazz) {
		return root.byType(clazz);
	}

	public TransformationOptions getTransformationOptions() {
		return fragmentModelImpl().transformationOptions;
	}

	public DomDocument getDomDocument() {
		return this.domDocument;
	}

	@Override
	public void onModelMutation(ModelMutation event) {
		documentBinding.onModelMutation(event);
	}

	public ParserContext getParserContext() {
		return this.parserContext;
	}

	public R getRoot() {
		return this.root;
	}

	public <AD extends FragmentDocument> AD link() {
		FragmentDocumentResolver resolver = new FragmentDocumentResolver(
				markupSchema, nodeType, domDocument, root, asGwtDoc);
		/*
		 * If the document is non-null, this will parse (generate the dirndl
		 * node tree/fragment node models)
		 */
		Rendered rendered = new DirectedLayout().render(resolver, root)
				.getRendered();
		domDocument = rendered.asDomNode().document;
		return (AD) this;
	}

	public abstract Class<? extends SpecialisedDescendant>
			permittedSpecialisedDescendants();

	public Footnotes footnotes() {
		if (footnotes == null) {
			footnotes = new Footnotes(this);
		}
		return footnotes;
	}

	public Sections sections() {
		if (sections == null) {
			sections = new Sections(this);
		}
		return sections;
	}

	public ReversePagination reversePagination() {
		return new ReversePagination(this);
	}

	public <AD extends FragmentDocument> AD
			createBlankDocument(MarkupSchema markupSchema) {
		Builder builder = new Builder().withDocumentClass(getClass());
		builder.withMarkupSchema(markupSchema);
		return builder.build();
	}

	public DocumentBinding getDocumentBinding() {
		return documentBinding;
	}

	protected TransformingFragmentModel<R> fragmentModelImpl() {
		return (TransformingFragmentModel<R>) fragmentModel;
	}

	protected void createFragmentModel() {
		fragmentModel = new TransformingFragmentModel<>(this, root);
		fragmentModel.events.emissionStyle = EmissionStyle.NONE;
		/*
		 * Fixme - better, traverse from the root node and add reachable node
		 * types (but requires a complete schema)
		 */
		List<ResolvedMapping> matchers = markupSchema.schemaMappings
				.getMappings(permittedSpecialisedDescendants()).mappings
						.values().stream().collect(Collectors.toList());
		Collections.reverse(matchers);
		matchers.forEach(m -> fragmentModel.addModelled(m.nodeType()));
	}

	protected void init(Builder builder) {
		this.root = (R) builder.root;
		if (root == null) {
			root = (R) Reflections.newInstance(
					Reflections.at(getClass()).firstGenericBound());
			if (builder.forOutput) {
				root.version = builder.version == null
						? new FragmentDocumentVersion.UNKNOWN()
						: builder.version;
				root.persistentId = builder.persistentId;
			} else {
				Directed directed = Reflections.at(root)
						.annotation(Mapping.class).directed();
				if (builder.fromDocument == null) {
					String markup = Ax.format(
							"<%s class=\"%s\"><div>[Blank working document]</div></%s>",
							directed.tag(), directed.className(),
							directed.tag());
					builder.fromDocument = DomDocument.from(markup, true);
				}
				builder.asGwtDoc = true;
				String versionStr = builder.fromDocument
						.getDocumentElementNode().attr("version");
				root.version = FragmentDocumentVersion.valueOf(versionStr);
			}
			//
		}
		this.asGwtDoc = builder.asGwtDoc;
		this.root.document = this;
		this.domDocument = builder.fromDocument;
		this.markupSchema = builder.markupSchema;
		this.nodeType = (Class<R>) builder.nodeType;
		createFragmentModel();
		root.setFragmentModel(fragmentModel);
	}

	public static FragmentDocument<?> from(String markup) {
		Builder documentBuilder = FragmentDocument.builder()
				.withMarkupSchema(MarkupSchema.FragmentDocumentClass.TYPE)
				.withFromDocument(DomDocument.from(markup, true));
		FragmentDocument doc = documentBuilder.build();
		doc.link();
		return doc;
	}

	public void bindToDocumentMutations() {
		fragmentModel().events.emissionStyle = EmissionStyle.MANUAL;
		TopicListener<List<MutationRecord>> listener = list -> {
			Mutation mutation = new Mutation();
			mutation.records = list;
			fragmentModel.onMutation(mutation);
		};
		domDocument.gwtDoc().addBatchedLocalMutationListener(listener);
	}

	public void emitBatchedMutations() {
		domDocument.gwtDoc().flushLocalMutations();
		fragmentModel().events.pump();
	}
}
