package cc.alcina.framework.servlet.component.featuretree;

import java.util.Optional;

import com.github.javaparser.ast.comments.JavadocComment;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceChangeEvent;

import cc.alcina.framework.entity.util.source.SourceNodes;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafRenderer;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

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
			if (featurePlace.feature != null) {
				try {
					Optional<JavadocComment> javadocComment = SourceNodes
							.getTypeJavadoc(featurePlace.feature);
					if (javadocComment.isPresent()) {
						content = javadocComment.get().getContent();
						content = content.replaceAll("\n\\s*\\*", "\n");
						content = content.replaceFirst("@author.+", "");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		setContent(content);
	}
}
