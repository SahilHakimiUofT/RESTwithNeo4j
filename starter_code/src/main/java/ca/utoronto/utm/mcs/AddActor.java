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

public class AddActor implements HttpHandler {

	private Driver driver;
	private String uriDb;

	public AddActor() {
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
			String name = new String("");
			String actorId = new String("");

			if (deserialized.has("name")) {
				name = deserialized.getString("name");
			} else {
				String response = "Improper Format";
				r.sendResponseHeaders(400, response.length());
				OutputStream os = r.getResponseBody();
				os.write(response.getBytes());
				os.close();
				return;
			}

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
			if((name.strip()).compareTo("")==0 ||(actorId.strip()).compareTo("")==0) {
				String response = "Improper Format";
				r.sendResponseHeaders(400, response.length());
				OutputStream os = r.getResponseBody();
				os.write(response.getBytes());
				os.close();
				return;		
			}
			boolean checker = addActor(name, actorId);

			if (checker == false) {
				String response = "Actor ID already exists";
				r.sendResponseHeaders(400, response.length());
				OutputStream os = r.getResponseBody();
				os.write(response.getBytes());
				os.close();
				return;
			}
			String response = "Successfull Add";
			r.sendResponseHeaders(200,response.length());
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

	public boolean addActor(String name, String actorId) {
		try (Session session = driver.session()) {
			try (Transaction tx = session.beginTransaction()) {
				Result checker = tx.run("MATCH(n:Actor) WHERE EXISTS(n.id) AND n.id=$x RETURN n",
						parameters("x", actorId));
				if (checker.hasNext()) {
					return false;
				}
			}

		}

		try (Session session = driver.session()) {
			session.writeTransaction(
					tx -> tx.run("MERGE (a:Actor {Name:$x,id:$y})", parameters("x", name, "y", actorId)));
			session.close();
			return true;
		}
	}
}
