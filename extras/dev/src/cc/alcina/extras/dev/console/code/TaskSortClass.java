package cc.alcina.extras.dev.console.code;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithAccessModifiers;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithStaticModifier;

import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitWrapperVisitor;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.util.FsObjectCache;
import cc.alcina.framework.entity.util.PersistentObjectCache.SingletonCache;
import cc.alcina.framework.servlet.schedule.PerformerTask;

@TypeSerialization(flatSerializable = false, reflectiveSerializable = false)
public class TaskSortClass extends PerformerTask.Fields {
	public String classNameRegex = ".*";

	public List<String> classPathList;

	transient CompilationUnits compUnits;

	public boolean refresh;

	public boolean test = true;

	public boolean doNotOrderNonStaticFields = true;

	@Override
	public void run() throws Exception {
		SingletonCache<CompilationUnits> cache = FsObjectCache
				.singletonCache(CompilationUnits.class, getClass())
				.asSingletonCache();
		compUnits = CompilationUnits.load(cache, classPathList,
				CompilationUnitWrapperVisitor.Noop::new, refresh);
		compUnits.unitTypeByName.allValues().stream()
				.map(type -> type.unitWrapper).distinct()
				.map(unitWrapper -> unitWrapper.unitTypes.get(0))
				.filter(type -> type.qualifiedSourceName
						.matches(classNameRegex))
				.forEach(this::visit);
		compUnits.writeDirty(test);
	}

	void visit(UnitType type) {
		try {
			visit0(type);
		} catch (VerifyError ve) {
			Ax.out("Verify error: %s", type.qualifiedSourceName);
		} catch (Throwable t) {
			Ax.out("Other visitor issue: %s", type.qualifiedSourceName);
			t.printStackTrace();
		}
	}

	void visit0(UnitType type) {
		ClassOrInterfaceDeclaration declaration = type.getDeclaration();
		List<OrderedSourceNode> sourceNodes = declaration.getMembers().stream()
				.map(member -> new OrderedSourceNode(declaration, member))
				.collect(Collectors.toList());
		List<OrderedSourceNode> preSort = sourceNodes.stream().toList();
		sourceNodes.sort(null);
		if (!Objects.equals(preSort, sourceNodes)) {
			type.dirty();
		}
		sourceNodes.forEach(OrderedSourceNode::remove);
		sourceNodes.forEach(OrderedSourceNode::add);
	}

	enum StaticOrder {
		_static, _instance;

		static StaticOrder forMember(BodyDeclaration member) {
			if (member instanceof NodeWithStaticModifier) {
				return ((NodeWithStaticModifier) member).isStatic() ? _static
						: _instance;
			} else {
				return _instance;
			}
		}
	}

	enum TypeOrder {
		_class, field, initializer, constructor, method;

		static TypeOrder forMember(BodyDeclaration member) {
			if (member instanceof TypeDeclaration) {
				return _class;
			}
			if (member instanceof FieldDeclaration) {
				return field;
			}
			if (member instanceof ConstructorDeclaration) {
				return constructor;
			}
			if (member instanceof MethodDeclaration) {
				return method;
			}
			throw new UnsupportedOperationException(
					"Unimplemented method 'forMember'");
		}
	}

	static AccessSpecifier memberAccess(BodyDeclaration member) {
		if (member instanceof NodeWithAccessModifiers) {
			return ((NodeWithAccessModifiers) member).getAccessSpecifier();
		} else {
			return AccessSpecifier.NONE;
		}
	}

	class OrderedSourceNode implements Comparable<OrderedSourceNode> {
		BodyDeclaration member;

		Node languageNode;

		TypeOrder typeOrder;

		AccessSpecifier accessSpecifier;

		StaticOrder staticOrder;

		ClassOrInterfaceDeclaration declaration;

		OrderedSourceNode(ClassOrInterfaceDeclaration declaration,
				BodyDeclaration<?> member) {
			this.declaration = declaration;
			this.member = member;
			typeOrder = TypeOrder.forMember(member);
			accessSpecifier = memberAccess(member);
			staticOrder = StaticOrder.forMember(member);
		}

		void remove() {
			member.remove();
		}

		void add() {
			if (name().equals("Inner3")) {
				int debug = 3;
			}
			declaration.addMember(member);
		}

		boolean isNonStaticField() {
			return typeOrder == TypeOrder.field
					&& staticOrder == StaticOrder._instance;
		}

		@Override
		public int compareTo(OrderedSourceNode o) {
			{
				int cmp = typeOrder.compareTo(o.typeOrder);
				if (cmp != 0) {
					return cmp;
				}
			}
			if (typeOrder != TypeOrder._class) {
				{
					int cmp = staticOrder.compareTo(o.staticOrder);
					if (cmp != 0) {
						return cmp;
					}
				}
			}
			if (doNotOrderNonStaticFields && isNonStaticField()
					&& o.isNonStaticField()) {
				return 0;
			}
			{
				int cmp = accessSpecifier.compareTo(o.accessSpecifier);
				if (cmp != 0) {
					return cmp;
				}
			}
			return name().compareTo(o.name());
		}

		String name() {
			if (member instanceof NodeWithSimpleName) {
				return ((NodeWithSimpleName) member).getNameAsString();
			}
			if (member instanceof FieldDeclaration) {
				return ((FieldDeclaration) member).getVariable(0)
						.getNameAsString();
			}
			throw new UnsupportedOperationException();
		}
	}
}
