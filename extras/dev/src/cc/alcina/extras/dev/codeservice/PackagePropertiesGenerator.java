package cc.alcina.extras.dev.codeservice;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import cc.alcina.extras.dev.codeservice.CodeService.Event;
import cc.alcina.extras.dev.codeservice.CodeService.PackageEvent;
import cc.alcina.extras.dev.codeservice.PackagePropertiesGenerator.PackagePropertiesUnitData.DeclarationProperties;
import cc.alcina.extras.dev.codeservice.SourceFolder.SourcePackage;
import cc.alcina.extras.dev.console.code.CompilationUnits;
import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitWrapper;
import cc.alcina.extras.dev.console.code.CompilationUnits.PersistentUnitData;
import cc.alcina.extras.dev.console.code.UnitType;
import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.reflection.impl.ClassReflectorProvider;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.entity.ClassUtilEntity;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.util.FileUtils;

/**
 * <p>
 * This class generates a PackageProperties.java source file if the package
 * contains any types annotated with {@link TypedProperties}.
 * 
 * <p>
 * The PackageProperties class contains a TypedProperty.Container class for each
 * annotated type, which in turn contains static TypedProperty fields
 * corresponding to each property of the annotated type.
 * 
 */
/*
 * The implementation uses the GWT sourcewriter to write the code - it could
 * also have used JavaParser - sourcewriter is a touch simpler
 */
public class PackagePropertiesGenerator extends CodeService.Handler.Abstract {
	@Override
	public void handle(Event event) {
		if (event instanceof PackageEvent) {
			handlePackageEvent((PackageEvent) event);
		}
	}

