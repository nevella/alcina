package cc.alcina.extras.dev.codeservice;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;

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
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.reflection.impl.ClassReflectorProvider;
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
						.toList();
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

		void write() throws Exception {
			if (packageTypeMetadataList.stream()
					.noneMatch(PackagePropertiesUnitData::hasTypedProperties)) {
				file().delete();
				return;
			}
			ClassSourceFileComposerFactory composerFactory = new ClassSourceFileComposerFactory(
					sourcePackage.packageName, "PackageProperties");
			composerFactory.addImport(TypedProperty.class.getName());
			printWriter = new PrintWriter(file());
			sourceWriter = composerFactory.createSourceWriter(printWriter);
			sourceWriter.println();
			sourceWriter.println("// auto-generated, do not modify");
			sourceWriter.println("class PackageProperties {");
			packageTypeMetadataList.stream()
					.flatMap(list -> list.declarationPropertiesList.stream())
					.map(TypeWriter::new).forEach(TypeWriter::write);
			closeClassBody();
		}

		class TypeWriter {
			DeclarationProperties declarationProperties;

			TypeWriter(DeclarationProperties declarationProperties) {
				this.declarationProperties = declarationProperties;
			}

			void write() {
				int debug = 3;
			}
		}

		void closeClassBody() {
			sourceWriter.outdent();
			sourceWriter.println("}");
			printWriter.close();
		}
	}

	static class PackagePropertiesUnitData extends PersistentUnitData {
		public static final int VERSION = 1;

		@Override
		public int currentVersion() {
			return VERSION;
		}

		List<DeclarationProperties> declarationPropertiesList;

		boolean hasTypedProperties() {
			return declarationPropertiesList.stream()
					.anyMatch(DeclarationProperties::hasTypedProperties);
		}

		@Override
		protected void compute(File file, CompilationUnits compilationUnits) {
			CompilationUnitWrapper unit = compilationUnits
					.ensureUnitWrapper(file);
			declarationPropertiesList = unit.unitTypes.stream()
					.map(unitType -> new DeclarationProperties(unitType,
							compilationUnits))
					.toList();
		}

		static class DeclarationProperties {
			DeclarationProperties() {
			}

			boolean hasTypedProperties;

			boolean hasTypedProperties() {
				return hasTypedProperties;
			}

			String qualifiedBinaryName;

			DeclarationProperties(UnitType unitType,
					CompilationUnits compilationUnits) {
				ClassOrInterfaceDeclaration decl = unitType.getDeclaration();
				/*
				 * initial impl - use JDK reflection. pure-sure reflection wd be
				 * nice, but (again) basically involve a reimplementation of the
				 * JDK class model...a bit
				 */
				Class<?> clazz = unitType.clazz();
				ClassReflector classReflector = ClassReflectorProvider
						.getClassReflector(clazz);
				qualifiedBinaryName = clazz.getName();
				hasTypedProperties = classReflector.has(TypedProperties.class);
			}
		}
	}
}
