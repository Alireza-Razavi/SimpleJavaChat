import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;

public class Server {
	  private int port;
  	  private ServerSocket server;
  	  private DatabaseHandler databaseHandler;

	  public static void main(String[] args) {
		  try {
			  File jarDir = new File(ClassLoader.getSystemClassLoader().getResource(".").getPath());
			  Singleton.DATABASE_PATH = jarDir.getAbsolutePath() + "\\chat.db";
			  Singleton.DATABASE_PATH = Singleton.DATABASE_PATH.replaceAll("\\\\", "/");
			  new Server(12345).start();
		  } catch (Exception e) {
			  System.out.println("An error occured: " + e.toString());
		  }
	  }
	
	  public Server(int port) throws Exception {
		  this.port = port;
		  this.databaseHandler = new DatabaseHandler();
	  }
	  
	  public void start() throws Exception {
		  // Listen to 12345 port
		  server = new ServerSocket(port) {
		      protected void finalize() throws IOException {
		        this.close();
		      }
		  };
		  // This is a test comment
		  System.out.println("Port " + port + " is now open.");
		  
		  while (true) {
			  Socket socket = server.accept();
			  String info = new Scanner(socket.getInputStream()).nextLine();
			  JSONObject authentication = new JSONObject(info);
			  String userName = authentication.getString("userName");
			  String password = authentication.getString("password");
			  String action = authentication.getString("action");
			  
			  
			  boolean result = false;
			  JSONObject response = null;
			  if (action.equalsIgnoreCase("register")) { 
				  result = databaseHandler.doRegister(userName, password, action);
				  if (result)
					  response = getOutJson(result, "Registered successfully!", "register");
				  else 
					  response = getOutJson(result, "This username is already exists!", "register");
			  } else if (action.equalsIgnoreCase("login")) {
				  result = databaseHandler.doLogin(userName, password, action);
				  if (result)
					  response = getOutJson(result, "Welcome dear " + userName + "!", "login");
				  else 
					  response = getOutJson(result, "Invalid information!", "login");
			  }
				
			  fetchAllUsers();
			  
			  User user = null;
			  if (result) {
				  user = databaseHandler.getUser(userName);
				  user.setSocket(socket);
				  Singleton.onlineUsers.add(user);
				  response.put("savedMessages", databaseHandler.getSavedMessaged(user.getUserName())).put("id", user.getId()).put("array", Singleton.getOnlineUsers());
				  user.sendMessage(response.toString());
				  databaseHandler.deleteSavedMessages(user.getUserName());
				  new Thread(new MessageListener(socket.getInputStream(), databaseHandler, this)).start();
				  sendOnlineUsers(user.getUserName());
			  } else {
				  user = new User();
				  user.setSocket(socket);
				  user.sendMessage(response.toString());
			  }
		  }
	  }
	  
	  public void sendOnlineUsers(String joined) throws Exception { 
		  for (User online : Singleton.onlineUsers) {
			  JSONObject jo = new JSONObject();
			  jo.put("action", "update_online_users");
			  jo.put("array", Singleton.getOnlineUsers());
			  jo.put("left_username", "");
			  jo.put("joined_username", joined);
			  online.sendMessage(jo.toString());
		  }
	  }
	  
	  public void fetchAllUsers() throws Exception {
		  Singleton.allUsers.clear();
		  Singleton.allUsers.addAll(databaseHandler.getAllUsers());
	  }
	  
	  public void notifyOnlineUsers() {
		  for (int i = 0; i < Singleton.onlineUsers.size(); i++) {
			  User online = Singleton.onlineUsers.get(i);
			  Singleton.onlineUsers.remove(i);
			  for (User user : Singleton.allUsers) {
				  if (user.getId() == online.getId()) {
					  user.setSocket(online.getSocket());
					  Singleton.onlineUsers.add(user);
				  }
			  }
		  }
	  }
	  
	  public JSONObject getOutJson(boolean success, String msg, String action) throws Exception {
		  return new JSONObject().put("success", success).put("msg", msg).put("action", action);
	  }
	  
}