	void handlePackageEvent(PackageEvent event) {
		List<PackagePropertiesUnitData> packageTypeMetadataList = event
				.packageUnits().units
						.stream()
						.map(unitWrapper -> event.context.units.compilationUnits
								.ensure(PackagePropertiesUnitData.class,
										unitWrapper.getFile()))
						.collect(Collectors.toList());
		try {
			new PackagePropertiesWriter(event.sourcePackage,
					packageTypeMetadataList).write();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static class PackagePropertiesWriter {
		SourcePackage sourcePackage;

		List<PackagePropertiesUnitData> packageTypeMetadataList;

		PackagePropertiesWriter(SourcePackage sourcePackage,
				List<PackagePropertiesUnitData> packageTypeMetadataList) {
			this.sourcePackage = sourcePackage;
			this.packageTypeMetadataList = packageTypeMetadataList;
		}

		File file() {
			return FileUtils.child(sourcePackage.file,
					"PackageProperties.java");
		}

		SourceWriter sourceWriter;

		PrintWriter printWriter;

		ClassSourceFileComposerFactory composerFactory;

		Set<String> imports = new TreeSet<>();

		void addImport(Class clazz) {
			clazz = CommonUtils.getWrapperType(clazz);
			Class topLevel = ClassUtilEntity.getTopLevelType(clazz);
			String name = topLevel.getName();
			imports.add(name);
		}

		Logger logger = LoggerFactory.getLogger(getClass());

		String resolvedTypeName(Class clazz) {
			clazz = CommonUtils.getWrapperType(clazz);
			return NestedName.get(clazz);
		}

		List<Class> possibleInvalidClassFiles = new ArrayList<>();;

		void write() throws Exception {
			if (packageTypeMetadataList.stream()
					.anyMatch(PackagePropertiesUnitData::hasUnitException)) {
				logger.info("Package exceptions: {}", sourcePackage);
				packageTypeMetadataList.stream()
						.filter(PackagePropertiesUnitData::hasUnitException)
						.forEach(data -> data.declarationPropertiesList.stream()
								.filter(list -> list.exception != null)
								.forEach(list -> {
									logger.info("\t{}\n\t{}",
											new File(data.path).getName(),
											list.exception.replaceFirst(
													"(?s)([^\n]+).+", "$1"));
								}));
			}
			if (packageTypeMetadataList.stream()
					.noneMatch(PackagePropertiesUnitData::hasTypedProperties)) {
				// file().delete();
				return;
			}
			composerFactory = new ClassSourceFileComposerFactory(
					sourcePackage.packageName, "PackageProperties");
			List<TypeWriter> typeWriters = packageTypeMetadataList.stream()
					.flatMap(list -> list.declarationPropertiesList.stream())
					.filter(DeclarationProperties::hasTypedProperties).sorted()
					.map(TypeWriter::new).collect(Collectors.toList());
			imports.add(TypedProperty.class.getName());
			imports.add(InstanceProperty.class.getName());
			typeWriters.forEach(TypeWriter::addImports);
			imports.forEach(composerFactory::addImport);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			printWriter = new PrintWriter(baos);
			sourceWriter = composerFactory.createSourceWriter(printWriter);
			sourceWriter.indent();
			sourceWriter.println("// auto-generated, do not modify");
			sourceWriter.println("//@formatter:off");
			sourceWriter.println();
			typeWriters.forEach(TypeWriter::writeField);
			sourceWriter.println();
			typeWriters.forEach(TypeWriter::write);
			sourceWriter.outdent();
			closeClassBody();
			if (possibleInvalidClassFiles.size() > 0) {
				LoggerFactory.getLogger(getClass())
						.warn("Not writing - possible invalid files :: \n{}",
								possibleInvalidClassFiles.stream()
										.map(NestedName::get)
										.collect(Collectors.joining("\n")));
			} else {
				Io.write().bytes(baos.toByteArray()).withNoUpdateIdentical(true)
						.toFile(file());
				logger.info("Wrote packageproperties: {}", sourcePackage);
			}
		}

		class TypeWriter implements Comparable<TypeWriter> {
			DeclarationProperties declarationProperties;

			Class<?> clazz;

			String modifier;

			TypeWriter(DeclarationProperties declarationProperties) {
				this.declarationProperties = declarationProperties;
				clazz = CodeService
						.forName(declarationProperties.qualifiedBinaryName);
				modifier = Modifier.isPublic(clazz.getModifiers()) ? "public "
						: "";
			}

			void write() {
				String containerTypeName = getContainerTypeName();
				String containingTypeName = resolvedTypeName(clazz);
				sourceWriter.println(
						"%sstatic class %s implements TypedProperty.Container {",
						modifier, containerTypeName);
				sourceWriter.indent();
				List<Property> properties = Reflections.at(clazz).properties()
						.stream()
						.sorted(Comparator.comparing(Property::getName))
						.collect(Collectors.toList());
				if (properties.isEmpty()) {
					possibleInvalidClassFiles.add(clazz);
				}
				properties.forEach(this::writeProperty);
				/*
				 * write instanceProperties container
				 */
				sourceWriter.println(
						"%sstatic class InstanceProperties extends InstanceProperty.Container<%s> {",
						modifier, containingTypeName);
				sourceWriter.indent();
				sourceWriter.println(
						"%s InstanceProperties(%s source){super(source);}",
						modifier, containingTypeName);
				properties.forEach(this::writeInstancePropertyGenerator);
				sourceWriter.outdent();
				sourceWriter.println("}");
				sourceWriter.println();
				sourceWriter.println(
						"%s InstanceProperties instance(%s instance) {",
						modifier, containingTypeName);
				sourceWriter.indent();
				sourceWriter
						.println("return new InstanceProperties( instance);");
				sourceWriter.outdent();
				sourceWriter.println("}");
				sourceWriter.println();
				/*
				 * write instanceProperties container constructor
				 */
				sourceWriter.outdent();
				sourceWriter.println("}");
				sourceWriter.println();
			}

			String getContainerTypeName() {
				return "_" + NestedName.get(clazz).replace(".", "_");
			}

			void writeField() {
				String containerTypeName = getContainerTypeName();
				String fieldName = getFieldName(containerTypeName);
				sourceWriter.println("%sstatic %s %s = new %s();", modifier,
						containerTypeName, fieldName, containerTypeName);
			}

			String getFieldName(String containerTypeName) {
				String fieldName = Arrays
						.stream(containerTypeName.substring(1).split("_"))
						.map(CommonUtils::lcFirst)
						.collect(Collectors.joining("_"));
				return fieldName;
			}

			void writeProperty(Property property) {
				String containingTypeName = resolvedTypeName(
						property.getOwningType());
				String propertyTypeName = resolvedTypeName(property.getType());
				sourceWriter.println(
						"%sTypedProperty<%s, %s> %s = new TypedProperty<>(%s.class, \"%s\");",
						modifier, containingTypeName, propertyTypeName,
						property.getName(), containingTypeName,
						property.getName());
			}

			void writeInstancePropertyGenerator(Property property) {
				String containingTypeName = resolvedTypeName(
						property.getOwningType());
				String propertyTypeName = resolvedTypeName(property.getType());
				String containerTypeName = getContainerTypeName();
				sourceWriter.println(
						"%sInstanceProperty<%s, %s> %s(){return new InstanceProperty<>(source,PackageProperties.%s.%s);}",
						modifier, containingTypeName, propertyTypeName,
						property.getName(), getFieldName(containerTypeName),
						property.getName());
			}

			void addImports() {
				Reflections.at(clazz).properties()
						.forEach(p -> addImport(p.getType()));
			}

			@Override
			public int compareTo(TypeWriter o) {
				return NestedName.get(clazz).compareTo(NestedName.get(o.clazz));
			}
		}

		void closeClassBody() {
			sourceWriter.outdent();
			sourceWriter.println("//@formatter:on");
			sourceWriter.println("}");
			printWriter.close();
		}
	}

	static class PackagePropertiesUnitData extends PersistentUnitData {
		public static final transient int VERSION = 1;

		@Override
		public int currentVersion() {
			return VERSION;
		}

		boolean hasUnitException() {
			return declarationPropertiesList.stream()
					.anyMatch(p -> Ax.notBlank(p.exception));
		}

		List<DeclarationProperties> declarationPropertiesList;

		boolean hasTypedProperties() {
			return declarationPropertiesList.stream()
					.anyMatch(DeclarationProperties::hasTypedProperties);
		}

		@Override
		protected void compute(File file, CompilationUnits compilationUnits) {
			putFile(file);
			CompilationUnitWrapper unit = compilationUnits.ensureUnit(file);
			unit.ensureUnitTypeDeclarations();
			declarationPropertiesList = unit.unitTypes.stream()
					.map(unitType -> new DeclarationProperties(unitType, unit,
							compilationUnits))
					.collect(Collectors.toList());
			updateMetadata();
		}

		static class DeclarationProperties
				implements Comparable<DeclarationProperties> {
			DeclarationProperties() {
			}

			boolean hasTypedProperties;

			String exception;

			boolean hasTypedProperties() {
				return hasTypedProperties;
			}

			String qualifiedBinaryName;

			transient Class clazz;

			Class clazz() {
				if (clazz == null) {
					clazz = CodeService.forName(qualifiedBinaryName);
				}
				return clazz;
			}

			DeclarationProperties(UnitType unitType,
					CompilationUnitWrapper unit, CompilationUnits units) {
				ClassOrInterfaceDeclaration decl = unitType.getDeclaration();
				/*
				 * initial impl - use JDK reflection. pure-source reflection wd
				 * be nice, but (again) basically involve a reimplementation of
				 * the JDK class model...a bit.
				 */
				if (unitType.provideIsLocal()) {
					return;
				}
				try {
					qualifiedBinaryName = unitType.qualifiedBinaryName;
					Class<?> clazz = clazz();
					ClassReflector classReflector = ClassReflectorProvider
							.getClassReflector(clazz);
					hasTypedProperties = classReflector
							.has(TypedProperties.class);
				} catch (Throwable e) {
					// e.printStackTrace();
					exception = CommonUtils.toSimpleExceptionMessage(e);
					Ax.err("%s :: %s", unitType.qualifiedBinaryName, exception);
				}
			}

			@Override
			public int compareTo(DeclarationProperties o) {
				return NestedName.get(clazz())
						.compareTo(NestedName.get(o.clazz()));
			}
		}
	}
}
