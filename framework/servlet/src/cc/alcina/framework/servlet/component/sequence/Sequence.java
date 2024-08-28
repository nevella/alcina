package cc.alcina.framework.servlet.component.sequence;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.domain.IdOrdered;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer.DeserializerOptions;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.util.FileUtils;
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

		static void writeElements(File folder,
				List<? extends IdOrdered> elements) {
			folder.mkdirs();
			SEUtilities.deleteDirectory(folder, true);
			elements.forEach(e -> {
				File writeTo = FileUtils.child(folder,
						String.valueOf(e.getId()) + ".json");
				Io.write().asReflectiveSerialized(true).object(e)
						.toFile(writeTo);
			});
			LoggerFactory.getLogger(Loader.class).info(
					"Logged {} sequence elements to {}", elements.size(),
					folder);
		}
	}

	/*
	 * Children must have a no-args constructor that populates the fields
	 */
	public static abstract class AbstractLoader implements Sequence.Loader {
		String path;

		String name;

		Function<String, String> serializationRefactoringHandler;

		Sequence.Abstract<?> sequence;

		public AbstractLoader(String path, String name,
				Function<String, String> serializationRefactoringHandler,
				Sequence.Abstract<?> sequence) {
			this.path = path;
			this.name = name;
			this.serializationRefactoringHandler = serializationRefactoringHandler;
			this.sequence = sequence;
		}

		@Override
		public Sequence<?> load(String location) {
			sequence.name = name;
			try {
				sequence.elements = (List) SEUtilities
						.listFilesRecursive(path, null).stream()
						.filter(f -> f.isFile())
						.map(f -> Io.read().file(f).asString())
						.map(serializationRefactoringHandler::apply)
						.map(s -> ReflectiveSerializer.deserialize(s,
								new DeserializerOptions()
										.withContinueOnException(true)))
						.sorted().collect(Collectors.toList());
			} catch (Exception e) {
				e.printStackTrace();
				sequence.name += " (load exception)";
				sequence.elements = new ArrayList<>();
			}
			return sequence;
		}
	}
}
