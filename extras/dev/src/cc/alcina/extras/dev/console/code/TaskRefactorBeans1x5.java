package cc.alcina.extras.dev.console.code;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitWrapper;
import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitWrapperVisitor;
import cc.alcina.extras.dev.console.code.CompilationUnits.TypeFlag;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer.ReflectiveSerializable;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.util.TextUtils;
import cc.alcina.framework.entity.util.FsObjectCache;
import cc.alcina.framework.entity.util.PersistentObjectCache.SingletonCache;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedRenderer;
import cc.alcina.framework.servlet.schedule.PerformerTask;

/*

 * @formatter:off
 *

Plan (impl):
Property:
- Add field support
  - can serialize this task?
- @Bean resolver (since we want tree serializable)


Tool:
- Find beans (@bean annotation)
- Find tree/reflect serializable
- Find beanlike (has property methods and corresponding field)
- Note - use jvm, not javaparser model (quicker)
- Fix signature generation tasks


 *
 *@formatter:on
 *
 */
@Bean(PropertySource.FIELDS)
// (tmp)
@TypeSerialization(reflectiveSerializable = false, flatSerializable = false)
public class TaskRefactorBeans1x5 extends PerformerTask {
	transient CompilationUnits compUnits;

	public boolean overwriteOriginals;

	public String classPathList;

	public boolean refresh;

	public Action action;

	public boolean test;

	public Class onlyAssignableFrom;

	public String classNameFilter;

	public transient List<String> warnings = new ArrayList<>();

	void accessToPackage(UnitType type) {
		// TODO:
		// verify that class has no public fields
		ClassOrInterfaceDeclaration declaration = type.getDeclaration();
		List<FieldDeclaration> targetFields = declaration.getFields().stream()
				.filter(f -> !f.isStatic()).filter(f -> !f.isTransient())
				.collect(Collectors.toList());
		List<FieldDeclaration> publicOrProtectedFields = targetFields.stream()
				.filter(f -> f.getAccessSpecifier() == AccessSpecifier.PUBLIC
						|| f.getAccessSpecifier() == AccessSpecifier.PROTECTED)
				.collect(Collectors.toList());
		if (publicOrProtectedFields.size() > 0) {
			List<String> names = publicOrProtectedFields.stream()
					.map(f -> f.getVariables().get(0).getNameAsString())
					.collect(Collectors.toList());
			warn(type, "Fields with public/protected access: %s - %s ",
					type.simpleName(), names.toString());
			return;
		}
		targetFields.stream().filter(f -> f.isPrivate()).forEach(f -> {
			type.dirty();
			f.setPrivate(false);
		});
		if (declaration.isPublic()) {
			type.dirty();
			declaration.setPublic(false);
			declaration.getConstructors().forEach(con -> con.setPublic(false));
		}
	}

	boolean filter(UnitType type) {
		if (onlyAssignableFrom != null) {
			return onlyAssignableFrom.isAssignableFrom(type.clazz());
		} else if (classNameFilter != null) {
			String name = type.clazz().getName();
			return name.matches(classNameFilter);
		} else {
			return true;
		}
	}

	void modelToModelFields(UnitType type) {
		ClassOrInterfaceDeclaration decl = type.getDeclaration();
		NodeList<ClassOrInterfaceType> extendedTypes = decl.getExtendedTypes();
		List<Property> properties = Reflections.at(type.clazz()).properties();
		if (properties.isEmpty()) {
			return;
		}
		if (extendedTypes.size() == 1
				&& extendedTypes.get(0).getNameAsString().equals("Model")) {
			type.dirty();
			extendedTypes.get(0).setName("Model.Fields");
		}
	}

