package cc.alcina.framework.common.client.traversal;

import java.util.function.Function;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.traversal.layer.Measure;
import cc.alcina.framework.common.client.util.Ax;

public abstract class DocumentSelection extends Measure.MeasureSelection {
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

	public interface Loader<S extends Selection> {
		DomDocument load(S selection) throws Exception;

		Class<S> selectionClass();
	}

	public abstract static class TransformLayer<I extends Selection, O extends DocumentSelection>
			extends Layer<I> {
		private Function<I, O> transform;

		public TransformLayer(Class<I> input, Class<O> output,
				Function<I, O> transform) {
			super(input, output);
			this.transform = transform;
		}

		@Override
		public void process(SelectionTraversal traversal, I selection)
				throws Exception {
			traversal.select(transform.apply(selection));
		}
	}
}
