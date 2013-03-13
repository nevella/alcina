package com.totsp.gwittir.rebind.beans;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import cc.alcina.framework.common.client.logic.reflection.NotIntrospected;
import cc.alcina.framework.common.client.logic.reflection.ReflectionModule;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.ResourceUtilities;

import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.typeinfo.JAnnotationType;
import com.google.gwt.core.ext.typeinfo.JClassType;

public interface IntrospectorFilter {
	public static final String ALCINA_INTROSPECTOR_FILTER_CLASSNAME = "alcina.introspectorFilter.classname";

	public static final String ALCINA_INTROSPECTOR_FILTER_DATA_FILE = "alcina.introspectorFilter.dataFile";

	public static class IntrospectorFilterPassthrough implements
			IntrospectorFilter {
		@Override
		public void filterIntrospectorResults(List<BeanResolver> results) {
			// do nothing
		}

		@Override
		public void filterAnnotations(List<JAnnotationType> jAnns,
				List<Class<? extends Annotation>> visibleAnnotationClasses) {
			// do nothing
		}

		@Override
		public void filterReflectionInfo(List<JClassType> beanInfoTypes,
				List<JClassType> instantiableTypes,
				Map<JClassType, Set<RegistryLocation>> gwtRegisteringClasses) {
			// do nothing
		}

		@Override
		public boolean emitBeanResolver(BeanResolver resolver) {
			return true;
		}

		@Override
		public void filterProperties(BeanResolver resolver) {
		}

		@Override
		public void setModuleName(String value) {
		}

		@Override
		public boolean omitForModule(JClassType jClassType) {
			return false;
		}

		@Override
		public void setContext(GeneratorContext context) {
		}

		@Override
		public void generationComplete() {
			
		}
	}

	void filterIntrospectorResults(List<BeanResolver> results);

	void filterAnnotations(List<JAnnotationType> jAnns,
			List<Class<? extends Annotation>> visibleAnnotationClasses);

	void filterReflectionInfo(List<JClassType> beanInfoTypes,
			List<JClassType> instantiableTypes,
			Map<JClassType, Set<RegistryLocation>> gwtRegisteringClasses);

