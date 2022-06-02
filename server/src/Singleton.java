import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class Singleton {
	
	public static List<User> onlineUsers = new ArrayList<>();
	public static List<User> allUsers = new ArrayList<>();
	public static String DATABASE_PATH;

	public static JSONArray getOnlineUsers() throws Exception {
		  JSONArray array = new JSONArray();
		  for (int i = 0; i < onlineUsers.size(); i++) {
			  User x = onlineUsers.get(i);
			  JSONObject jo = new JSONObject();
			  jo.put("userName", x.getUserName());
			  jo.put("id", x.getId());
			  array.put(jo);
		  }
		  return array;
	}
}
