package cc.alcina.framework.entity.gwt.reflection.jdk;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import cc.alcina.framework.common.client.reflection.ModuleReflector;
import cc.alcina.framework.entity.ClassUtil;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.gwt.reflection.ClientReflectionFilterPeer;
import cc.alcina.framework.entity.gwt.reflection.ClientReflectionGenerator;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityLinkerPeer;

public class JdkReflectionGenerator {
	Attributes attributes;

	private GeneratorContext generatorContext;

	BuildtimeTypeProvider typeProvider;

	JdkReflectionGenerator(Attributes attributes) {
		this.attributes = attributes;
	}

	public void generate() throws Exception {
		typeProvider = new BuildtimeTypeProvider(this);
		typeProvider.scan();
		TreeLogger logger = new PrintWriterTreeLogger();
		generatorContext = new GeneratorContextImpl(this);
		ClientReflectionGenerator generator = new ClientReflectionGenerator();
		generator.attributes.guaranteedSinglePermutationBuild = true;
		try {
			generator.generate(logger, generatorContext,
					attributes.generatingType.getName());
		} catch (Exception e) {
			SEUtilities.invokeDelayed(e::printStackTrace, 200);
			throw e;
		}
	}

	public static Attributes attributes() {
		return new Attributes();
	}

	public static class Attributes {
		public String outputRoot;

		public Predicate<Class> typeFilter = c -> true;

		public List<String> classDirectoryPaths;

		public boolean clean;

		/*
		 * All reflection metadata will go to this module
		 */
		public Class<?> generatingType = ModuleReflector.Initial.class;

		public Class<? extends ClientReflectionFilterPeer> filterPeerClass = ClientReflectionFilterPeer.Default.class;

		public Class<? extends ReachabilityLinkerPeer> linkerPeerClass = ReachabilityLinkerPeer.Default.class;

		Attributes() {
		}

		public JdkReflectionGenerator build() {
			return new JdkReflectionGenerator(this);
		}

		private File generationDataFolder;

		private File generationSrcFolder;

		private File dataFolder;

		File dataFolder() {
			if (dataFolder == null) {
				dataFolder = new File(outputRoot);
				if (clean) {
					SEUtilities.deleteDirectory(dataFolder, true);
				}
				dataFolder.mkdirs();
			}
			return dataFolder;
		}

		File generationDataFolder() {
			if (generationDataFolder == null) {
				generationDataFolder = SEUtilities.getChildFile(dataFolder(),
						"data");
				generationDataFolder.mkdirs();
			}
			return generationDataFolder;
		}

		File generationSrcFolder() {
			if (generationSrcFolder == null) {
				generationSrcFolder = SEUtilities.getChildFile(dataFolder(),
						"src");
				generationSrcFolder.mkdirs();
			}
			return generationSrcFolder;
		}

		public void
				loadClassDirectoryPaths(Class<?>... classpathDefiningClasses) {
			classDirectoryPaths = Arrays.stream(classpathDefiningClasses)
					.map(ClassUtil::getRootClasspathElement).distinct()
					.toList();
		}
	}
}
