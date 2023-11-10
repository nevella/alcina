package cc.alcina.framework.servlet.component.traversal;

import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

@Directed(tag = "layers")
class SelectionLayers extends Model.Fields {
	@Directed
	Heading header = new Heading("Selection layers");

	@Directed
	List<LayerSelections> layers;

	Page page;

	SelectionTraversal traversal;

	SelectionLayers(Page page) {
		this.page = page;
		render();
	}

	public void setLayers(List<LayerSelections> layers) {
		set("layers", this.layers, layers, () -> this.layers = layers);
	}

	void render() {
		if (page.history == null) {
			return;
		}
		traversal = page.history.traversal;
		DepthFirstTraversal<Layer> renderTraversal = new DepthFirstTraversal<Layer>(
				traversal.getRootLayer(), Layer::getChildren, false);
		List<LayerSelections> layers = renderTraversal.stream()
				.map(LayerSelections::new).collect(Collectors.toList());
		setLayers(layers);
	}

	public class LayerSelections extends Model.All {
		private Layer layer;

		String key;

		String outputs;

		public LayerSelections(Layer layer) {
			this.layer = layer;
			FormatBuilder keyBuilder = new FormatBuilder();
			keyBuilder.indent(layer.depth());
			keyBuilder.append(layer.getName());
			key = keyBuilder.toString();
			outputs = computeOutputs();
		}

		String computeOutputs() {
			int size = traversal.getSelections(layer).size();
			if (size != 0) {
				return String.valueOf(size);
			}
			Layer firstLeaf = layer.firstLeaf();
			int firstLeafSize = traversal.getSelections(firstLeaf).size();
			if (firstLeafSize != 0) {
				return "-";
			} else {
				return "0";
			}
		}
	}
}
