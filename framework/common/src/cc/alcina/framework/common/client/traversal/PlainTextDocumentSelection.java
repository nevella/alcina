package cc.alcina.framework.common.client.traversal;

import java.util.function.Function;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.NestedNameProvider;

public class PlainTextDocumentSelection extends TextSelection
		implements PlainTextSelection {
	protected String text;

	protected Loader loader;

	public PlainTextDocumentSelection(Selection parent,
			PlainTextDocumentSelection.Loader loader, String pathSegment) {
		super(parent, null, pathSegment);
		this.loader = loader;
	}

	public PlainTextDocumentSelection(Selection parentSelection, String text) {
		super(parentSelection, text,
				NestedNameProvider.get(PlainTextDocumentSelection.class));
	}

	public String getText() {
		if (text == null) {
			try {
				text = loader.load(ancestorSelection(loader.selectionClass()));
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}
		return this.text;
	}

	public interface Loader<S extends Selection> {
		String load(S selection) throws Exception;

		Class<S> selectionClass();
	}

	public abstract static class TransformLayer<I extends Selection, O extends PlainTextDocumentSelection>
			extends Layer<I> {
		private Function<I, O> transform;

		public TransformLayer(Function<I, O> transform) {
			this.transform = transform;
		}

		@Override
		public void process(I selection) throws Exception {
			state.select(transform.apply(selection));
		}
	}
}