	public static class IntrospectorFilterHelper {
		public static IntrospectorFilter getFilter(GeneratorContext context) {
			try {
				ConfigurationProperty cp = context.getPropertyOracle()
						.getConfigurationProperty(
								ALCINA_INTROSPECTOR_FILTER_CLASSNAME);
				for (String filterClassName : cp.getValues()) {
					if (CommonUtils.isNotNullOrEmpty(filterClassName)) {
						try {
							IntrospectorFilter filter = (IntrospectorFilter) Class
									.forName(filterClassName).newInstance();
							filter.setContext(context);
							return filter;
						} catch (Exception e) {
							e.printStackTrace();
							throw new RuntimeException(e);
						}
					}
				}
				return new IntrospectorFilterPassthrough();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	boolean emitBeanResolver(BeanResolver resolver);

	void setContext(GeneratorContext context);

	public static class ModuleIntrospectionHelper {
		private IntrospectorFilterBase filter;

		private String dataFilePath;

		public ModuleIntrospectionHelper(IntrospectorFilterBase filter,
				String dataFilePath) {
			this.filter = filter;
			this.dataFilePath = dataFilePath;
			initInfo();
		}

		private ModuleIntrospectionInfo info = new ModuleIntrospectionInfo();

		private void initInfo() {
			File infoFile = getInfoFile();
			info.mode = ModuleIntrospectionMode.INITIAL_ONLY;
			if (infoFile.exists()) {
				try {
					String content = ResourceUtilities
							.readFileToString(infoFile);
					if (!content.isEmpty()) {
						Map<String, String> emptyProps = new HashMap<String, String>();
						JAXBContext jc = JAXBContext.newInstance(
								new Class[] { ModuleIntrospectionInfo.class },
								emptyProps);
						Unmarshaller um = jc.createUnmarshaller();
						StringReader sr = new StringReader(content);
						info = (ModuleIntrospectionInfo) um.unmarshal(sr);
					}
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}
			if (info.mode == ModuleIntrospectionMode.DISALLOW_ALL) {
				filterForHumans();
			}
		}

		protected void filterForHumans() {
			CollectionFilter<ModuleIntrospectionClassInfo> autoFilter = new CollectionFilter<IntrospectorFilter.ModuleIntrospectionClassInfo>() {
				@Override
				public boolean allow(ModuleIntrospectionClassInfo o) {
					return o.provenance == ModuleIntrospectionClassInfoProvenance.HUMAN;
				}
			};
			CollectionFilters.filterInPlace(info.classInfo, autoFilter);
		}

		public void saveInfoFile() throws Exception {
			Map<String, String> emptyProps = new HashMap<String, String>();
			JAXBContext jc = JAXBContext.newInstance(
					new Class[] { ModuleIntrospectionInfo.class }, emptyProps);
			Marshaller m = jc.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			StringWriter s = new StringWriter();
			m.marshal(info, s);
			ResourceUtilities.writeStringToFile(s.toString(), getInfoFile());
		}

		private File getInfoFile() {
			try {
				String path = dataFilePath != null ? dataFilePath : filter
						.getContext()
						.getPropertyOracle()
						.getConfigurationProperty(
								ALCINA_INTROSPECTOR_FILTER_DATA_FILE)
						.getValues().get(0);
				File file = new File(path);
				file.getParentFile().mkdirs();
				return file;
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
		public ModuleIntrospectionClassInfo getInfo(String className){
			return info.getInfo(className, false);
		}
		public boolean omit(JClassType type) {
			switch (info.mode) {
			case INITIAL_ONLY:
				return !filter.getModuleName().equals(
						ReflectionModule.INITIAL);
			case DISALLOW_ALL:
				return true;
			case PER_CLASS_STRICT:
			case PER_CLASS_FORGIVING:
				ModuleIntrospectionClassInfo classInfo = info.getInfo(type
						.getQualifiedSourceName(),true);
				if (classInfo.modules.isEmpty()
						&& info.mode == ModuleIntrospectionMode.PER_CLASS_FORGIVING) {
					return !filter.getModuleName().equals(
							ReflectionModule.INITIAL);
				} else {
					return !classInfo.modules.contains(filter.getModuleName());
				}
			}
			return true;
		}

		public ModuleIntrospectionInfo getInfo() {
			return this.info;
		}

		public void reset() throws Exception {
			info.mode = ModuleIntrospectionMode.DISALLOW_ALL;
			filterForHumans();
			saveInfoFile();
		}

		public void generationComplete() {
			try {
				saveInfoFile();
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}

	public enum ModuleIntrospectionMode {
		INITIAL_ONLY, DISALLOW_ALL, PER_CLASS_STRICT, PER_CLASS_FORGIVING
	}

	public enum ModuleIntrospectionClassInfoProvenance {
		AUTO, HUMAN
	}

	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class ModuleIntrospectionInfo {
		public String filterName;

		public ModuleIntrospectionMode mode = null;

		public List<ModuleIntrospectionClassInfo> classInfo = new ArrayList<IntrospectorFilter.ModuleIntrospectionClassInfo>();

		ModuleIntrospectionClassInfo getInfo(String sourceName, boolean ensure) {
			if (infoLookup == null) {
				infoLookup = new LinkedHashMap<String, IntrospectorFilter.ModuleIntrospectionClassInfo>();
				for (ModuleIntrospectionClassInfo info : classInfo) {
					infoLookup.put(info.classSourceName, info);
				}
			}
			if (!infoLookup.containsKey(sourceName)&&ensure) {
				ModuleIntrospectionClassInfo info = new ModuleIntrospectionClassInfo();
				info.provenance = ModuleIntrospectionClassInfoProvenance.AUTO;
				info.classSourceName = sourceName;
				classInfo.add(info);
				infoLookup.put(sourceName, info);
			}
			return infoLookup.get(sourceName);
		}

		@XmlTransient
		Map<String, ModuleIntrospectionClassInfo> infoLookup;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class ModuleIntrospectionClassInfo {
		public String classSourceName;

		public ModuleIntrospectionClassInfoProvenance provenance;

		public Set<String> modules = new LinkedHashSet<String>();
	}

	public static abstract class IntrospectorFilterBase implements
			IntrospectorFilter {
		private GeneratorContext context;

		private String moduleName;

		private Map<JClassType, Boolean> implBoundWidgetMap = new LinkedHashMap<JClassType, Boolean>();

		protected boolean isImplBoundWidget(BeanResolver resolver) {
			return isImplBoundWidgetJct(resolver.getType());
		}

		protected boolean isImplBoundWidgetJct(JClassType t) {
			if (!implBoundWidgetMap.containsKey(t)) {
				JClassType[] interfaces = t.getImplementedInterfaces();
				boolean implBoundWidget = false;
				for (JClassType jClassType : interfaces) {
					if (jClassType.getQualifiedSourceName().contains(
							"BoundWidget")) {
						implBoundWidget = true;
					}
				}
				while (t != null) {
					if (t.getQualifiedSourceName().contains("BoundWidget")) {
						implBoundWidget = true;
					}
					t = t.getSuperclass();
				}
				implBoundWidgetMap.put(t, implBoundWidget);
			}
			return implBoundWidgetMap.get(t);
		}

		protected CollectionFilter<Entry<String, RProperty>> valueOnlyFilter = new CollectionFilter<Map.Entry<String, RProperty>>() {
			@Override
			public boolean allow(Entry<String, RProperty> o) {
				return o.getKey().equals("value");
			}
		};

		protected void filterPropertiesCollection(BeanResolver resolver,
				CollectionFilter<Entry<String, RProperty>> filter) {
			Map<String, RProperty> properties = resolver.getProperties();
			CollectionFilters.filterInPlace(properties.entrySet(), filter);
		}

		@Override
		public void filterIntrospectorResults(List<BeanResolver> results) {
			CollectionFilters.filterInPlace(results,
					new CollectionFilter<BeanResolver>() {
						@Override
						public boolean allow(BeanResolver o) {
							JClassType t = o.getType();
							return !t
									.isAnnotationPresent(NotIntrospected.class)
									&& emitBeanResolver(o);
						}
					});
		}

		CollectionFilter<Entry<String, RProperty>> alwaysIgnoreFilter = new CollectionFilter<Map.Entry<String, RProperty>>() {
			@Override
			public boolean allow(Entry<String, RProperty> o) {
				return o.getValue().getReadMethod() != null
						&& o.getValue().getWriteMethod() != null;
			}
		};

		@Override
		public void filterProperties(BeanResolver resolver) {
			CollectionFilters.filterInPlace(
					resolver.getProperties().entrySet(), alwaysIgnoreFilter);
		}

		public String getModuleName() {
			return this.moduleName;
		}

		public void setModuleName(String moduleName) {
			this.moduleName = moduleName;
		}

		public GeneratorContext getContext() {
			return this.context;
		}

		public void setContext(GeneratorContext context) {
			this.context = context;
		}

		@Override
		public void generationComplete() {
		}
	}

	void filterProperties(BeanResolver resolver);

	void setModuleName(String value);

	boolean omitForModule(JClassType jClassType);

	void generationComplete();
}
