package cc.alcina.extras.dev.console.crud;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlRootElement;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import cc.alcina.extras.dev.console.DevConsoleCommand;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.console.FilterArgvParam;
import cc.alcina.framework.entity.entityaccess.WrappedObject.WrappedObjectHelper;

public class CmdGenerateCrudUI extends DevConsoleCommand {
	Spec spec;

	@Override
	public String[] getCommandIds() {
		return new String[] { "crud" };
	}

	@Override
	public String getDescription() {
		return "generate cruddy classes for an entity";
	}

	@Override
	public String getUsage() {
		return "crud <path-to-crud-spec>";
	}

	@Override
	public boolean rerunIfMostRecentOnRestart() {
		return false;
	}

	@Override
	public String run(String[] argv) throws Exception {
		if (argv.length != 1) {
			Ax.out("spec.fn");
			return "";
		}
		FilterArgvParam argvParam = new FilterArgvParam(argv);
		String specPath = argvParam.value;
		String specXml = ResourceUtilities.read(specPath);
		spec = WrappedObjectHelper.xmlDeserialize(Spec.class, specXml);
		TemplateGenerator gen = new TemplateGenerator();
		gen.generateLookup();
		gen.generateFile(GeneratedUnitType.Place);
		gen.generateFile(GeneratedUnitType.BaseCriterion);
		for (String referredObjectPath : spec.referredObjectPaths) {
			gen.generateReferredObject(referredObjectPath);
		}
		gen.generateFile(GeneratedUnitType.SearchDefinition);
		gen.generateFile(GeneratedUnitType.SearchOrders);
		gen.generateFile(GeneratedUnitType.TextCriterionPack);
		String blockSearchables = gen.referredCriterionConstructors.stream()
				.map(s -> Ax.format("searchables.add(%s);", s))
				.collect(Collectors.joining("\n"));
		gen.set("block-referredCriterionSearchables", blockSearchables);
		gen.generateFile(GeneratedUnitType.Searchables);
		return "hyup";
	}

	@XmlRootElement
	public static class Spec {
		public List<String> referredObjectPaths = new ArrayList<>();

		public String entityClassPath;

		public String token;
	}

	enum GeneratedUnitType {
		Place, Referred_object_handler, BaseCriterion, SearchDefinition,
		SearchOrders, TextCriterionPack, Searchables, Referred_object;
		public String outputPath(TemplateGenerator gen) {
			switch (this) {
			case Place:
				return Ax.format("%s/place/%sPlace.java", gen.outputBasePath,
						gen.entityName);
			case BaseCriterion:
				return Ax.format("%s/%sCriterionHandler.java",
						gen.searchBasePath, gen.entityName);
			case Referred_object_handler:
				return Ax.format("%s/%sObjectCriterionPack.java",
						gen.searchBasePath, gen.referredObjectName);
			case Referred_object:
				return Ax.format("%s/%sObjectCriterionPack.java",
						gen.referredObjectBasePath, gen.referredObjectName);
			case SearchDefinition:
				return Ax.format("%s/%sSearchDefinition.java",
						gen.searchBasePath, gen.entityName);
			case SearchOrders:
				return Ax.format("%s/%sSearchOrders.java", gen.searchBasePath,
						gen.entityName);
			case TextCriterionPack:
				return Ax.format("%s/%sTextCriterionPack.java",
						gen.searchBasePath, gen.entityName);
			case Searchables:
				return Ax.format("%s/%sSearchables.java", gen.searchBasePath,
						gen.entityName);
			default:
				throw new UnsupportedOperationException();
			}
		}

		public String templateRelativePath() {
			switch (this) {
			case Place:
				return "CrudPlace.java.template";
			case BaseCriterion:
				return "CrudCriterionHandler.java.template";
			case Referred_object_handler:
				return "CrudReferredObjectCriterionPack.java.template";
			case Referred_object:
				return "CrudObjectCriterionPack.java.template";
			case SearchDefinition:
				return "CrudSearchDefinition.java.template";
			case SearchOrders:
				return "CrudSearchOrders.java.template";
			case TextCriterionPack:
				return "CrudTextCriterionPack.java.template";
			case Searchables:
				return "CrudSearchables.java.template";
			default:
				throw new UnsupportedOperationException();
			}
		}
	}

	class TemplateGenerator {
		String templateRelativePath;

		StringMap replaceLookup = new StringMap();

		private String entityPackageName;

		private String entityName;

		private String searchDefinitionName;

		private String placeName;

		private String searchDefinitionPackageName;

		private String outputBasePath;

		private String placePackageName;

		private String referredObjectPackageName;

		private String referredObjectName;

		private String searchBasePath;

