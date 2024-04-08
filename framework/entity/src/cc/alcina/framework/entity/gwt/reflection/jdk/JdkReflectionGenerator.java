package cc.alcina.framework.entity.gwt.reflection.jdk;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.reflection.ModuleReflector;
import cc.alcina.framework.entity.ClassUtil;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.gwt.reflection.ClientReflectionFilterPeer;
import cc.alcina.framework.entity.gwt.reflection.ClientReflectionGenerator;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityLinkerPeer;

public class JdkReflectionGenerator {
	public static Attributes attributes() {
		return new Attributes();
	}

	Attributes attributes;

	GeneratorContextImpl generatorContext;

	BuildtimeTypeProvider typeProvider;

	Logger logger = LoggerFactory.getLogger(getClass());

	JdkReflectionGenerator(Attributes attributes) {
		this.attributes = attributes;
	}

	public void generate() throws Exception {
		typeProvider = new BuildtimeTypeProvider(this);
		typeProvider.scan();
		TreeLogger treeLogger = new PrintWriterTreeLogger();
		generatorContext = new GeneratorContextImpl(this);
		ClientReflectionGenerator generator = new ClientReflectionGenerator();
		generator.attributes.guaranteedSinglePermutationBuild = true;
		generator.attributes.simpleExcludes = attributes.exclude;
		generator.attributes.useJdkForName = true;
		generator.providesTypeBounds = generatorContext.typeOracle;
		try {
			generator.generate(treeLogger, generatorContext,
					attributes.generatingType.getName());
		} catch (Exception e) {
			SEUtilities.invokeDelayed(e::printStackTrace, 200);
			throw e;
		}
		List<File> inSourceFolder = SEUtilities.listFilesRecursive(
				attributes.generationSrcFolder.getPath(), null, true);
		List<File> toDelete = inSourceFolder.stream().filter(f -> {
			try {
				return !generatorContext.printWriterPaths
						.contains(f.getCanonicalPath());
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}).collect(Collectors.toList());
		logger.info("Deleting {} removed source files", toDelete.size());
		toDelete.forEach(File::delete);
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

		private File generationDataFolder;

		private File generationSrcFolder;

		private File dataFolder;

		/*
		 * Another (simple) filter atop filterPeerClass
		 */
		public String exclude;

		Attributes() {
		}

		public JdkReflectionGenerator build() {
			return new JdkReflectionGenerator(this);
		}

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
					.collect(Collectors.toList());
		}
	}
}