	void removeDefaultPropertyMethods(UnitType type) {
		// Ax.out("Removing methods: %s", NestedName.get(type.clazz()));
		ClassOrInterfaceDeclaration decl = type.getDeclaration();
		List<Property> properties = Reflections.at(type.clazz()).properties();
		List<String> warns = new ArrayList<>();
		for (Property property : properties) {
			String propertyName = property.getName();
			FieldDeclaration field = decl.getFieldByName(propertyName)
					.orElse(null);
			if (field == null) {
				// warns.add(Ax.format("%s - no field", propertyName));
				continue;
			}
			VariableDeclarator fieldVariable = field.getVariables().get(0);
			boolean isBoolean = fieldVariable.getType().asString()
					.equals("boolean");
			String fieldMethodNamePart = propertyName.substring(0, 1)
					.toUpperCase() + propertyName.substring(1);
			{
				String getMethodName = (isBoolean ? "is" : "get")
						+ fieldMethodNamePart;
				MethodDeclaration getterDeclaration = decl
						.getMethodsByName(getMethodName).stream()
						.filter(m -> m.getParameters().size() == 0).findFirst()
						.orElse(null);
				if (getterDeclaration != null) {
					BlockStmt blockStmt = getterDeclaration.getBody().get();
					if (blockStmt.getChildNodes().size() == 1) {
						String string = blockStmt.getChildNodes().get(0)
								.toString();
						String defaultBody = Ax.format("return this.%s;",
								propertyName);
						if (Objects.equals(string, defaultBody)) {
							type.dirty();
							getterDeclaration.getAnnotations().forEach(ann -> {
								field.addAnnotation(ann);
							});
							getterDeclaration.remove();
						} else {
							warns.add(
									Ax.format("%s - getter - non default body",
											propertyName));
							continue;
						}
					}
				} else {
					warns.add(Ax.format("%s - no getter", propertyName));
				}
			}
			{
				String setMethodName = "set" + fieldMethodNamePart;
				MethodDeclaration setterDeclaration = decl
						.getMethodsByName(setMethodName).stream()
						.filter(m -> m.getParameters().size() == 1).findFirst()
						.orElse(null);
				if (setterDeclaration != null) {
					BlockStmt blockStmt = setterDeclaration.getBody().get();
					if (blockStmt.getChildNodes().size() == 1) {
						String string = blockStmt.getChildNodes().get(0)
								.toString();
						String defaultBody = Ax.format("this.%s = %s;",
								propertyName, propertyName);
						if (Objects.equals(string, defaultBody)) {
							type.dirty();
							setterDeclaration.remove();
						} else {
							warns.add(
									Ax.format("%s - setter - non default body",
											propertyName));
							continue;
						}
					}
				} else {
					// warns.add(Ax.format("%s - no setter", propertyName));
				}
			}
		}
		warns.forEach(s -> warn(type, "  %s", s));
	}

	@Override
	public void run() throws Exception {
		StringMap classPaths = StringMap.fromStringList(classPathList);
		SingletonCache<CompilationUnits> cache = FsObjectCache
				.singletonCache(CompilationUnits.class, getClass())
				.asSingletonCache();
		compUnits = CompilationUnits.load(cache, classPaths.keySet(),
				DeclarationVisitor::new, refresh);
		compUnits.declarations.values().stream().filter(dec -> dec.hasFlags())
				.filter(this::filter).forEach(type -> {
					switch (action) {
					case LIST_INTERESTING: {
						Ax.out("%s - %s", type.clazz().getSimpleName(),
								type.typeFlags);
						break;
					}
					case ACCESS_TO_PACKAGE: {
						accessToPackage(type);
						break;
					}
					case MANIFEST: {
						accessToPackage(type);
						modelToModelFields(type);
						removeDefaultPropertyMethods(type);
						updatePropertySetters(type);
						break;
					}
					case TRANSFORM_TO_TRANSFORM_ELEMENTS: {
						transformToTransformElements(type);
						break;
					}
					}
				});
		compUnits.writeDirty(test);
		Ax.out("\n\n");
		Ax.err(warnings.stream().collect(Collectors.joining("\n")));
	}

	void transformToTransformElements(UnitType type) {
		if (type.hasFlag(Type.HasBeanAnnotation)
				|| type.hasFlag(Type.HasPropertyMethods)) {
			ClassReflector<?> reflector = Reflections.at(type.clazz());
			List<TransformableProperty> props = reflector.properties().stream()
					.map(t -> new TransformableProperty(type, t))
					.filter(TransformableProperty::isTransformable)
					.collect(Collectors.toList());
			if (props.size() > 0) {
				type.dirty();
				props.forEach(p -> p.apply(type));
			}
		}
	}

