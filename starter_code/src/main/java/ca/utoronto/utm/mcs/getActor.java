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

public class getActor implements HttpHandler {

	private Driver driver;
	private String uriDb;

	public getActor() {
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
			/*
			 * try (Session session = driver.session()) { try (Transaction tx =
			 * session.beginTransaction()) { Result checker =
			 * tx.run("MATCH (a:Actor {id:$x})-[:ACTED_IN]-(b:Movie)" + "RETURN b.Name",
			 * parameters("x", actorId)); System.out.println(checker.next()); if
			 * (checker.hasNext()) { System.out.println(checker.next().get("b.Name")); } } }
			 */
			if (actorExists(actorId) == false) {
				JSONObject obj = new JSONObject();
				String response = obj.toString();
				r.sendResponseHeaders(404, response.length());
				OutputStream os = r.getResponseBody();
				os.write(response.getBytes());
				os.close();
				return;
			}

			String actorName = actorName(actorId);
			JSONObject obj = new JSONObject();
			obj.put("name", actorName);
			obj.put("actorId", actorId);			
			obj.put("movies",getMovies(actorId));
			
			//String movieList = getMovies(actorId).toString();
		
			//System.out.println(movieList);
			//obj.put("movies","hey");
			//obj.put("movies","hi");
			//System.out.println(movieList);
			
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

	public boolean actorExists(String actorId) {
		try (Session session = driver.session()) {
			try (Transaction tx = session.beginTransaction()) {
				Result checker = tx.run("MATCH(n:Actor) WHERE EXISTS(n.id) AND n.id=$x RETURN n",
						parameters("x", actorId));
				if (checker.hasNext()) {
					return true;
				}
			}

		}
		return false;
	}

	public String actorName(String actorId) {
		String name = "";
		try (Session session = driver.session()) {
			try (Transaction tx = session.beginTransaction()) {
				Result checker = tx.run("	MATCH (a:Actor) WHERE a.id = $x RETURN a.Name", parameters("x", actorId));
				if (checker.hasNext()) {
					name = checker.next().get("a.Name").asString();
				}
			}

		}
		return name;
	}

	public List<String> getMovies(String actorId) {
		List<String> movieArr = new ArrayList<String>();
		try (Session session = driver.session()) {
			try (Transaction tx = session.beginTransaction()) {
				Result checker = tx.run("MATCH (a:Actor {id:$x})-[:ACTED_IN]-(b:Movie)" + "RETURN b.id",
						parameters("x", actorId));

				while (checker.hasNext()) {
					movieArr.add(checker.next().get("b.id").asString());

				}
			}
		}
		return movieArr;
	}

}
