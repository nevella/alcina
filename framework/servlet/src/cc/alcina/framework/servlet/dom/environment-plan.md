# environment plan

## goal

### romcom handshake

  - client: /feature-tree
  - server: authenticate (optional) (alcina-servlet or querystring auth)
  - server: serve bootstrap html
  	- create environment
  	  - if ui is 'single instance only', invalidate others
  	- inject initial rpc connection parameters
  	  - component
  	  - environment id (rename any use of 'session' to 'environment')
  	  - environment auth
  	  - retry behaviour
  - client: 
  	- post bootstrap packet to server
  - server:
  	- environment enters state 'connected' (but this may already be handled)(throw if already bootstrapped)
  - client:
  	- send heartbeat/observe mutation packet (with backoff on unreachable/404)
  	  - component
  	  - env id
  - server: (backoff)
  	- if env does not exist, if:
  	  - reply with 'refresh'
  	- else if:
  	  - single instance:
      	- reply with 'expired' (message includes 'will invalidate any other tabs viewing this component')
  	  - else:
      	- reply with 'refresh'      	