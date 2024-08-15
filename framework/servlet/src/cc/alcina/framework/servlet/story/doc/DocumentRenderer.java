package cc.alcina.framework.servlet.story.doc;

import java.io.File;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.util.FileUtils;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel.Img;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafTransforms;
import cc.alcina.framework.gwt.client.dirndl.layout.Tables;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.story.doc.StoryDocObservable;
import cc.alcina.framework.servlet.publication.DirndlRenderer;

public class DocumentRenderer implements StoryDocRenderer {
	List<StoryDocObservable> observables;

	StoryDocPart part;

	@Override
	public void render(StoryDocPart part, File outputFolder,
			List<StoryDocObservable> observables) {
		this.part = part;
		this.observables = observables;
		Sequence sequence = new Sequence();
		String markup = new DirndlRenderer().withRenderable(sequence)
				.addStyleFile(DocumentRenderer.class, "res/css/styles.css")
				.withWrapStyleInCdata(true).asMarkup();
		File out = FileUtils.child(outputFolder, "document.html");
		Io.write().string(markup).toFile(out);
		Ax.out("Wrote report markup to %s", out);
		File debugOut = FileUtils.child(new File("/tmp/story"),
				"document.html");
		Io.write().string(markup).toFile(debugOut);
		Ax.out("Wrote report markup to %s", debugOut);
	}

	@Directed
	class Sequence extends Model.All {
		@Directed.Transform(Tables.Single.class)
		class ReportMetadata extends Model.All {
			ReportMetadata() {
				device = part.rendererConfiguration.device;
				build = part.rendererConfiguration.build;
				date = part.rendererConfiguration.date;
			}

			String device;

			@Directed.Transform(LeafTransforms.TimestampHuman.class)
			Date date;

			String build;
		}

		class Header extends Model.All {
			Header() {
				heading = part.rendererConfiguration.storyTitle;
			}

			String heading;
		}

		Header header;

		ReportMetadata metadata;

		List<VisitArea> visits;

		Sequence() {
			header = new Header();
			metadata = new ReportMetadata();
			visits = DocumentRenderer.this.observables.stream()
					.map(VisitArea::new).collect(Collectors.toList());
		}

		class VisitArea extends Model.All {
			@Property.Not
			StoryDocObservable observable;

			String path;

			String description;

			Img screenshot;

			VisitArea(StoryDocObservable observable) {
				this.observable = observable;
				path = observable.ancestorDisplayNames.stream()
						.collect(Collectors.joining(" > "));
				description = observable.description;
				if (observable.screenshot != null) {
					String base64 = Base64.getEncoder()
							.encodeToString(observable.screenshot);
					String uri = Ax.format("data:image/png;base64,%s", base64);
					screenshot = new Img(uri);
				}
			}
		}
	}
}