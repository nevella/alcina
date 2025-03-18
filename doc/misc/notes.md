# alcina > misc > notes

## ui history + implicit history

- make it explicit - TraversalPlace.listSource is a case in point, although during a session the table source can be
  derived from navigation, refresh (and general stability) are better if the list source is explicit

  - ditto helpplace (fragment) etc

- devex styles

 <h2>DEVEX styles</h2>
  <table>
  <tr>
  <td>Ordinal</td>
  <td>Meaning</td>
  </tr>
  <tr>
  <td>0</td>
  <td>Noted, no logging code</td>
  </tr>
  <tr>
  <td>1</td>
  <td>Noted, logging code</td>
  </tr>
  
  <tr>
  <td>2</td>
  <td>Noted, testing fix</td>
  </tr>
  <tr>
  <td>3</td>
  <td>(Hopefully) verified fix - should not be thrown</td>
  </tr>
  <tr>
  <td>5</td>
  <td>Unknown how it got here - just catch and log for now</td>
  </tr>
  <tr>
  <td>6</td>
  <td>Cannot reproduce</td>
  </tr>
  </table>
