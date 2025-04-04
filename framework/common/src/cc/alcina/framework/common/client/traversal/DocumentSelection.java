package cc.alcina.framework.common.client.traversal;

import java.util.function.Function;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.process.TreeProcess.HasReleaseableResources;
import cc.alcina.framework.common.client.traversal.AbstractSelection.AllowsNullValue;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.common.client.util.Ax;

public abstract class DocumentSelection extends MeasureSelection
		implements AllowsNullValue, HasReleaseableResources {
	protected DomDocument document;

	protected Loader loader;

	public DocumentSelection(Selection parent,
			DocumentSelection.Loader loader) {
		super(parent, null, Ax.format("url2doc:%s", parent.getPathSegment()));
		this.loader = loader;
	}

	public DocumentSelection(PlainTextSelection parent,
			boolean normaliseWhitespace) {
		super(parent, null);
		String text = parent.get();
		if (normaliseWhitespace) {
			text = Ax.ntrim(text);
		}
		document = DomDocument.createTextContainer(text);
		documentToMeasure();
	}

	public DocumentSelection(Selection parent, DomDocument document) {
		super(parent, null);
		this.document = document;
		documentToMeasure();
	}

	private Measure documentToMeasure() {
		Location.Range documentRange = document.getLocationRange();
		Measure measure = new Measure(documentRange.start, documentRange.end,
				DocumentToken.TYPE);
		set(measure);
		return measure;
	}

	public DomDocument ensureDocument() {
		try {
			if (document == null) {
				document = loader
						.load(ancestorSelection(loader.selectionClass()));
			}
			return document;
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}

	@Override
	public Measure get() {
		Measure value = super.get();
		if (value != null) {
			return value;
		} else {
			ensureDocument();
			return documentToMeasure();
		}
	}

	@Override
	public void releaseResources() {
		if (super.get() != null) {
			super.releaseResources();
		}
		document = null;
	}

	public static class DocumentToken implements Measure.Token {
		public static final DocumentToken TYPE = new DocumentToken();

		private DocumentToken() {
		}
	}

	public interface Loader<S extends Selection> {
		DomDocument load(S selection) throws Exception;

		Class<S> selectionClass();
	}

	public abstract static class TransformLayer<I extends Selection, O extends DocumentSelection>
			extends Layer<I> {
		protected Function<I, O> transform;

		/*
		 * Only for subtypes that generate documents imperatively
		 */
		public TransformLayer() {
		}

		public TransformLayer(Function<I, O> transform) {
			this.transform = transform;
		}

		@Override
		public void process(I selection) throws Exception {
			select(transform.apply(selection));
		}
	}

	/*
	 * Marker, documentselection views shd be async
	 */
	public static class View<D extends DocumentSelection>
			extends MeasureSelection.View<D> implements ViewAsync {
	}
}
