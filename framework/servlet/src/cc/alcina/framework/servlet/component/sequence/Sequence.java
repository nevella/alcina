package cc.alcina.framework.servlet.component.sequence;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public interface Sequence<T> {
	List<T> getElements();

	String getName();

	ModelTransform<T, ? extends Model> getRowTransform();

	ModelTransform<T, ? extends Model> getDetailTransform();

	public abstract static class Abstract<T> implements Sequence<T> {
		public List<T> elements;

		public List<T> getElements() {
			return elements;
		}

		public String name;

		public String getName() {
			return name;
		}
	}

	public static class Blank extends Abstract<String> {
		@Override
		public ModelTransform<String, ? extends Model> getRowTransform() {
			return new LeafModel.HtmlBlock.To();
		}

		@Override
		public ModelTransform<String, ? extends Model> getDetailTransform() {
			return new LeafModel.HtmlBlock.To();
		}

		public static class LoaderImpl implements Loader {
			@Override
			public boolean handlesSequenceLocation(String location) {
				return Ax.isBlank(location);
			}

			@Override
			public Sequence<?> load(String location) {
				Blank blank = new Blank();
				blank.name = "No sequence specified";
				blank.elements = new ArrayList<>();
				return blank;
			}
		}
	}

	@Registration(Loader.class)
	public interface Loader {
		// this can be a name, or a name+parameters
		boolean handlesSequenceLocation(String location);

		Sequence<?> load(String location);

		static Loader getLoader(String sequenceKey) {
			Loader loader = Registry.query(Sequence.Loader.class)
					.implementations()
					.filter(l -> l.handlesSequenceLocation(sequenceKey))
					.findFirst().orElse(new Blank.LoaderImpl());
			return loader;
		}
	}
}