	void updatePropertySetters(UnitType type) {
		ClassOrInterfaceDeclaration decl = type.getDeclaration();
		List<Property> properties = Reflections.at(type.clazz()).properties();
		List<String> warns = new ArrayList<>();
		boolean hasSetters = false;
		for (Property property : properties) {
			String propertyName = property.getName();
			FieldDeclaration field = decl.getFieldByName(propertyName)
					.orElse(null);
			if (field == null) {
				// warns.add(Ax.format("%s - no field", propertyName));
				continue;
			}
			VariableDeclarator fieldVariable = field.getVariables().get(0);
			boolean isBoolean = fieldVariable.getType().asString()
					.equals("boolean");
			String fieldMethodNamePart = propertyName.substring(0, 1)
					.toUpperCase() + propertyName.substring(1);
			{
				String setMethodName = "set" + fieldMethodNamePart;
				MethodDeclaration setterDeclaration = decl
						.getMethodsByName(setMethodName).stream()
						.filter(m -> m.getParameters().size() == 1).findFirst()
						.orElse(null);
				if (setterDeclaration != null) {
					hasSetters = true;
					String defaultOldChange = ("\\{\\s*\\S+ old_%s = this.%s;\n"
							+ "this.%s = %s;\n"
							+ "propertyChangeSupport\\(\\).firePropertyChange\\(\"%s\",\n"
							+ "old_%s, %s\\s*\\);\\s*\\}").replace("%s",
									propertyName);
					String defaultOldChangeNormalised = TextUtils
							.normalizeWhitespaceAndTrim(defaultOldChange);
					BlockStmt blockStmt = setterDeclaration.getBody().get();
					String setterBodyNormalised = TextUtils
							.normalizeWhitespaceAndTrim(blockStmt.toString());
					if (setterBodyNormalised
							.matches(defaultOldChangeNormalised)) {
						type.dirty();
						blockStmt.getChildNodes().stream()
								.collect(Collectors.toList()).forEach(
										com.github.javaparser.ast.Node::remove);
						String newSource = "set(\"%s\", this.%s, %s,() -> this.%s = %s);"
								.replace("%s", propertyName);
						Statement parse = StaticJavaParser
								.parseStatement(newSource);
						blockStmt.addStatement(parse);
					}
				} else {
					// warns.add(Ax.format("%s - no setter", propertyName));
				}
			}
		}
		if (hasSetters) {
			Ax.out("Updated setters: %s", NestedName.get(type.clazz()));
		}
		warns.forEach(s -> Ax.out("  %s", s));
	}

	private void warn(UnitType type, String template, Object... args) {
		String message = Ax.format(template, args);
		message = Ax.format("%s :: %s", type.getDeclaration().getNameAsString(),
				message);
		Ax.err(message);
		warnings.add(message);
	}

	public enum Action {
		LIST_INTERESTING, MANIFEST, ACCESS_TO_PACKAGE,
		TRANSFORM_TO_TRANSFORM_ELEMENTS;
	}

	static class DeclarationVisitor extends CompilationUnitWrapperVisitor {
		public DeclarationVisitor(CompilationUnits units,
				CompilationUnitWrapper compUnit) {
			super(units, compUnit);
		}

		boolean hasBeanAnnotation(UnitType type) {
			return Reflections.at(type.clazz()).has(Bean.class);
		}

		boolean hasPropertyMethods(UnitType type) {
			return Reflections.at(type.clazz()).properties().size() > 0;
		}

		boolean hasRegistrations(UnitType type) {
			return Reflections.at(type.clazz()).has(Registration.class);
		}

		boolean isTreeOrReflectSerializable(UnitType type) {
			Class clazz = type.clazz();
			return TreeSerializable.class.isAssignableFrom(clazz)
					|| ReflectiveSerializable.class.isAssignableFrom(clazz);
		}

		@Override
		public void visit(ClassOrInterfaceDeclaration node, Void arg) {
			try {
				visit0(node, arg);
			} catch (VerifyError ve) {
				Ax.out("Verify error: %s", node.getName());
			}
		}

		void visit0(ClassOrInterfaceDeclaration node, Void arg) {
			if (!node.isInterface()) {
				UnitType type = new UnitType(unit, node);
				String nameAsString = type.getDeclaration().getNameAsString();
				type.setDeclaration(node);
				unit.unitTypes.add(type);
				if (hasBeanAnnotation(type)) {
					type.setFlag(Type.HasBeanAnnotation);
				}
				if (isTreeOrReflectSerializable(type)) {
					type.setFlag(Type.TreeOrReflectSerializable);
				}
				if (hasPropertyMethods(type)) {
					type.setFlag(Type.HasPropertyMethods);
				}
				if (hasRegistrations(type)) {
					type.setFlag(Type.HasRegistrations);
				}
			}
			super.visit(node, arg);
		}
	}

	class TransformableProperty {
		Property property;

		boolean transformable;

		TransformableProperty(UnitType type, Property property) {
			this.property = property;
			if (property.has(Directed.Transform.class)) {
				if (java.util.Collection.class
						.isAssignableFrom(property.getType())) {
					if (property.has(Directed.class) && property
							.annotation(Directed.class)
							.renderer() == DirectedRenderer.TransformRenderer.class) {
						warn(type, "non-element transform :: %s",
								NestedName.get(property.getOwningType()));
					} else {
						transformable = true;
					}
				}
			}
		}

		public void apply(UnitType type) {
		}

		boolean isTransformable() {
			return transformable;
		}
	}

	enum Type implements TypeFlag {
		HasBeanAnnotation, TreeOrReflectSerializable, HasPropertyMethods,
		HasRegistrations
	}
}
