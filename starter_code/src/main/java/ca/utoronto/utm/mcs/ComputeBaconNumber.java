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

public class ComputeBaconNumber implements HttpHandler {
	private Driver driver;
	private String uriDb;
	
	public ComputeBaconNumber() {
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
				r.sendResponseHeaders(400, response.length());
				OutputStream os = r.getResponseBody();
				os.write(response.getBytes());
				os.close();
				return;
			}
			
			String baconNum = "";
			
			if(actorId.equals("nm0000102")) {
				baconNum = "0";
				JSONObject obj = new JSONObject();
				obj.put("baconNumber", baconNum);
				String response = obj.toString();
				r.sendResponseHeaders(200, response.length());
				OutputStream os = r.getResponseBody();
				os.write(response.getBytes());
				os.close();
				return;
			}
			
			
			baconNum = findBaconNum(actorId);
			if(baconNum.equals("")) {
				JSONObject obj = new JSONObject();
				String response = obj.toString();
				r.sendResponseHeaders(404, response.length());
				OutputStream os = r.getResponseBody();
				os.write(response.getBytes());
				os.close();
				return;
				
			}
			int adjusted = Integer.parseInt(baconNum);
			adjusted = (adjusted+1)/2;
			
			String adjustedBaconNum = String.valueOf(adjusted);
			JSONObject obj = new JSONObject();
			obj.put("baconNumber", adjustedBaconNum);
			String response = obj.toString();
			r.sendResponseHeaders(200, response.length());
			OutputStream os = r.getResponseBody();
			os.write(response.getBytes());
			os.close();
			
		
			
		
			//String movieList = getMovies(actorId).toString();
		
			//System.out.println(movieList);
			//obj.put("movies","hey");
			//obj.put("movies","hi");
			//System.out.println(movieList);

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
	
	public String findBaconNum(String actorId) {
		try (Session session = driver.session()) {
			try (Transaction tx = session.beginTransaction()) {
				Result checker = tx.run("MATCH (a:Actor { id: $x})"
						+ "MATCH(b:Actor { id: 'nm0000102'}),"
						+ "p = ShortestPath((a)-[*]-(b))"
						+ "RETURN length(p)",
						parameters("x", actorId));
				if (checker.hasNext()) {
					
					//System.out.println(checker.next());
					return String.valueOf(checker.next().get("length(p)"));
				}
			}

		}		
		return "";
	}
	
	
	
}
