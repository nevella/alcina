package cc.alcina.framework.common.client.traversal;

import java.util.function.Function;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.traversal.layer.Measure;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.common.client.util.Ax;

public abstract class DocumentSelection extends MeasureSelection {
	protected DomDocument document;

	protected Loader loader;

	public DocumentSelection(AbstractUrlSelection parent,
			DocumentSelection.Loader loader) {
		super(parent, null, parent.get());
		this.loader = loader;
	}

	public DocumentSelection(PlainTextSelection parent) {
		super(parent, null, parent.getClass().getName());
		String text = Ax.ntrim(parent.get());
		document = DomDocument.createTextContainer(text);
		documentToMeasure();
	}

	public DocumentSelection(Selection parent, DomDocument document) {
		super(parent, null, parent.getClass().getName());
		this.document = document;
		documentToMeasure();
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

	private Measure documentToMeasure() {
		Location.Range documentRange = document.getLocationRange();
		Measure measure = new Measure(documentRange.start, documentRange.end,
				null);
		set(measure);
		return measure;
	}

	@Override
	public void releaseResources() {
		super.releaseResources();
		document = null;
	}

	public interface Loader<S extends Selection> {
		DomDocument load(S selection) throws Exception;

		Class<S> selectionClass();
	}

	public abstract static class TransformLayer<I extends Selection, O extends DocumentSelection>
			extends Layer<I> {
		private Function<I, O> transform;

		public TransformLayer(Function<I, O> transform) {
			this.transform = transform;
		}

		@Override
		public void process(I selection) throws Exception {
			select(transform.apply(selection));
		}
	}
}
