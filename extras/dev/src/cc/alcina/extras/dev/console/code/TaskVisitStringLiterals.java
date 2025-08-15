package cc.alcina.extras.dev.console.code;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;

import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitWrapper;
import cc.alcina.extras.dev.console.code.CompilationUnits.CompilationUnitWrapperVisitor;
import cc.alcina.extras.dev.console.code.CompilationUnits.TypeFlag;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.util.Csv;
import cc.alcina.framework.entity.util.FsObjectCache;
import cc.alcina.framework.entity.util.PersistentObjectCache.SingletonCache;
import cc.alcina.framework.servlet.schedule.PerformerTask;

/**
 * <p>
 * This task visits the string literals in a java package (located at
 * {@link #classPath} ), optionally mutating them
 * <p>
 * Initial uses are for translations
 */
@TypeSerialization(flatSerializable = false, reflectiveSerializable = false)
public class TaskVisitStringLiterals extends PerformerTask.Fields {
	public boolean refresh;

	transient CompilationUnits compUnits;

	public String classPath;

	public int dirtyWriteLimit;

	transient SourceHandler sourceHandler;

	@Override
	public void run() throws Exception {
		scanCodeRefs();
		logLiterals();
	}

	void scanCodeRefs() throws Exception {
		SingletonCache<CompilationUnits> cache = FsObjectCache
				.singletonCache(CompilationUnits.class, getClass())
				.asSingletonCache();
		compUnits = CompilationUnits.load(cache, List.of(classPath),
				DeclarationVisitor::new, refresh);
		long count = compUnits.declarations.values().stream()
				.filter(dec -> dec.hasFlag(Type.HasLiterals)).count();
		Ax.out("count with literals: %s", count);
		sourceHandler = new SourceHandler();
		compUnits.declarations.values().stream().filter(UnitType::exists)
				.forEach(dec -> sourceHandler.visitLiterals(dec));
		if (dirtyWriteLimit != 0) {
			sourceHandler.transformLiterals();
			compUnits.writeDirty(false, dirtyWriteLimit);
		}
	}

	void logLiterals() {
		sourceHandler.refs.sort(null);
		String resultCsv = Csv.fromCollection(sourceHandler.refs)
				.toOutputString();
		Io.write().string(resultCsv).toPath("/tmp/string-literals.csv");
	}

	static class DeclarationVisitor extends CompilationUnitWrapperVisitor {
		public DeclarationVisitor(CompilationUnits units,
				CompilationUnitWrapper compUnit) {
			super(units, compUnit);
		}

		@Override
		public void visit(ClassOrInterfaceDeclaration node, Void arg) {
			try {
				visit0(node, arg);
			} catch (VerifyError ve) {
				Ax.out("Verify error: %s", node.getName());
			}
		}

		private void visit0(ClassOrInterfaceDeclaration node, Void arg) {
			boolean hasNonAnnotationLiteral = node.stream()
					.filter(n -> n instanceof StringLiteralExpr).anyMatch(n -> {
						Node cursor = n;
						while (cursor != null) {
							if (cursor instanceof AnnotationExpr) {
								return false;
							}
							cursor = cursor.getParentNode().orElse(null);
						}
						return true;
					});
			UnitType type = new UnitType(unit, node);
			type.setDeclaration(node);
			unit.unitTypes.add(type);
			if (hasNonAnnotationLiteral) {
				type.setFlag(Type.HasLiterals);
			}
			super.visit(node, arg);
		}
	}

	public interface NameResolver {
		public UnitType resolve(List<UnitType> choices, Object source,
				String name);
	}

	class SourceHandler {
		List<Ref> refs = new ArrayList<>();

		void visitLiterals(UnitType type) {
			type.getDeclaration().stream()
					.filter(n -> n instanceof StringLiteralExpr)
					.map(n -> (StringLiteralExpr) n).filter(n -> {
						Node cursor = n;
						while (cursor != null) {
							if (cursor instanceof AnnotationExpr) {
								return false;
							}
							cursor = cursor.getParentNode().orElse(null);
						}
						return true;
					}).forEach(expr -> {
						refs.add(new Ref(expr, type));
					});
		}

		void logLiterals() {
		}

		void transformLiterals() {
		}

		@Bean(PropertySource.FIELDS)
		class Ref implements Comparable<Ref> {
			@Property.Not
			UnitType type;

			@Property.Not
			StringLiteralExpr expr;

			String path;

			String fileName;

			String typeName;

			String methodName;

			String fieldName;

			int lineNumber;

			String line;

			String literal;

			Boolean localizable;

			Ref(StringLiteralExpr expr, UnitType type) {
				this.expr = expr;
				this.type = type;
				path = type.unitWrapper.path;
				fileName = new File(path).getName();
				typeName = type.getDeclaration().getNameAsString();
				methodName = expr.findAncestor(MethodDeclaration.class)
						.map(MethodDeclaration::getNameAsString).orElse(null);
				fieldName = expr.findAncestor(FieldDeclaration.class)
						.map(fd -> {
							VariableDeclarator variable = fd.getVariable(0);
							String fieldName = variable.getNameAsString();
							return fieldName;
						}).orElse(null);
				lineNumber = expr.getRange().get().begin.line;
				line = type.unitWrapper.content().getLine(lineNumber);
				literal = expr.getValue();
			}

			@Override
			public int compareTo(Ref o) {
				{
					int cmp = fileName.compareTo(o.fileName);
					if (cmp != 0) {
						return cmp;
					}
				}
				{
					int cmp = lineNumber - o.lineNumber;
					if (cmp != 0) {
						return cmp;
					}
				}
				{
					int cmp = path.compareTo(o.path);
					if (cmp != 0) {
						return cmp;
					}
				}
				return 0;
			}
		}
	}

	enum Type implements TypeFlag {
		HasLiterals
	}
}
