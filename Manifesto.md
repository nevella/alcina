### Terminology ###
  * **Dataobject** - An object encapsulating a row in a database table - e.g. a JPA entity bean
  * **Client app** - in this case, a GWT javascript app running in a browser
  * **DAO**, **DTO** - Data access and data transfer object - used in many application patterns (particularly J2EE), according to some schools of thought these are what the client app should see/manipulate instead of the dataobjects themselves. Decent definitions in [Wikipedia](http://en.wikipedia.org/wiki/Data_Access_Object)
  * **Object graph** -  Interconnected objects, connected via object reference
  * **Persistent model** - an object graph composed of dataobjects
  * **Detached object** - an object which does not have any reference to a database/JPA entitymanager
  * **Gruff** - Gwt 2.1 ReqUestFactory/record/sync Framework



# Introduction #

Ve hold the following premises to be self-evident (after being bitten by their contrapositives):
  * Application client <> server model synchronisation is serious business, and bears a lot of similarities to database replication
  * Operations on detached object graphs should be propogated via a delta (formal description of changes), not via objects themselves.
  * Client apps should work on (their subgraph of) the persistent model directly, not on a DTO/DAO projection of the persistent model.
  * The following problems/tasks are in fact interrelated, and if you want to address one of them in a framework, you might as well address all of them:
    * Client <> server persistent model synchronisation
    * Object graph clipping
    * Property-level client model operations (validation, permissions, default editable view)
    * Offline persistence for the client app

# Details #

**Consider the following:**

_Out of order requests_

| **Persistence via objects** | **Persistence via transforms** |
|:----------------------------|:-------------------------------|
|<font color='#D25D76'>Rosalind</font> (web client) create bookmark folder f1|<font color='#5D76D2'>Morgana</font> (web client) create bookmark folder f1|
| ...send folder object to server (POST #1)| ...send domaintransformrequest #1 to server (POST #1)|
|<font color='#2AB094'>Iago</font> (JPA server) ...yawn|<font color='#9CAEEF'>Alcina</font> (JPA server) ..yawn|
| ...still nothing. one of those portland 4g connections with 20% packet loss, right? | ...hungry|
|<font color='#D25D76'>Rosalind</font> create bookmark b1, set parent folder f1|<font color='#5D76D2'>Morgana</font> create bookmark b1, set parent folder f1 |
| ...send bookmark object (and reffed parent folder object) to server  (POST #2)| ...send domaintransformrequest #2 to server (including #1, since we haven't received acknowledgement) (POST #2)|
|<font color='#2AB094'>Iago</font> receives POST #2|<font color='#9CAEEF'>Alcina</font> receives POST #2 |
| Iago now either <br> - throws an exception (if naive, requiring the parent folder to have an id) or <br> - commits folder, bookmark child successfully <table><thead><th> ho hum, transforms. consume </th></thead><tbody>
<tr><td> receives POST #1</td><td>  receives POST #1</td></tr>
<tr><td> creates a new, totally different (childless) folder. arggghh </td><td> sees that domaintransformrequest #1 has been processed, ignores</td></tr></tbody></table>

<i>as soon as a client requests persistence of two or more asynchronous modifications to related objects, this is a case that will happen, sooner or later,  and bite you</i>

<blockquote><i>Now add object clipping...</i></blockquote>

- persistence via object remoting either has to send all objects, or run into the classic <code>EntityManager.merge()</code> "is that null or was it clipped?" trap somewhere down the line...which is fine until you start working with connected graphs of 100s of objects<br>
<br>
- Alcina just sends deltas. No problem<br>
<br>
<blockquote><i>How about a server restart?</i></blockquote>

<ul><li>Alcina - <code>ThreadlocalTransformManager.reconstituteHiliMap()</code>
</li><li>Gruff (GWT2.1 persistence framework) - well, it needs to track sync transaction commit state and probably across server restarts, if it's going to allow async modifications.<br>
</li><li>Change remoting via objects - well, either you're sending the whole graph each time or...be prepared for nondeterminism</li></ul>

<b>Conclusion:</b> Don't send changed detached object subgraphs - send a delta.<br>
<br>
<h3>DTOs, DAOs - they're so SUN</h3>
<blockquote><i><code>not Oracle (tm)</code></i></blockquote>

Back in the mists of time, when abstraction was king, and people still populated objects with hand-written SQL, there was <a href='http://java.sun.com/blueprints/corej2eepatterns/Patterns/DataAccessObject.html'>Core J2EE Patterns - Data Access Object</a>. It (and the DTO) went on to acquire a few more/differnt meanings and purposes along the way...filling new evolutionary niches...but, thing is, once your persistence domain becomes complex, you suddenly have a parallel, equally complex-and-requiring maintenance DTO structure. Now...here's what works for me:<br>
<br>
<pre><code>public class AlcinaTemplateUser extends DomainBaseVersionable implements IUser<br>
<br>
...<br>
<br>
	@ManyToMany(mappedBy = "memberUsers", targetEntity = AlcinaTemplateGroup.class)<br>
	@VisualiserInfo(displayInfo = @DisplayInfo(name = "Groups", orderingHint = 96))<br>
	@Association(implementationClass = AlcinaTemplateGroup.class, propertyName = "memberUsers")<br>
	@PropertyPermissions(read = @Permission(access = AccessLevel.ADMIN_OR_OWNER), write = @Permission(access = AccessLevel.ADMIN_OR_OWNER))<br>
	@CustomiserInfo(customiserClass = SelectorCustomiser.class)<br>
	@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)<br>
	@XmlTransient<br>
	public Set&lt;? extends IGroup&gt; getSecondaryGroups() {<br>
		return (Set&lt;AlcinaTemplateGroup&gt;) this.secondaryGroups;<br>
	}<br>
</code></pre>


<code>@VisualiserInfo? @CustomiserInfo?</code>

UI annotations on a dataobject? Traditionalists will howl and say "separation of concerns" ... but, in many simple application cases, a dataobject will have one canoncial visual/editing mode - a plain old form full of values - often for admin use only (viz Django).<br>
<br>
The <code>@PropertyPermissions</code> annotation is more interesting though - it describes all that's separating a DO from a DTO really - and guess what, we have to walk a JPA object graph before sending to the client anyway (to get rid of or instantiate lazy initializers), so simultaneous pruning of the graph based on the user's permissions is actually a more flexible way of expressing "here's data I want the client to be allowed to see" than a fixed DTO.<br>
<br>
A simple example of this might be the following:<br>
<pre><code>public class Expense {<br>
@PropertyPermissions(read = @Permission(access = AccessLevel.GROUP, rule=ACCOUNTS_SECTION), write = @Permission(access = AccessLevel.GROUP, rule=ACCOUNTS_SECTION))<br>
public String getDisapprovalReason(){<br>
<br>
}<br>
...<br>
}<br>
public class Employee {<br>
...<br>
}<br>
</code></pre>

Even if the employee created the expense, they shouldn't have any access to <code>disapprovalReason</code>:="buying an iPhone 4 for research purposes - no way! offer him/her early termination with extreme prejudice" or the like.<br>
<br>
If the code uses the DTO model, the Expense DTO either has to contain the <code>disapprovalReason</code> field (for accounts section users), or you would need two DTOs (yrrch). And of course population of the DTO still has to be permissions dependent - so ... you don't really gain any benefit of wire-bytes reduction by using a DTO.<br>
<br>
