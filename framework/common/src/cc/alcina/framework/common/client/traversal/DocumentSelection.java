package cc.alcina.framework.common.client.traversal;

import java.util.function.Function;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.traversal.layer.Slice;
import cc.alcina.framework.common.client.util.Ax;

public abstract class DocumentSelection extends Slice.SliceSelection {
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
		documentToSlice();
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
	public Slice get() {
		Slice value = super.get();
		if (value != null) {
			return value;
		} else {
			ensureDocument();
			return documentToSlice();
		}
	}

	private Slice documentToSlice() {
		Location.Range documentRange = document.getLocationRange();
		Slice slice = new Slice(documentRange.start, documentRange.end, null);
		set(slice);
		return slice;
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
