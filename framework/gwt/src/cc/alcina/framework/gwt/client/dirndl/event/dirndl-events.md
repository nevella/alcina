# Dirndl events

<!-- @javadoc-include -->

## Todo

add a simple example to start with

## Goals

The overall goal is to have the effects of UI interactions be expressed as concisely as possible, in the language of the 
application model. To achieve this: 

* Dirndl events should, where possible, occur in the 'model space' - event listeners are methods on {@link DirectedLayout.Node.model} 
  models, not Node instances or (horrors) DOM Element instances
* To move from the DOM event spsace(Click, KeyUp etc) to the model space, have a simple mechanism to translate to DOM events
* Event listeners should be registered declaratively
* Events should only bubble up to containing models, not down (or in any other direction)
* Dirndl events should fire on the GWT event pump (for consistency with the existing event *firing* model). Their dispatch/propagation
  model is what separates them from vanilla GWT events
  
## Notes

* @Directed -  reemits.length must equal zero or receives length. a model either handles received events or reemits (document in annotation
 doc in this .md)
 
* Doc structure
  * Simple example (hello world - click)
  * Slightly more complex - a 1-2-3 enum choices

* Explain goals against examples

* Advanced - the event dispatch sequence

### Event bus use

The GWT SimpleEventBus is a reentrant, not sequential event bus, so it's OK to make calls to dispatchEvent during propagation (since 
the propagation dispatch will be executed before any other event). If it were a sequential bus (like say the DOM event bus), 
it'd be necessary to handle propagation differently (i.e. not via dispatch), which means the ModelEvent.dispatch handling would 
have to either dispatch via the event bus if *not* in an event dispatch frame, or propagate without dispatch if in an event dispatch frame. 
Otherwise there'd need to be a callback to handle further propagation post-dispatch

## Example - a click event

**TODO** - @include

```
@Directed(
	tag = "div",
	receives = ModelEvents.Selected.class,
	bindings = @Binding(
		type = Type.PROPERTY,
		to = "style",
		literal = "background: green; padding: 1em; margin: 2em; font-size:3em;text-align:center"))
public static class Container extends Model
		implements ModelEvents.Selected.Handler {
	private final TagTextModel string = new TagTextModel("div",
			"some text");


	@Directed(
		receives = DomEvents.Click.class,
		reemits = ModelEvents.Selected.class)
	public TagTextModel getString() {
		return this.string;
	}

	@Override
	public void onSelected(Selected event) {
		ClientNotifications.get()
				.log(event.getModel().getClass().getName());
	}
}
```

**TODO** - 
The order of code events caused by a DOM click event on the DOM element corresponding to the TagTextModel instance is:

* <div> <-- DOM ClickEvent (DOM VM)
* DOMImpl.com.google.gwt.user.client.impl.DOMImplStandard.dispatchEvent(Event) [event is the native (DOM) event]
(event is received by the main GWT DOM event dispatch handler, dispatched)
* DomEvent.fireNativeEvent(NativeEvent, HasHandlers, Element) [event is a GWT Click event, wrapping the native event]
(event is received by the per-element (Widget) GWT event registration, wrapped in a GWT-friendly event, fired on the GWT bus)
* com.google.web.bindery.event.shared.SimpleEventBus.fireEvent(Event<?>)...ClickEvent is fired on the DomEvent.Click.Handler.fireEvent() method
* (fill in)
* Container.onClick


### change

* --don't-- use event bus (because listener registration and propagation mechs are different) (note 

