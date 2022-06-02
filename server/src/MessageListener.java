import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONObject;

public class MessageListener implements Runnable {

	private InputStream is;
	private DatabaseHandler databaseHandler;
	private Server server;
	
	public MessageListener(InputStream inputStream, DatabaseHandler databaseHandler, Server server) {
		this.is = inputStream;
		this.databaseHandler = databaseHandler;
		this.server = server;
	}
	
	@Override
	public void run() {
		try {
			Scanner sc = new Scanner(is);
			while (sc.hasNext() && !Thread.currentThread().isInterrupted()) {
				JSONObject response = new JSONObject(sc.nextLine());
				System.out.println("\n" + response + "\n");
				String action = response.getString("action");
				if (action.equals("do_offline")) 
					doOffline(response);
				else if (action.equals("online_users"))
					sendOnlines(response);
				else if (action.equals("my_friends"))
					sendFriends(response);
				else if (action.equals("online_friends"))
					sendOnlineFriends(response);
				else if (action.equals("add_as_friend"))
					addAsFriend(response);
				else if (action.equals("result_add_as_friend"))
					resultAddAsFriend(response);
				else if (action.equals("set_listening_port"))
					setListeningPort(response);
				else if (action.equals("get_listening_port"))
					sendListeningPort(response);
				else if (action.equals("new_message"))
					saveMessage(response);
			}
			sc.close();
		} catch (Exception e) {
			System.out.println("MessageListener : " + e.toString());
		}
	}
	
	private void saveMessage(JSONObject jo) throws Exception {
		databaseHandler.saveMessage(jo.getString("sender_username"), jo.getString("target_username"), jo.getString("message"));
	}
	
	private void sendListeningPort(JSONObject jo) throws Exception {
		JSONObject response = new JSONObject();
		boolean isOnline = false;
		int listeningPort = 0;
		int targetId = 0;
		User sender = null;
		for (User online : Singleton.onlineUsers) {
			if (online.getUserName().equals(jo.getString("target_username"))) {
				isOnline = true;
				listeningPort = online.getListeningPort();
				targetId = online.getId();
			}
			
			if (online.getId() == jo.getInt("sender_id"))
				sender = online;
		}
		response.put("action", "get_listening_port");
		response.put("is_online", isOnline);
		response.put("target_id", targetId);
		response.put("target_username", jo.getString("target_username"));
		response.put("message", jo.getString("message"));
		if (isOnline)
			response.put("listening_port", listeningPort);
		sender.sendMessage(response.toString());
	}
	
	private void setListeningPort(JSONObject jo) throws Exception {
		for (int i = 0; i < Singleton.allUsers.size(); i++) {
			User x = Singleton.allUsers.get(i);
			if (x.getId() == jo.getInt("id")) {
				Singleton.allUsers.remove(i);
				x.setListeningPort(jo.getInt("listening_port"));
				Singleton.allUsers.add(x);
				break;
			}
		}
		for (int i = 0; i < Singleton.onlineUsers.size(); i++) {
			User x = Singleton.onlineUsers.get(i);
			if (x.getId() == jo.getInt("id")) {
				Singleton.onlineUsers.remove(i);
				x.setListeningPort(jo.getInt("listening_port"));
				Singleton.onlineUsers.add(x);
				break;
			}
		}
	}
	
	private void addAsFriend(JSONObject jo) throws Exception {
		String targetUserName = jo.getString("target_username");
		for (User x : Singleton.onlineUsers) {
			if (x.getUserName().equals(targetUserName)) {
				x.sendMessage(jo.toString());
			}
		}
	}
	
