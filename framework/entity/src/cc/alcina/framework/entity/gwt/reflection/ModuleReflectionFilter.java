package cc.alcina.framework.entity.gwt.reflection;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;

import cc.alcina.framework.common.client.logic.reflection.reachability.ReflectionModule;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.AppImplRegistrations;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.AppReflectableTypes;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.LegacyModuleAssignments;
import cc.alcina.framework.entity.gwt.reflection.ReachabilityData.ModuleTypes.TypeList;

public class ModuleReflectionFilter implements ClientReflectionFilter {
	private GeneratorContext context;

	private File typesFile;

	private ReachabilityData.ModuleTypes moduleTypes;

	private String moduleName;

	private ClientReflectionFilterPeer peer;

	private TreeLogger logger;

	private boolean reflectUnknownInInitialModule;

	@Override
	public boolean emitAnnotation(JClassType type,
			Class<? extends Annotation> annotationType) {
		return peer.emitAnnotation(type, annotationType, moduleName);
	}

	@Override
	public boolean emitProperty(JClassType type, String name) {
		return peer.emitProperty(type, name, moduleName);
	}

	@Override
	public boolean emitType(JClassType type) {
		Boolean emit = peer.emitType(type, moduleName);
		if (emit != null) {
			return emit;
		}
		if (moduleTypes.permit(type, moduleName)) {
			return true;
		}
		return isInitial() && reflectUnknownInInitialModule
				&& moduleTypes.permit(type, ReflectionModule.UNKNOWN);
	}

	public void init(TreeLogger logger, GeneratorContext context,
			String moduleName, boolean reflectUnknownInInitialModule)
			throws Exception {
		this.logger = logger;
		this.context = context;
		this.moduleName = moduleName;
		this.reflectUnknownInInitialModule = reflectUnknownInInitialModule;
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

	@Override
	public void onGenerationComplete(AppImplRegistrations registrations,
			AppReflectableTypes reflectableTypes,
			Stream<JClassType> compilationTypes, String emitMessage)
			throws UnableToCompleteException {
		ReachabilityData.serializeReachabilityFile(logger, moduleTypes,
				typesFile);
		if (isInitial()) {
			LegacyModuleAssignments legacyModuleAssignments = getLegacyModuleAssignments(
					compilationTypes);
			context.commitArtifact(logger, registrations.serialize());
			context.commitArtifact(logger, reflectableTypes.serialize());
			context.commitArtifact(logger, legacyModuleAssignments.serialize());
		}
		logger.log(Type.INFO, emitMessage);
	}

	@Override
	public void updateReachableTypes(List<JClassType> types) {
		TypeList unknownList = moduleTypes
				.ensureTypeList(ReflectionModule.UNKNOWN);
		unknownList.types.clear();
		types.stream().filter(moduleTypes::doesNotContain)
				.forEach(unknownList::add);
		if (unknownList.types.size() > 0) {
			if (Objects.equals(moduleName, ReflectionModule.INITIAL)) {
				logger.log(Type.INFO,
						Ax.format("%s classes with unknown reachability",
								unknownList.types.size()));
			}
		}
		moduleTypes.generateLookup();
	}

	private boolean isInitial() {
		return moduleName.equals(ReflectionModule.INITIAL);
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
