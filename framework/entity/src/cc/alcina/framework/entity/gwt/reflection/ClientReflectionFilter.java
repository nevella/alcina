package cc.alcina.framework.entity.gwt.reflection;

import java.io.File;
import java.util.Objects;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.typeinfo.JClassType;

import cc.alcina.framework.common.client.logic.reflection.ReflectionModule;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.AppImplRegistrations;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.AppReflectableTypes;

public class ClientReflectionFilter {
	private GeneratorContext context;

	private File typesFile;

	private ReachabilityData.ModuleTypes moduleTypes;

	private String moduleName;

	public void init(GeneratorContext context, String moduleName)
			throws Exception {
		this.context = context;
		this.moduleName = moduleName;
		if (Objects.equals(moduleName, ReflectionModule.INITIAL)) {
			ReachabilityData.initConfiguration(context.getPropertyOracle());
		}
		typesFile = ReachabilityData.getCacheFile("reachability.json");
		if (typesFile.exists()) {
			try {
				moduleTypes = ReachabilityData.deserialize(
						ReachabilityData.ModuleTypes.class, typesFile);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (moduleTypes == null) {
			moduleTypes = new ReachabilityData.ModuleTypes();
		}
		moduleTypes.ensureTypeList(moduleName);
		moduleTypes.generateLookup();
	}

	private boolean isInitial() {
		return moduleName.equals(ReflectionModule.INITIAL);
	}

	protected boolean isReflectableJavaCollectionClass(JClassType jClassType) {
		return CommonUtils.COLLECTION_CLASS_NAMES
				.contains(jClassType.getQualifiedSourceName());
	}

	protected boolean isReflectableJavaCoreClass(JClassType jClassType) {
		return CommonUtils.CORE_CLASS_NAMES
				.contains(jClassType.getQualifiedSourceName())
				|| CommonUtils.PRIMITIVE_CLASS_NAMES
						.contains(jClassType.getQualifiedSourceName())
				|| CommonUtils.PRIMITIVE_WRAPPER_CLASS_NAMES
						.contains(jClassType.getQualifiedSourceName());
	}

	protected void onGenerationComplete(AppImplRegistrations registrations,
			AppReflectableTypes reflectableTypes) {
		ReachabilityData.serialize(moduleTypes, typesFile);
		if (isInitial()) {
			File registryFile = ReachabilityData.getRegistryFile();
			ReachabilityData.serialize(registrations, registryFile);
			File reflectableTypesFile = ReachabilityData
					.getReflectableTypesFile();
			ReachabilityData.serialize(reflectableTypes, reflectableTypesFile);
		}
	}

	protected boolean permit(JClassType type) {
		if (!context.isProdMode() && moduleTypes.moduleLists.size() == 0) {
			return true;
		} else {
			return moduleTypes.permit(type, moduleName);
		}
	}
}
