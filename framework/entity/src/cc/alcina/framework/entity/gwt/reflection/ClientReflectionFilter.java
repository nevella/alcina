package cc.alcina.framework.entity.gwt.reflection;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.typeinfo.JClassType;

import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.util.JacksonUtils;

public class ClientReflectionFilter {
	private GeneratorContext context;

	private String moduleName;

	private File cacheFile;

	private ReachabilityData reachabilityData;

	public void init(GeneratorContext context, String moduleName)
			throws Exception {
		this.context = context;
		this.moduleName = moduleName;
		cacheFile = getCacheFile();
		if (cacheFile.exists()) {
			reachabilityData = JacksonUtils.deserializeFromFile(cacheFile,
					ReachabilityData.class);
		} else {
			reachabilityData = new ReachabilityData();
			reachabilityData.moduleName = moduleName;
		}
		reachabilityData.generateLookup();
	}

	protected File getCacheFile() throws Exception {
		Optional<String> configurationFolderPath = Ax.optionalFirst(context
				.getPropertyOracle()
				.getConfigurationProperty(
						ClientReflectionFilter.class.getName() + ".dataFolder")
				.getValues());
		JClassType[] types = context.getTypeOracle().getTypes();
		// first type will be compilation module name
		String defaultFolderPath = Ax.format(
				"/var/local/build/gwt/client-reflection-filter/%s", types[0]);
		String folderPath = configurationFolderPath.orElse(defaultFolderPath);
		return new File(Ax.format("%s/reachability.json", folderPath));
	}

	protected void onGenerationComplete() {
		JacksonUtils.serializeToFile(reachabilityData, cacheFile);
	}

	protected boolean permit(JClassType type) {
		if (!context.isProdMode() || "yep".length() > 0) {
			return true;
		} else {
			return reachabilityData.sourceNameType
					.containsKey(type.getQualifiedSourceName());
		}
	}

	public static class ReachabilityData {
		public String moduleName;

		public List<Type> reachable = new ArrayList<>();

		public transient Map<String, Type> sourceNameType;

		public void generateLookup() {
			sourceNameType = reachable.stream().collect(
					AlcinaCollectors.toKeyMap(t -> t.qualifiedSourceName));
		}
	}

	public static class Type {
		public String qualifiedSourceName;

		public boolean allSubclassesReachable;

		public String moduleName;
	}
}
