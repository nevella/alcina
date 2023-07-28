package cc.alcina.framework.gwt.client.dirndl.layout.doc.example;

import com.google.gwt.user.client.Window;

import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Add;
import cc.alcina.framework.gwt.client.dirndl.layout.doc.example.CroissanteriaEvents.BiteSample;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/**
 * <p>
 * How the example below works:
 * 
 * <p>
 * {@code Link}...withModelEvent(ModelEvents.Add.class) instructs the Link
 * instance to translate a dom ClickEvent on its rendered DOM element to a
 * ModelEvent.Add event, which is then propagated up the stack. From the
 * application logic point of view, this is defining what a click on that Link
 * <i>means</i> (add) without needing to define there how to handle that 'add'.
 * 
 * <p>
 * At any point higher in the model structure (in this case, we're using the
 * containing {@link Croissanteria} instance, the 'add' event can be handled by
 * registering it as a receiver ( adding the event class to
 * the @Directed.receives array) and implementing the event's Handler interface
 * 
 * <p>
 * By default, the onXxx event handlers consume Model events (not DOM events) -
 * to continue bubbling, call event.bubble()
 *
 */
@Directed(
	receives = { ModelEvents.Add.class, CroissanteriaEvents.BiteSample.class })
public class Croissanteria extends Model.Fields implements
		CroissanteriaEvents.BiteSample.Handler, ModelEvents.Add.Handler {
	@Directed
	String orderDemo = "Your order is 1 chocolate croissant and an espresso";

	Link addAnotherCroissant = new Link().withText("Add another croissant")
			.withModelEvent(ModelEvents.Add.class);

	Link biteASample = new Link().withText("Bite into a free sample")
			.withModelEvent(CroissanteriaEvents.BiteSample.class);

	@Override
	public void onAdd(Add event) {
		Window.alert("adding another croissant...");
	}

	@Override
	public void onTrySample(BiteSample event) {
		Window.alert("trying a free sample...");
	}
}
