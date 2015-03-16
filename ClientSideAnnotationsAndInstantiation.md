# Introduction #

The Gwittir project provides method reflection, Alcina provides (constrained) class.forName, no-arg class instantiation and method/class annotation discovery.


# Details #

See the following framework classes:
  * `ClientReflectionGenerator` - the rebinder
  * `ClientVisible` - meta-annotation indicating the annotation should be visible to the GWT client
  * `ClientInstantiable` - indicates a class should be instantiable via (equivalent of) Class.newInstance()
  * `BeanInfo` - annotated bean info - implies `ClientInstantiable`