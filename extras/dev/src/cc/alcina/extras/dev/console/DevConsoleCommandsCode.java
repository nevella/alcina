package cc.alcina.extras.dev.console;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.console.FilterArgvParam;

public class DevConsoleCommandsCode {
	public static class CmdApplyBuilder extends DevConsoleCommand {
		@Override
		public boolean canUseProductionConn() {
			return true;
		}

		@Override
		public String[] getCommandIds() {
			return new String[] { "builder" };
		}

		@Override
		public String getDescription() {
			return "convert spec/params object to builder";
		}

		@Override
		public String getUsage() {
			return "builder <filename> <classname>";
		}

		@Override
		public String run(String[] argv) throws Exception {
			if (argv.length != 2) {
				Ax.out("fn/classname");
				return "";
			}
			FilterArgvParam argvParam = new FilterArgvParam(argv);
			String fileName = argvParam.value;
			String className = argvParam.next();
			File file = new File(fileName);
			CompilationUnit unit = StaticJavaParser.parse(file);
			TypeDeclaration typeDeclaration = unit
					.findAll(TypeDeclaration.class).stream()
					.filter(td -> td.getNameAsString().equals(className))
					.findFirst().get();
			BuilderBuilder builderBuilder = new BuilderBuilder(typeDeclaration);
			builderBuilder.addWiths();
			ResourceUtilities.write(unit.toString(), file);
			return "hyup";
		}
	}

	static class BuilderBuilder {
		private TypeDeclaration typeDeclaration;

		private List<FieldDeclaration> fieldDeclarations = new ArrayList<>();

		private List<MethodDeclaration> methodDeclarations = new ArrayList<>();

		public BuilderBuilder(TypeDeclaration typeDeclaration) {
			this.typeDeclaration = typeDeclaration;
			typeDeclaration.accept(new VoidVisitorAdapter() {
				@Override
				public void visit(FieldDeclaration n, Object arg) {
					fieldDeclarations.add(n);
				}

				@Override
				public void visit(MethodDeclaration n, Object arg) {
					methodDeclarations.add(n);
				}
			}, null);
			Set<String> withMethodNames = methodDeclarations.stream()
					.map(md -> md.getNameAsString())
					.filter(n -> n.matches("with.+"))
					.map(n -> n.substring(4, 5).toLowerCase() + n.substring(5))
					.collect(Collectors.toSet());
			fieldDeclarations.stream()
					.filter(f -> !withMethodNames.contains(
							f.getVariables().get(0).getNameAsString()))
					.forEach(f -> {
						String fieldName = f.getVariables().get(0)
								.getNameAsString();
						String methodName = Ax.format("with%s%s",
								fieldName.substring(0, 1).toUpperCase(),
								fieldName.substring(1));
						MethodDeclaration methodDeclaration = typeDeclaration
								.addMethod(methodName, Keyword.PUBLIC);
						methodDeclaration
								.setType(typeDeclaration.getNameAsString());
						methodDeclaration.addAndGetParameter(
								f.getVariables().get(0).getType(), fieldName);
						BlockStmt block = new BlockStmt();
						methodDeclaration.setBody(block);
						block.addAndGetStatement(
								Ax.format("this.%s=%s", fieldName, fieldName));
						block.addAndGetStatement("return this");
					});
		}

		public void addWiths() {
		}
	}
}
