import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.json.JSONArray;
import org.json.JSONObject;

public class DatabaseHandler {
	
	private Connection database;
	
	public DatabaseHandler() throws Exception {
		String url = "jdbc:sqlite:" + Singleton.DATABASE_PATH;
		Class.forName("org.sqlite.JDBC");
	    database = DriverManager.getConnection(url);
	}
	
	public boolean doRegister(String userName, String password, String action) throws Exception {
		ResultSet set = database.createStatement().executeQuery("SELECT count(*) FROM users WHERE userName = '" + userName + "'");
		int size = 0;
		if (set.next()) size = set.getInt("count(*)");
		if (size != 0) return false;
		
		  String sql = "INSERT INTO users(userName,password,friends) VALUES ('" + userName + "','" + password + "','')";
		  Statement statement = database.createStatement();
          statement.executeUpdate(sql);
          return true;
	}
	
	public void deleteSavedMessages(String targetUserName) throws Exception {
		database.createStatement().executeUpdate("DELETE FROM saved WHERE targetUserName = '" + targetUserName + "'");
	}
	
	public void saveMessage(String senderUN, String targetUN, String msg) throws Exception {
		String sql = "INSERT INTO saved(senderUserName,targetUserName,message) VALUES ('" + senderUN + "','" + targetUN + "','" + msg + "')";
		Statement statement = database.createStatement();
        statement.executeUpdate(sql);
	}
	  
	public boolean doLogin(String userName, String password, String action) throws Exception {
	  Statement statement = database.createStatement();
	  ResultSet set = statement.executeQuery("SELECT * FROM users");
	  while (set.next()) {
		  String u = set.getString("userName");
		  String p = set.getString("password");
		  if (userName.equals(u) && password.equals(p)) {
			  return true;
		  }
	  }
	  return false;
	}
	
	public List<User> getAllUsers() throws Exception {
		List<User> users = new ArrayList<>();
		ResultSet set = database.createStatement().executeQuery("SELECT * FROM users ORDER BY id");
		while (set.next()) {
			User user = new User();
			user.setId(set.getInt("id"));
			user.setUserName(set.getString("userName"));
			user.setPassword(set.getString("password"));
			user.setFriends(set.getString("friends"));
			users.add(user);
		}
		return users;
	}
	
	public User getUser(int id) throws Exception {
		User user = new User();
		ResultSet set = database.createStatement().executeQuery("SELECT * FROM users WHERE id = '" + id + "'");
		if (set.next()) {
			user.setId(set.getInt("id"));
			user.setUserName(set.getString("userName"));
			user.setPassword(set.getString("password"));
			user.setFriends(set.getString("friends"));
		}
		return user;
	}
	
	public User getUser(String userName) throws Exception {
		User user = new User();
		ResultSet set = database.createStatement().executeQuery("SELECT * FROM users WHERE userName = '" + userName + "'");
		if (set.next()) {
			user.setId(set.getInt("id"));
			user.setUserName(set.getString("userName"));
			user.setPassword(set.getString("password"));
			user.setFriends(set.getString("friends"));
		}
		return user;
	}
	
	public JSONArray getSavedMessaged(String targetUserName) throws Exception {
		ResultSet set = database.createStatement().executeQuery("SELECT * FROM saved WHERE targetUserName = '" + targetUserName + "'");
		JSONArray arr = new JSONArray();
		while (set.next()) {
			JSONObject message = new JSONObject();
			message.put("message", set.getString("message"));
			message.put("senderUserName", set.getString("senderUserName"));
			arr.put(message);
		}
		return arr;
	}
	
	public void addFriend(int forId, int targetId) throws Exception {
		Statement statement = database.createStatement();
		String sql = "SELECT * FROM users WHERE id = '" + forId + "'";
		ResultSet set = statement.executeQuery(sql);
		if (set.next()) {
			String friends = set.getString("friends");
			friends = friends + targetId + ",";
			sql = "UPDATE users SET friends = '" + friends + "' WHERE id = '" + forId + "'";
			statement.executeUpdate(sql);
		}
	}
}
