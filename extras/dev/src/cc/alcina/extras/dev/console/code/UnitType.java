package cc.alcina.extras.dev.console.code;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Stream;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitWrapper;
import cc.alcina.extras.dev.console.code.CompilationUnits.TypeFlag;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;

public class UnitType {
	public static transient boolean evaluateSuperclassFqn = false;

	public static ClassOrInterfaceDeclaration
			findContainingClassOrInterfaceDeclaration(Node n) {
		Node cursor = n;
		while (cursor != null) {
			if (cursor instanceof ClassOrInterfaceDeclaration) {
				return (ClassOrInterfaceDeclaration) cursor;
			} else {
				cursor = cursor.getParentNode().orElse(null);
			}
		}
		return null;
	}

	private transient ClassOrInterfaceDeclaration declaration;

	public Set<TypeFlag> typeFlags = new LinkedHashSet<>();

	public String name;

	public CompilationUnitWrapper unitWrapper;

	public String qualifiedSourceName;

	public String qualifiedBinaryName;

	public String superclassFqn;

	public boolean invalid;

	public UnitType() {
	}

	public UnitType(CompilationUnitWrapper unit,
			ClassOrInterfaceDeclaration n) {
		this.unitWrapper = unit;
		this.name = n.getNameAsString();
		qualifiedSourceName = CompilationUnits.fqn(unit, n, false);
		qualifiedBinaryName = CompilationUnits.fqn(unit, n, true);
		if (qualifiedSourceName == null) {
			invalid = true;
			return;
		}
		if (evaluateSuperclassFqn) {
			NodeList<ClassOrInterfaceType> extendedTypes = n.getExtendedTypes();
			if (extendedTypes.size() > 1) {
				throw new UnsupportedOperationException();
			} else if (extendedTypes.size() == 1) {
				ClassOrInterfaceType exType = extendedTypes.get(0);
				try {
					superclassFqn = CompilationUnits.fqn(unit, exType);
				} catch (Exception e) {
					CompilationUnits.invalidSuperclassFqns
							.add(exType.getNameAsString());
					if (LooseContext.is(CompilationUnits.CONTEXT_COMP_UNITS)) {
						Ax.out("%s: %s", e.getMessage(),
								exType.getNameAsString());
					}
				}
			}
		}
	}

	public void addAnnotation(AnnotationExpr element) {
		declaration.addAnnotation(element);
		dirty();
	}

	public Class<?> clazz() {
		return Reflections.forName(qualifiedBinaryName);
	}

	/**
	 * Call *before* modification
	 */
	public void dirty() {
		unitWrapper.prepareForModification();
		unitWrapper.dirty = true;
	}

	public void dirty(String initialSource, String ensuredSource) {
		if (!Objects.equals(initialSource, ensuredSource)) {
			dirty();
		}
	}

	public void ensureImport(Class<?> clazz) {
		unitWrapper.ensureImport(clazz);
	}

	public void ensureImport(String name) {
		unitWrapper.ensureImport(name);
	}

	public boolean exists() {
		return new File(unitWrapper.path).exists();
	}

	public ClassOrInterfaceDeclaration getDeclaration() {
		if (declaration == null) {
			unitWrapper.unit().accept(new VoidVisitorAdapter<Void>() {
				@Override
				public void visit(ClassOrInterfaceDeclaration node, Void arg) {
					if (Objects.equals(qualifiedSourceName,
							CompilationUnits.fqn(unitWrapper, node, false))) {
						declaration = node;
					}
					super.visit(node, arg);
				}
			}, null);
		}
		return declaration;
	}

	public boolean hasFlag(TypeFlag flag) {
		return typeFlags.contains(flag);
	}

	public boolean hasFlags() {
		return typeFlags.size() > 0;
	}

	public boolean hasSuperclass(String fqn) {
		if (Objects.equals(fqn, superclassFqn)) {
			return true;
		}
		if (superclassFqn == null) {
			return false;
		}
		UnitType compUnitClassDec = compUnits().declarations.get(superclassFqn);
		if (compUnitClassDec == null) {
			return false;
		}
		return compUnitClassDec.hasSuperclass(fqn);
	}

	public boolean isAssignableFrom(Class<?> from) {
		return from.isAssignableFrom(clazz());
	}

	public String resolveFqn(Type type) {
		return CompilationUnits.fqn(unitWrapper, type.asString());
	}

	public String resolveFqnOrNull(String genericPart) {
		try {
			return CompilationUnits.fqn(unitWrapper, genericPart);
		} catch (Exception e) {
			return null;
		}
	}

	public String resolveFqnOrNull(Type type) {
		try {
			return resolveFqn(type);
		} catch (Exception e) {
			return null;
		}
	}

	/*
	 * Output direct subclass of object *if* it doesn't implement any
	 * interfaces, otherwise root interfaces
	 */
	public Stream<Class<?>> rootTypes() {
		Set<Class<?>> rootTypes = new LinkedHashSet<>();
		Stack<Class> stack = new Stack<>();
		stack.push(clazz());
		while (!stack.isEmpty()) {
			Class cursor = stack.pop();
			Class[] interfaces = cursor.getInterfaces();
			if (cursor.isInterface()) {
				if (interfaces.length == 0) {
					rootTypes.add(cursor);
				}
			} else {
				Class superclass = cursor.getSuperclass();
				if (superclass == Object.class) {
					if (interfaces.length == 0) {
						rootTypes.add(cursor);
					}
				} else {
					stack.push(superclass);
				}
			}
			Arrays.stream(interfaces).forEach(stack::add);
		}
		return rootTypes.stream();
	}

	public void setDeclaration(ClassOrInterfaceDeclaration declaration) {
		this.declaration = declaration;
	}

	public void setFlag(TypeFlag flag) {
		setFlag(flag, true);
	}

	public void setFlag(TypeFlag flag, boolean set) {
		if (flag == null) {
			return;
		}
		if (set) {
			typeFlags.add(flag);
		} else {
			typeFlags.remove(flag);
		}
	}

	public String simpleName() {
		return qualifiedSourceName.replaceFirst(".+\\.", "");
	}

	@Override
	public String toString() {
		return Ax.format("%s: %s", typeFlags, name);
	}

	public <T> T typedInstance() {
		return (T) Reflections.newInstance(clazz());
	}

	private CompilationUnits compUnits() {
		return LooseContext.get(CompilationUnits.CONTEXT_COMP_UNITS);
	}
}