	private void resultAddAsFriend(JSONObject jo) throws Exception {
		String senderUserName = jo.getString("sender_username");
		if (jo.getBoolean("result")) {
			int targetId = 0;
			for (User x : Singleton.allUsers) {
				if (x.getUserName().equals(jo.getString("target_username"))) {
					targetId = x.getId();
					break;
				}
			}
			databaseHandler.addFriend(jo.getInt("sender_id"), targetId);
			databaseHandler.addFriend(targetId, jo.getInt("sender_id"));
			User target = null;
			for (int i = 0; i < Singleton.allUsers.size(); i++) {
				User x = Singleton.allUsers.get(i);
				if (x.getUserName().equals(jo.getString("target_username"))) {
					target = Singleton.allUsers.get(i);
					break;
				}
			}
			if (target == null) return;
			for (int i = 0; i < Singleton.allUsers.size(); i++) {
				User x = Singleton.allUsers.get(i);
				if (x.getId() == jo.getInt("sender_id")) {
					Singleton.allUsers.remove(i);
					x.setFriends(x.getFriends() + target.getId() + ",");
					Singleton.allUsers.add(x);
					break;
				}
			}
			for (int i = 0; i < Singleton.onlineUsers.size(); i++) {
				User x = Singleton.onlineUsers.get(i);
				if (x.getId() == jo.getInt("sender_id")) {
					Singleton.onlineUsers.remove(i);
					x.setFriends(x.getFriends() + target.getId() + ",");
					Singleton.onlineUsers.add(x);
					break;
				}
			}
			
			for (int i = 0; i < Singleton.allUsers.size(); i++) {
				User x = Singleton.allUsers.get(i);
				if (x.getId() == targetId) {
					Singleton.allUsers.remove(i);
					x.setFriends(x.getFriends() + jo.getInt("sender_id") + ",");
					Singleton.allUsers.add(x);
					break;
				}
			}
			for (int i = 0; i < Singleton.onlineUsers.size(); i++) {
				User x = Singleton.onlineUsers.get(i);
				if (x.getId() == targetId) {
					Singleton.onlineUsers.remove(i);
					x.setFriends(x.getFriends() + jo.getInt("sender_id") + ",");
					Singleton.onlineUsers.add(x);
					break;
				}
			}
		} 
		for (User x : Singleton.onlineUsers) {
			if (x.getUserName().equals(senderUserName)) {
				x.sendMessage(jo.toString());
				break;
			}
		}
		
	}
	
	private void doOffline(JSONObject jo) throws Exception {
		for (int i = 0; i < Singleton.onlineUsers.size(); i++) {
			User x = Singleton.onlineUsers.get(i);
			if (x.getId() == jo.getInt("id")) {
				Singleton.onlineUsers.remove(i);
				break;
			}
		}
		for (User online : Singleton.onlineUsers) {
			JSONObject g = new JSONObject();
			g.put("action", "update_online_users");
			g.put("left_username", jo.getString("left_username"));
			g.put("joned_username", "");
			g.put("array", Singleton.getOnlineUsers());
			online.sendMessage(g.toString());
	    }
	}
	
	private void sendOnlines(JSONObject jo) throws Exception {
		for (int i = 0; i < Singleton.onlineUsers.size(); i++) {
			User x = Singleton.onlineUsers.get(i);
			if (x.getId() == jo.getInt("id")) {
				x.sendMessage(jo.put("action", "update_online_users").put("array", Singleton.getOnlineUsers()).put("left_username", "").put("joined_username", "").toString());
				break;
			}
		}
	}
	
	private void sendFriends(JSONObject jo) throws Exception {
		JSONArray friends = new JSONArray();
		for (int i = 0; i < Singleton.onlineUsers.size(); i++) {
			User x = Singleton.onlineUsers.get(i);
			if (x.getId() == jo.getInt("id")) {
				if (x.getFriends() == null || x.getFriends().isEmpty()) {
					x.sendMessage(jo.put("array", friends).toString());
					break;
				}
				List<String> ids = new ArrayList<>(Arrays.asList(x.getFriends().split(",")));
				for (User u : Singleton.allUsers) {
					if (ids.contains(String.valueOf(u.getId()))) {
						JSONObject o = new JSONObject();
						o.put("userName", u.getUserName());
						o.put("id", u.getId());
						friends.put(o);
					}
				}
				x.sendMessage(jo.put("array", friends).toString());
				break;
			}
		}
	}
	
	private void sendOnlineFriends(JSONObject jo) throws Exception {
		JSONArray friends = new JSONArray();
		for (int i = 0; i < Singleton.onlineUsers.size(); i++) {
			User x = Singleton.onlineUsers.get(i);
			if (x.getId() == jo.getInt("id")) {
				if (x.getFriends() == null || x.getFriends().isEmpty()) {
					x.sendMessage(jo.put("array", friends).toString());
					break;
				}
				List<String> friendIds = new ArrayList<>(Arrays.asList(x.getFriends().split(",")));
				for (User online : Singleton.onlineUsers)
					if (friendIds.contains(String.valueOf(online.getId())))
						friends.put(new JSONObject().put("id", online.getId()).put("userName", online.getUserName()));
				x.sendMessage(jo.put("array", friends).toString());
				break;
			}
		}
	}

}
