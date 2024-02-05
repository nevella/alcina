package cc.alcina.framework.servlet.component.featuretree;

import java.util.List;
import java.util.Optional;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.comments.JavadocComment;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceChangeEvent;

import cc.alcina.framework.entity.persistence.mvcc.SourceFinder;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafRenderer;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.featuretree.place.FeaturePlace;

class Documentation extends Model.Fields {
	@Directed
	Heading header = new Heading("Documentation");

	@Directed(renderer = LeafRenderer.Html.class, tag = "content")
	String content;

	Documentation() {
		PlaceChangeEvent.Handler handler = evt -> {
			updateContent();
		};
		bindings().addRegistration(() -> Client.eventBus()
				.addHandler(PlaceChangeEvent.TYPE, handler));
		updateContent();
	}

	public void setContent(String content) {
		set("content", this.content, content, () -> this.content = content);
	}

	void updateContent() {
		Place currentPlace = Client.currentPlace();
		String content = null;
		if (currentPlace instanceof FeaturePlace) {
			FeaturePlace featurePlace = (FeaturePlace) currentPlace;
			try {
				String source = SourceFinder.findSource(featurePlace.feature);
				CompilationUnit compilationUnit = StaticJavaParser
						.parse(source);
				List<ClassOrInterfaceDeclaration> childNodesByType = compilationUnit
						.findAll(ClassOrInterfaceDeclaration.class);
				Optional<ClassOrInterfaceDeclaration> o_decl = childNodesByType
						.stream()
						.filter(n -> n.getNameAsString()
								.equals(featurePlace.feature.getSimpleName()))
						.findFirst();
				if (o_decl.isPresent()) {
					ClassOrInterfaceDeclaration decl = o_decl.get();
					Optional<JavadocComment> javadocComment = decl
							.getJavadocComment();
					/*
					 * the comment may (?) be rendered as a node *following* the
					 * type declaration, if there's also a non-javadoc comment
					 * (yup, limitation in the JavaParser model - should support
					 * multiple comments I guess)
					 */
					if (javadocComment.isEmpty()) {
						Node parent = decl.getParentNode().get();
						List<Node> children = parent.getChildNodes();
						int idx = children.indexOf(decl);
						if (idx + 1 < children.size()) {
							Node test = children.get(idx + 1);
							if (test instanceof JavadocComment) {
								javadocComment = Optional
										.of((JavadocComment) test);
							}
						}
					}
					if (javadocComment.isPresent()) {
						content = javadocComment.get().getContent();
						content = content.replaceAll("\n\\s*\\*", "\n");
						content = content.replaceFirst("@author.+", "");
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		setContent(content);
	}
}
