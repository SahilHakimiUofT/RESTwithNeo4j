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

public class GetMovie implements HttpHandler {
	
	private Driver driver;
	private String uriDb;
	
	public GetMovie() {
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
			String movieId = new String("");
			
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
			
			if (movieExists(movieId) == false) {
				JSONObject obj = new JSONObject();
				String response = obj.toString();
				r.sendResponseHeaders(404, response.length());
				OutputStream os = r.getResponseBody();
				os.write(response.getBytes());
				os.close();
				return;
			}
			
			String movieName = movieName(movieId);
			JSONObject obj = new JSONObject();
			
			obj.put("actors",getActors(movieId));
			obj.put("name", movieName);
			obj.put("movieId", movieId);
			
			String response = obj.toString();
			r.sendResponseHeaders(200, response.length());
			OutputStream os = r.getResponseBody();
			os.write(response.getBytes());
			os.close();
		}catch (Exception e) {
		//	e.printStackTrace();
			JSONObject obj = new JSONObject();
			String response = obj.toString();
			r.sendResponseHeaders(500, response.length());
			OutputStream os = r.getResponseBody();
			os.write(response.getBytes());
			os.close();
		}
	}
	
	public boolean movieExists(String movieId) {
		try (Session session = driver.session()) {
			try (Transaction tx = session.beginTransaction()) {
				Result checker = tx.run("MATCH(n:Movie) WHERE EXISTS(n.id) AND n.id=$x RETURN n",
						parameters("x", movieId));
				if (checker.hasNext()) {
					return true;
				}
			}
		}
		return false;
	}
	
	public String movieName(String movieId) {
		String name = "";
		try (Session session = driver.session()) {
			try (Transaction tx = session.beginTransaction()) {
				Result checker = tx.run("MATCH (a:Movie) WHERE a.id = $x RETURN a.Name", parameters("x", movieId));
				if (checker.hasNext()) {
					name = checker.next().get("a.Name").asString();
				}
			}
		}
		return name;
	}
	
	public List<String> getActors(String movieId) {
		List<String> ActorM = new ArrayList<String>();
		try (Session session = driver.session()) {
			try (Transaction tx = session.beginTransaction()) {
				Result checker = tx.run("MATCH (a:Actor)-[:ACTED_IN]-(b:Movie {id:$x})" + "RETURN a.id",
						parameters("x", movieId));

				while (checker.hasNext()) {
					ActorM.add(checker.next().get("a.id").asString());
				}
			}
		}
		return ActorM;
	}
}
