package cc.alcina.framework.servlet.component.sequence;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.domain.IdOrdered;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.NotificationObservable;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer.DeserializerOptions;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.util.FileUtils;
import cc.alcina.framework.gwt.client.dirndl.cmp.status.StatusModule;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel.PreText;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.overlay.Overlay;

/**
 * <p>
 * Note that this is not really a totally abstract sequence, it's a sequence
 * _plus_ UI generation support
 * 
 * <p>
 * The two could be teased apart via the registry, but I don't see any
 * application for that (yet)
 * <p>
 * Currently the sequence specifies the sequence elements, and three transforms:
 * element to row, element to detail (properties) and elemenets to detail
 * (additional). It also allows the specification of additional css/sass to
 * support the transform models
 */
public interface Sequence<T> {
	List<T> getElements();

	String getName();

	ModelTransform<T, ? extends Model> getRowTransform();

	ModelTransform<T, ? extends Model> getDetailTransform();

	default ModelTransform<T, ? extends Model> getDetailTransformAdditional() {
		return t -> null;
	}

	default String getCss() {
		return null;
	}

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

	@Registration.Self
	public interface CommandExecutor<T> {
		default String name() {
			return getClass().getSimpleName();
		}

		void execCommand(ModelEvent event, List<T> filteredElements);

		public static class ListCommands implements CommandExecutor {
			@Override
			public String name() {
				return "list";
			}

			@Override
			public void execCommand(ModelEvent event, List filteredElements) {
				Support.showAvailableCommands();
			}
		}

		static class Support {
			public static void showAvailableCommands() {
				String message = Registry.query(CommandExecutor.class)
						.implementations()
						.map(qe -> Ax.format("%s - %s", qe.name(),
								NestedName.get(qe)))
						.collect(Collectors.joining("\n"));
				Overlay.attributes().withContents(new PreText(message))
						.positionViewportCentered()
						.withRemoveOnMouseDownOutside(true).create().open();
			}

			static void execCommand(ModelEvent event,
					List filteredSequenceElements, String commandString) {
				Optional<CommandExecutor> exec = Registry
						.query(CommandExecutor.class).implementations()
						.filter(i -> i.name().equals(commandString))
						.findFirst();
				if (exec.isPresent()) {
					exec.get().execCommand(event, filteredSequenceElements);
				} else {
					NotificationObservable
							.of("No command '%s' found", commandString)
							.publish();
				}
			}
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
			if (!new File(path).exists()) {
				// FIXME - romcom - this causes a mutation record apply issue -
				// see
				// com.google.gwt.dom.client.mutations.MutationNode.remove(MutationNode
				// remove, ApplyTo applyTo) -
				// https://github.com/nevella/alcina/issues/34
				StatusModule.get().showMessageTransitional(Ax.format(
						"Load sequence - path '%s' does not exist", path));
				sequence.elements = new ArrayList<>();
			} else {
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
			}
			return sequence;
		}
	}

	public static class SequenceGenerationComplete
			implements ProcessObservable {
		public Sequence sequence;

		public SequenceGenerationComplete(Sequence sequence) {
			this.sequence = sequence;
		}
	}

	default String getUid() {
		return getName();
	}
}
