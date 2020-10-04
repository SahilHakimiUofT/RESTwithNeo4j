package ca.utoronto.utm.mcs;

import static org.neo4j.driver.Values.parameters;

import java.io.IOException;
import java.io.OutputStream;

import org.json.*;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import static org.neo4j.driver.Values.parameters;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class AddRelationship implements HttpHandler {

	private Driver driver;
	private String uriDb;

	public AddRelationship() {
		uriDb = "bolt://localhost:7687";
		driver = GraphDatabase.driver(uriDb, AuthTokens.basic("neo4j", "1234"));
	}

	public void handle(HttpExchange r) {
		try {
			if (r.getRequestMethod().equals("PUT")) {
				handlePut(r);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void handlePut(HttpExchange r) throws IOException, JSONException {
		try {
			String body = Utils.convert(r.getRequestBody());
			JSONObject deserialized = new JSONObject(body);
			String actorId = new String("");
			String movieId = new String("");

			if (deserialized.has("actorId")) {
				actorId = deserialized.getString("actorId");
			} else {
				String response = "Improper Format";
				r.sendResponseHeaders(400, response.length());
				OutputStream os = r.getResponseBody();
				os.write(response.getBytes());
				os.close();
				return;
			}

			if (deserialized.has("movieId")) {
				movieId = deserialized.getString("movieId");
			} else {
				String response = "Improper Format";
				r.sendResponseHeaders(400, response.length());
				OutputStream os = r.getResponseBody();
				os.write(response.getBytes());
				os.close();
				return;
			}
			boolean checkActorMovieExists = checkExists(actorId, movieId);
			if (checkActorMovieExists == false) {
				String response = "Movie ID and/or Actor ID doesn't exist";
				r.sendResponseHeaders(404, response.length());
				OutputStream os = r.getResponseBody();
				os.write(response.getBytes());
				os.close();
				return;
			}
			boolean checker = checkRelationExists(actorId, movieId);

			if (checker == false) {
				String response = "Relationship already exists";
				r.sendResponseHeaders(400, response.length());
				OutputStream os = r.getResponseBody();
				os.write(response.getBytes());
				os.close();
				return;
			}
			addRelationship(actorId, movieId);
			String response = "Relation successfully added";
			r.sendResponseHeaders(200, response.length());
			OutputStream os = r.getResponseBody();
			os.write(response.getBytes());
		    os.close();
			
		} catch (Exception e) {
			String response = "Java exception";
			r.sendResponseHeaders(500, response.length());
			OutputStream os = r.getResponseBody();
			os.write(response.getBytes());
			os.close();
		}
	}

	public boolean checkExists(String actorId, String movieId) {
		int actorExists = 0;
		int movieExists = 0;
		try (Session session = driver.session()) {
			try (Transaction tx = session.beginTransaction()) {
				Result checker = tx.run("MATCH(n:Actor) WHERE EXISTS(n.id) AND n.id=$x RETURN n",
						parameters("x", actorId));
				if (checker.hasNext()) {
					actorExists = 1;
				}
			}

		}
		try (Session session = driver.session()) {
			try (Transaction tx = session.beginTransaction()) {
				Result checker = tx.run("MATCH(n:Movie) WHERE EXISTS(n.id) AND n.id=$x RETURN n",
						parameters("x", movieId));
				if (checker.hasNext()) {
					movieExists = 1;
				}
			}

		}

		if (actorExists == 1 & movieExists == 1) {
			return true;
		}

		return false;
	}

	public boolean checkRelationExists(String actorId, String movieId) {

		try (Session session = driver.session()) {
			try (Transaction tx = session.beginTransaction()) {
				Result checker = tx.run("MATCH (:Actor{id:$x})-[r:ACTED_IN]-(:Movie{id:$y})" + "RETURN r",
						parameters("x", actorId, "y", movieId));

				if (checker.hasNext()) {
					return false;
				}
			}
		}
		return true;
	}

	public void addRelationship(String actorId, String movieId) {

		try (Session session = driver.session()) {
			session.writeTransaction(tx -> tx.run(
					"MATCH (a:Actor {id:$x})," + "(m:Movie {id:$y})\n" + "MERGE (a)-[r:ACTED_IN]->(m)\n" + "RETURN r",
					parameters("x", actorId, "y", movieId)));
			session.close();

		}
	}

}
