package cc.alcina.framework.entity.gwt.reflection;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.Objects;
import java.util.stream.Stream;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;

import cc.alcina.framework.common.client.logic.reflection.ReflectionModule;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.AppImplRegistrations;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.AppReflectableTypes;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.LegacyModuleAssignments;

public class ClientReflectionFilter {
	private GeneratorContext context;

	private File typesFile;

	private ReachabilityData.ModuleTypes moduleTypes;

	private String moduleName;

	private ClientReflectionFilterPeer peer;

	private TreeLogger logger;

	public boolean emitAnnotation(JClassType type,
			Class<? extends Annotation> annotationType) {
		return peer.emitAnnotation(type, annotationType, moduleName);
	}

	public boolean emitProperty(JClassType type, String name) {
		return peer.emitProperty(type, name, moduleName);
	}

	public void init(TreeLogger logger, GeneratorContext context,
			String moduleName) throws Exception {
		this.logger = logger;
		this.context = context;
		this.moduleName = moduleName;
		if (Objects.equals(moduleName, ReflectionModule.INITIAL)) {
			ReachabilityData.initConfiguration(context.getPropertyOracle());
		}
		this.peer = ReachabilityData.filterPeerClass.getConstructor()
				.newInstance();
		typesFile = ReachabilityData.getReachabilityFile("reachability.json");
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

	protected boolean emitType(JClassType type) {
		Boolean emit = peer.emitType(type, moduleName);
		if (emit != null) {
			return emit;
		}
		if (!context.isProdMode() && moduleTypes.moduleLists.size() == 0) {
			return true;
		} else {
			return moduleTypes.permit(type, moduleName);
		}
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
			AppReflectableTypes reflectableTypes,
			Stream<JClassType> compilationTypes)
			throws UnableToCompleteException {
		ReachabilityData.serialize(moduleTypes, typesFile);
		if (isInitial()) {
			LegacyModuleAssignments legacyModuleAssignments = getLegacyModuleAssignments(
					compilationTypes);
			context.commitArtifact(logger, registrations.serialize());
			context.commitArtifact(logger, reflectableTypes.serialize());
			context.commitArtifact(logger, legacyModuleAssignments.serialize());
		}
	}

	LegacyModuleAssignments
			getLegacyModuleAssignments(Stream<JClassType> compilationTypes) {
		LegacyModuleAssignments assignments = new LegacyModuleAssignments();
		peer.getLegacyModuleTypeAssignments(compilationTypes).entrySet()
				.forEach(e -> e.getValue()
						.forEach(t -> assignments.addType(t, e.getKey())));
		return assignments;
	}
}