		private List<String> referredCriterionConstructors = new ArrayList<>();

		private String referredObjectBasePath;

		public void generateFile(GeneratedUnitType unitType) {
			String template = ResourceUtilities
					.readClazzp(unitType.templateRelativePath());
			String out = replaceLookup.replaceSubstrings(template);
			String outPath = unitType.outputPath(this);
			outPath = outPath.replace("/persistent/", "/");
			ResourceUtilities.write(out, outPath);
			Ax.out("Wrote %s file: \n\t%s", unitType, outPath);
		}

		public void generateLookup() throws Exception {
			File file = new File(spec.entityClassPath);
			this.outputBasePath = spec.entityClassPath
					.replaceFirst("(.+)/.+/.+", "$1");
			CompilationUnit unit = StaticJavaParser.parse(file);
			this.entityPackageName = unit.getPackageDeclaration().get()
					.getNameAsString();
			this.entityName = unit.getType(0).getNameAsString();
			this.searchBasePath = Ax.format("%s/search/%s", this.outputBasePath,
					this.entityName.toLowerCase());
			new File(this.searchBasePath).mkdirs();
			this.searchDefinitionPackageName = Ax.format("%s.search.%s",
					this.entityPackageName.replaceFirst("(.+)\\..+", "$1"),
					this.entityName.toLowerCase());
			this.placePackageName = Ax.format("%s.place",
					this.entityPackageName.replaceFirst("(.+)\\..+", "$1"));
			this.searchDefinitionName = this.entityName + "SearchDefinition";
			this.placeName = this.entityName + "Place";
			set("entity-package-name", this.entityPackageName);
			set("place-package-name", this.placePackageName);
			set("searchDefinition-package-name",
					this.searchDefinitionPackageName);
			set("searchDefinition-package", this.searchDefinitionPackageName);
			set("entity-name", "%s.%s", this.entityPackageName,
					this.entityName);
			set("entity-simpleName", this.entityName);
			set("place-simpleName", this.placeName);
			set("searchDefinition-name", "%s.%s",
					this.searchDefinitionPackageName,
					this.searchDefinitionName);
			set("searchDefinition-simpleName", this.searchDefinitionName);
			set("searchables-simpleName", this.entityName + "Searchables");
			set("baseCriterionHandler-simpleName",
					this.entityName + "CriterionHandler");
			set("searchOrders-simpleName", this.entityName + "SearchOrders");
			set("textCriterionPack-simpleName",
					this.entityName + "TextCriterionPack");
			set("textCriterionHandler-simpleName",
					this.entityName + "TextCriterionHandler");
			set("textCriterionSearchable-simpleName",
					this.entityName + "TextCriterionSearchable");
			set("textCriterionSearchable-packageName",
					Ax.format("%s%s.%s%s", this.entityName, "TextCriterionPack",
							this.entityName, "TextCriterionSearchable"));
			set("token", spec.token);
		}

		public void generateReferredObject(String referredObjectPath)
				throws Exception {
			File file = new File(referredObjectPath);
			CompilationUnit unit = StaticJavaParser.parse(file);
			this.referredObjectName = unit.getType(0).getNameAsString();
			this.referredObjectPackageName = Ax.format("%s.search.%s",
					this.entityPackageName.replaceFirst("(.+)\\..+", "$1"),
					this.referredObjectName.toLowerCase());
			this.referredObjectBasePath = Ax.format("%s/search/%s",
					this.outputBasePath, this.referredObjectName.toLowerCase());
			set("criterionPack-simpleName", "%sObjectCriterionPack",
					referredObjectName);
			set("criterionHandler-simpleName", "%sCriterionHandler",
					referredObjectName);
			set("criterion-simpleName", "%sObjectCriterion",
					referredObjectName);
			set("entityCriterion-simpleName", "%s%sObjectCriterion", entityName,
					referredObjectName);
			set("referredObjectSearchable-simpleName", "%sObjectSearchable",
					referredObjectName);
			set("referredObject-name", "%s.%s", this.referredObjectPackageName,
					this.referredObjectName);
			set("referredEntity-name", "%s.%s", this.entityPackageName,
					this.referredObjectName);
			set("referredObject-package-name", this.referredObjectPackageName);
			set("referredObject-simpleName", this.referredObjectName);
			generateFile(GeneratedUnitType.Referred_object_handler);
			generateFile(GeneratedUnitType.Referred_object);
			referredCriterionConstructors
					.add(Ax.format("new %sObjectSearchable(%s.class)",
							this.referredObjectName, this.entityName));
		}

		private void set(String key, String valueTemplate, Object... args) {
			replaceLookup.put(Ax.format("${%s}", key),
					Ax.format(valueTemplate, args));
		}
	}
}