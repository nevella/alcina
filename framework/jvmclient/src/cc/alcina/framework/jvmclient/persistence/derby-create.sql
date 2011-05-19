CREATE TABLE TransformRequests (
id INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1,
 INCREMENT BY 1) ,
 transform CLOB,
 timestamp BIGINT,
 user_id BIGINT,
 clientInstance_id BIGINT,
 request_id BIGINT,
 clientInstance_auth INTEGER,
   transform_request_type varchar(50),
  transform_event_protocol varchar(50),
  tag varchar(50) ,
  PRIMARY KEY (id)
) 

