package com.totsp.gwittir.rebind.beans;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.logic.reflection.ReflectionAction;
import cc.alcina.framework.common.client.logic.reflection.ReflectionModule;
import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.entity.ResourceUtilities;

import com.google.gwt.core.ext.typeinfo.JClassType;

public class ModuleIntrospectionHelper {
	private IntrospectorFilterBase filter;

	private String dataFilePath;

	public ModuleIntrospectionHelper(IntrospectorFilterBase filter,
			String dataFilePath) {
		this.filter = filter;
		this.dataFilePath = dataFilePath;
		initInfo();
	}

	private ModuleIntrospectionHelper.ModuleIntrospectionInfo info = new ModuleIntrospectionHelper.ModuleIntrospectionInfo();

	private void initInfo() {
		File infoFile = getInfoFile();
		info.mode = ModuleIntrospectionHelper.ModuleIntrospectionMode.INITIAL_ONLY;
		if (infoFile.exists()) {
			try {
				String content = ResourceUtilities.readFileToString(infoFile);
				if (!content.isEmpty()) {
					Map<String, String> emptyProps = new HashMap<String, String>();
					JAXBContext jc = JAXBContext
							.newInstance(
									new Class[] { ModuleIntrospectionHelper.ModuleIntrospectionInfo.class },
									emptyProps);
					Unmarshaller um = jc.createUnmarshaller();
					StringReader sr = new StringReader(content);
					info = (ModuleIntrospectionHelper.ModuleIntrospectionInfo) um
							.unmarshal(sr);
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
		if (info.mode == ModuleIntrospectionHelper.ModuleIntrospectionMode.DISALLOW_ALL) {
			filterForHumans();
		}
	}

	protected void filterForHumans() {
		CollectionFilter<ModuleIntrospectionHelper.ModuleIntrospectionClassInfo> autoFilter = new CollectionFilter<ModuleIntrospectionHelper.ModuleIntrospectionClassInfo>() {
			@Override
			public boolean allow(
					ModuleIntrospectionHelper.ModuleIntrospectionClassInfo o) {
				return o.provenance == ModuleIntrospectionHelper.ModuleIntrospectionClassInfoProvenance.HUMAN;
			}
		};
		CollectionFilters.filterInPlace(info.classInfo, autoFilter);
	}

	public void saveInfoFile() throws Exception {
		Map<String, String> emptyProps = new HashMap<String, String>();
		JAXBContext jc = JAXBContext
				.newInstance(
						new Class[] { ModuleIntrospectionHelper.ModuleIntrospectionInfo.class },
						emptyProps);
		Marshaller m = jc.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		StringWriter s = new StringWriter();
		m.marshal(info, s);
		ResourceUtilities.writeStringToFile(s.toString(), getInfoFile());
	}

	private File getInfoFile() {
		try {
			String path = dataFilePath != null ? dataFilePath
					: filter.getContext()
							.getPropertyOracle()
							.getConfigurationProperty(
									IntrospectorFilter.ALCINA_INTROSPECTOR_FILTER_DATA_FILE)
							.getValues().get(0);
			File file = new File(path);
			file.getParentFile().mkdirs();
			return file;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public ModuleIntrospectionHelper.ModuleIntrospectionClassInfo getClassInfo(
			String className) {
		return info.getInfo(className, false);
	}

	public boolean omit(JClassType type, ReflectionAction reflectionAction) {
		switch (info.mode) {
		case INITIAL_ONLY:
			return !filter.getModuleName().equals(ReflectionModule.INITIAL);
		case DISALLOW_ALL:
			return true;
		case PER_CLASS_STRICT:
		case PER_CLASS_FORGIVING:
		case PER_CLASS_FORGIVING_LEFTOVER:
			ModuleIntrospectionHelper.ModuleIntrospectionClassInfo classInfo = info
					.getInfo(type.getQualifiedSourceName(), true);
			if (classInfo.provenance == ModuleIntrospectionClassInfoProvenance.OMIT) {
				return true;
			}
			Set<String> modules = classInfo.modules;
			if (modules.isEmpty()) {
				switch (info.mode) {
				case PER_CLASS_FORGIVING:
					return !filter.getModuleName().equals(
							ReflectionModule.INITIAL);
				case PER_CLASS_FORGIVING_LEFTOVER:
					return !filter.getModuleName().equals(
							ReflectionModule.LEFTOVER);
				}
			} else {
				if (modules.contains(ReflectionModule.INITIAL)) {
					if (reflectionAction == ReflectionAction.BEAN_INFO_DESCRIPTOR) {
						return !filter.getModuleName().equals(
								ReflectionModule.LEFTOVER);
					} else {
						return !filter.getModuleName().equals(
								ReflectionModule.INITIAL);
					}
				}
				if (modules.size() > 1) {
					return !filter.getModuleName().equals(
							ReflectionModule.LEFTOVER);
				}
				return !modules.contains(filter.getModuleName());
			}
		}
		return true;
	}

	public ModuleIntrospectionHelper.ModuleIntrospectionInfo getInfo() {
		return this.info;
	}

	public void reset(boolean soft) throws Exception {
		if (soft) {
			Callback<ModuleIntrospectionClassInfo> removeModulesCallback = new Callback<ModuleIntrospectionHelper.ModuleIntrospectionClassInfo>() {
				@Override
				public void apply(ModuleIntrospectionClassInfo value) {
					if (value.provenance == ModuleIntrospectionClassInfoProvenance.AUTO) {
						value.modules.clear();
					}
				}
			};
			CollectionFilters.apply(info.classInfo, removeModulesCallback);
		} else {
			info.mode = ModuleIntrospectionHelper.ModuleIntrospectionMode.DISALLOW_ALL;
			filterForHumans();
		}
		saveInfoFile();
	}

	public void generationComplete() {
		try {
			saveInfoFile();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class ModuleIntrospectionInfo {
		public String filterName;

		public ModuleIntrospectionHelper.ModuleIntrospectionMode mode = null;

		public List<ModuleIntrospectionHelper.ModuleIntrospectionClassInfo> classInfo = new ArrayList<ModuleIntrospectionHelper.ModuleIntrospectionClassInfo>();

		ModuleIntrospectionHelper.ModuleIntrospectionClassInfo getInfo(
				String sourceName, boolean ensure) {
			if (infoLookup == null) {
				infoLookup = new LinkedHashMap<String, ModuleIntrospectionHelper.ModuleIntrospectionClassInfo>();
				for (ModuleIntrospectionHelper.ModuleIntrospectionClassInfo info : classInfo) {
					infoLookup.put(info.classSourceName, info);
				}
			}
			if (!infoLookup.containsKey(sourceName) && ensure) {
				ModuleIntrospectionHelper.ModuleIntrospectionClassInfo info = new ModuleIntrospectionHelper.ModuleIntrospectionClassInfo();
				info.provenance = ModuleIntrospectionHelper.ModuleIntrospectionClassInfoProvenance.AUTO;
				info.classSourceName = sourceName;
				classInfo.add(info);
				infoLookup.put(sourceName, info);
			}
			return infoLookup.get(sourceName);
		}

		@XmlTransient
		Map<String, ModuleIntrospectionHelper.ModuleIntrospectionClassInfo> infoLookup;
	}

	public static enum ModuleIntrospectionMode {
		INITIAL_ONLY, DISALLOW_ALL, PER_CLASS_STRICT, PER_CLASS_FORGIVING,
		PER_CLASS_FORGIVING_LEFTOVER
	}

	public static enum ModuleIntrospectionClassInfoProvenance {
		AUTO, HUMAN, OMIT
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class ModuleIntrospectionClassInfo {
		public String classSourceName;

		public ModuleIntrospectionClassInfoProvenance provenance;

		public String note;

		public Set<String> modules = new LinkedHashSet<String>();

		public Class forName() {
			String bName = classSourceName;
			Matcher m = IntrospectorFilter.sourceToBinary.matcher(bName);
			if (m.matches()) {
				bName = m.replaceAll("$1\\$$3");
			}
			try {
				return Class.forName(bName);
			} catch (Throwable e) {
				return null;
			}
		}
	}
}