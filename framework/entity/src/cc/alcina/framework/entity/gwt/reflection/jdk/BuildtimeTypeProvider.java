package cc.alcina.framework.entity.gwt.reflection.jdk;

import java.util.List;

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

	@Override
	public JClassType[] getTypes(TypeOracle typeOracle) {
		List<JClassType> types = dataCache.classData.values().stream()
				.filter(meta -> !meta.invalid && meta.hasCanonicalName)
				.map(ClassMetadata::className).map(typeOracle::findType)
				.toList();
		return types.toArray(new JClassType[types.size()]);
	}
}
