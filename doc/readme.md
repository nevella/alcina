# Welcome to Alcina

Named for an opera by the baroque composer Georg Friederich Händel, Alcina is an integrated set of 
software components that collectively implement a generative-process, semi-declarative client/server programming idiom 
which guides application design towards processing streams of changes.

*Generative-process* - several complex-ish application problems are tackled by Alcina with "generative" algorithms - algorithms 
that generate later stages of the algorithm during algorithm processing, rather than with up-front (imperative) specification. 
The problems are: UI modelling (Dirndl), deep website modelling (Selection Traversal), document transformation (Selection Traversal) and dependency resolution (Consort).

*Semi-declarative* code is an attempt to reconcile the virtues of declarative and imperative programming - 
the basic philosophy being "declarative for composition" - say, composing a UI from an annotated object tree 
rather than widget.add methods - "imperative for logic" - falling back on imperative code to resolve more
 complex logical decisions. This encourages granular programming in certain key areas
of the client/server software stack, and helps to minimise code replication, reuse domain 
logic and maximise (within limits) performance.



The main components of Alcina are (in no particular order):

## Enhanced GWT
Features which allow GWT - https://github.com/gwtproject/gwt - to be a reasonable development environment
for modern browsers:
*	**Websocket/pure-js dev mode** - code and debug in java with <1s refresh times
*	**Local DOM** - a GWT virtual dom, minimising jvm/browser rpc and enabling close-to-pure server-side applications
	(i.e. where client app state is maintained and html generated server side, client code mainly routes DOM events)
*	**Reflection** - implementation of class/property reflection in the GWT client, and tooling to minimise the 
	client footprint

##	Transforms
Conversion of all mutations of relational persistent objects (entities) in a graph ("domain") to a 
sequence of operations ("transforms"). This has the following benefits:
*	Low-code **relational** offline support. Although many systems (Firebase et al.) offer simple key/value offline
	browser support, relational offline support is significantly more complex and still has few implementations
	 (Apple CoreData is another)
*	Undo support	 
*	Reduces difficulties involved in EJB manipulations of highly connected graphs
*	Provides a natural way to program cancellable edits (via AdjunctTransformManager (client) (v2 in progress))
*	Centralisation of permissions domain logic
*	Provides an audit log
*	Provides a centralised application-side stream for processor domain logic ('cascades' or 'triggers'), both client and server
*	Reduces the differentiation between client and server code, since they use the same persistence mechanism (although 
	transactions are explicit server-side, implicit client-side)

##	Dirndl
"**Dir**ectedA**ndD**irected**L**ayout - a complete UI framework, runnable client- or server-side, that uses the 
expressive power of three tree structures (the view container hierarchy - *more or less* the DOM; the Java class hierarchy;
 the class/property hierarchy) to generate a UI from an arbitrary model with minimal imperative code.

Best shown by examples:
(TBD - show a collection rendering, show event handling, show an app skeleton)

## Domain Store
An in-memory, transactional, clustered, memory- and performance- optimised entity cache, where entities do not require 
hydration/dehydration, but can be accessed using standard java semantics - e.g. `Group group = User.byId(1).getPrimaryGroup();`

---

Smaller components, again in no particular order:

##	Registry

##	Publications

##	Permissions

##	Authentication

##	Jobs

##	Cluster

##	Knowns

##	Webdriver

##	Consort

##	DomainView

##	SelectionTraversal

