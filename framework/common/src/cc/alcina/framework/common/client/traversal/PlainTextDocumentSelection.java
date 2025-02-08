package cc.alcina.framework.common.client.traversal;

import java.util.function.Function;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.traversal.AbstractSelection.AllowsNullValue;

public class PlainTextDocumentSelection extends TextSelection
		implements PlainTextSelection, AllowsNullValue {
	protected String text;

	protected Loader loader;

	public Exception loadException;

	public void setLoader(Loader loader) {
		this.loader = loader;
	}

	public PlainTextDocumentSelection(Selection parent) {
		this(parent, null, null);
	}

	public PlainTextDocumentSelection(Selection parent,
			PlainTextDocumentSelection.Loader loader) {
		this(parent, loader, null);
	}

	public PlainTextDocumentSelection(Selection parent,
			PlainTextDocumentSelection.Loader loader, String pathSegment) {
		super(parent, null, pathSegment);
		this.loader = loader;
	}

	public PlainTextDocumentSelection(Selection parentSelection, String text) {
		super(parentSelection, text);
	}

	public String getText() {
		if (text == null) {
			try {
				text = loader.load(ancestorSelection(loader.selectionClass()));
				if (text == null) {
					loadException = new Exception("loader returned null");
				}
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}
		return this.text;
	}

	public interface Loader<S extends Selection> {
		String load(S selection) throws Exception;

		default Class<S> selectionClass() {
			return Reflections.at(getClass()).firstGenericBound();
		}
	}

	public abstract static class TransformLayer<I extends Selection, O extends PlainTextDocumentSelection>
			extends Layer<I> {
		Function<I, O> transform;

		public Function<I, O> getTransform() {
			return transform;
		}

		public TransformLayer(Function<I, O> transform) {
			this.transform = transform;
		}

		@Override
		public void process(I selection) throws Exception {
			select(transform.apply(selection));
		}
	}
}