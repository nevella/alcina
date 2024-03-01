package cc.alcina.framework.entity.gwt.reflection.jdk;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
		List<JClassType> types = Stream
				.concat(scannerClassNames, jdkReflectionClassNames.stream())
				.filter(this::checkFilter).map(typeOracle::findType)
				.collect(Collectors.toList());
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
