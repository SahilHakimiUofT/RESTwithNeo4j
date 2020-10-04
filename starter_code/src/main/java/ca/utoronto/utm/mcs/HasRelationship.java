package ca.utoronto.utm.mcs;

import static org.neo4j.driver.Values.parameters;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

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

public class HasRelationship implements HttpHandler {
	
	private Driver driver;
	private String uriDb;
	
	public HasRelationship() {
		uriDb = "bolt://localhost:7687";
		driver = GraphDatabase.driver(uriDb, AuthTokens.basic("neo4j", "1234"));
	}
	
	public void handle(HttpExchange r) {
		try {
			if (r.getRequestMethod().equals("GET")) {
				handleGet(r);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void handleGet(HttpExchange r) throws IOException, JSONException {
		try {
			String body = Utils.convert(r.getRequestBody());
			JSONObject deserialized = new JSONObject(body);
			String actorId = new String("");
			String movieId = new String("");
			
			if (deserialized.has("actorId")) {
				actorId = deserialized.getString("actorId");
			} else {
				JSONObject obj = new JSONObject();
				String response = obj.toString();
				r.sendResponseHeaders(400, response.length());
				OutputStream os = r.getResponseBody();
				os.write(response.getBytes());
				os.close();
				return;
			}
			
			if (deserialized.has("movieId")) {
				movieId = deserialized.getString("movieId");
			} else {
				JSONObject obj = new JSONObject();
				String response = obj.toString();
				r.sendResponseHeaders(400, response.length());
				OutputStream os = r.getResponseBody();
				os.write(response.getBytes());
				os.close();
				return;
			}
			
			boolean checkActorMovieExists = checkExists(actorId, movieId);
			//System.out.println(checkExists(actorId, movieId));
			
			if (checkActorMovieExists == false) {
				JSONObject obj = new JSONObject();
				String response = obj.toString();
				r.sendResponseHeaders(404, response.length());
				OutputStream os = r.getResponseBody();
				os.write(response.getBytes());
				os.close();
				return;
			}
			
			boolean checkRelation = checkRelationExists(actorId, movieId);
			JSONObject obj = new JSONObject();
			
			obj.put("hasRelationship", checkRelation);
			obj.put("movieId", movieId);
			obj.put("actorId", actorId);
			
			String response = obj.toString();
			r.sendResponseHeaders(200, response.length());
			OutputStream os = r.getResponseBody();
			os.write(response.getBytes());
			os.close();
		} catch (Exception e) {
			JSONObject obj = new JSONObject();
			String response = obj.toString();
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
		
		//System.out.println(actorExists);
		//System.out.println(movieExists);

		return false;
	}
	
	public boolean checkRelationExists(String actorId, String movieId) {

		try (Session session = driver.session()) {
			try (Transaction tx = session.beginTransaction()) {
				Result checker = tx.run("MATCH (:Actor{id:$x})-[r:ACTED_IN]-(:Movie{id:$y})" + "RETURN r",
						parameters("x", actorId, "y", movieId));

				if (checker.hasNext()) {
					return true;
				}
			}
		}
		return false;
	}

}
