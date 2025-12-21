package cc.alcina.framework.servlet.component.sequence;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.domain.IdOrdered;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer.DeserializerOptions;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.util.FileUtils;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.Sequence;

/*
 * Children must have a no-args constructor that populates the fields
 */
public abstract class AbstractSequenceLoader implements Sequence.Loader {
	String path;

	String name;

	Function<String, String> serializationRefactoringHandler;

	Sequence.Abstract<?> sequence;

	public AbstractSequenceLoader(String path, String name,
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
			throw new IllegalArgumentException(Ax
					.format("Load sequence - path '%s' does not exist", path));
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

	public static void writeElements(File folder,
			List<? extends IdOrdered> elements) {
		folder.mkdirs();
		SEUtilities.deleteDirectory(folder, true);
		elements.forEach(e -> {
			File writeTo = FileUtils.child(folder,
					String.valueOf(e.getId()) + ".json");
			Io.write().asReflectiveSerialized(true).object(e).toFile(writeTo);
		});
		LoggerFactory.getLogger(
				cc.alcina.framework.gwt.client.dirndl.cmp.sequence.Sequence.Loader.class)
				.info("Logged {} sequence elements to {}", elements.size(),
						folder);
	}
}