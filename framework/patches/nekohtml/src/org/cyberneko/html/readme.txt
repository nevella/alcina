@Notes
These mods are to ensure that the dom tree returned from the neko 
parser is as close as possible to the client (browser) DOM.

Naturally, this is different from browser to browser - but if we err on the side of strictness on the server,
we should be ok (e.g. UL closes P, A closes A) etc.

Main test browser: Chrome.
