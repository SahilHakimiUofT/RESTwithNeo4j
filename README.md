# RESTwithNeo4j
This project allows a client to interact with a Neo4j database, with GET and PUT requests. The database contains movies and actors,
there are server contexts addActor,addMovie,addRelationship that handle PUT requests that allow the client to add nodes and relationships between them. Additionally there are
hasRelationship, getActor, getMovie, computeBaconPath, computeBaconNumber contexts that handle GET requests that allow the client to retrieve nodes and information about relationships between them, as well as the "Bacon Path" or "Bacon Number" that gives them the shortest path/ shortest path length between an actor and Kevin Bacon. 
