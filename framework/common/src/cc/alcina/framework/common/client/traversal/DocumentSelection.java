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

	@Override
	public Slice get() {
		Slice value = super.get();
		if (value != null) {
			return value;
		} else {
			try {
				document = loader
						.load(ancestorSelection(AbstractUrlSelection.class));
				return documentToSlice();
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}
	}

	private Slice documentToSlice() {
		Location.Range documentRange = document.getLocationRange();
		Slice slice = new Slice(documentRange.start, documentRange.end, null);
		set(slice);
		return slice;
	}

	public interface Loader {
		DomDocument load(AbstractUrlSelection selection) throws Exception;
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
