package cc.alcina.framework.entity.gwt.reflection.jdk;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.gwt.reflection.impl.typemodel.JClassType;
import cc.alcina.framework.entity.gwt.reflection.impl.typemodel.TypeOracle;
import cc.alcina.framework.entity.gwt.reflection.jdk.ValidityScanner.ValidityMetadata;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.registry.ClassMetadata;
import cc.alcina.framework.entity.registry.ClassMetadataCache;

class BuildtimeTypeProvider implements TypeOracle.TypeProvider {
	JdkReflectionGenerator jdkReflectionGenerator;

	ClassMetadataCache<ValidityMetadata> dataCache;

	BuildtimeTypeProvider(JdkReflectionGenerator jdkReflectionGenerator) {
		this.jdkReflectionGenerator = jdkReflectionGenerator;
	}

	boolean checkFilter(String typeName) {
		if (jdkReflectionGenerator.attributes.exclude != null) {
			return !typeName.matches(jdkReflectionGenerator.attributes.exclude);
		} else {
			return true;
		}
	}

	@Override
	public JClassType[] getTypes(TypeOracle typeOracle) {
		Stream<String> scannerClassNames = dataCache.classData.values().stream()
				.filter(meta -> !meta.invalid && meta.hasCanonicalName)
				.map(ClassMetadata::className);
		List<String> jdkReflectionClassNames = new ArrayList<>();
		jdkReflectionClassNames.addAll(CommonUtils.COLLECTION_CLASS_NAMES);
		jdkReflectionClassNames.addAll(CommonUtils.CORE_CLASS_NAMES);
		jdkReflectionClassNames
				.addAll(CommonUtils.PRIMITIVE_WRAPPER_CLASS_NAMES);
		Set<String> exceptions = new LinkedHashSet<>();
		List<JClassType> types = Stream
				.concat(scannerClassNames, jdkReflectionClassNames.stream())
				.filter(this::checkFilter).map(t -> {
					try {
						return typeOracle.findType(t);
					} catch (Throwable e) {
						String msg = CommonUtils.toSimpleExceptionMessage(e);
						if (exceptions.add(msg)) {
							Ax.err(msg);
						}
						return null;
					}
				}).filter(Objects::nonNull).collect(Collectors.toList());
		return types.toArray(new JClassType[types.size()]);
	}

	void scan() throws Exception {
		EntityLayerObjects.get().setDataFolder(
				jdkReflectionGenerator.attributes.generationDataFolder());
		ValidityScanner validityScanner = new ValidityScanner();
		validityScanner.cacheFile = SEUtilities.getChildFile(
				jdkReflectionGenerator.attributes.generationDataFolder(),
				"validity-scanner.dat");
		dataCache = validityScanner
				.scan(jdkReflectionGenerator.attributes.classDirectoryPaths);
	}
}
