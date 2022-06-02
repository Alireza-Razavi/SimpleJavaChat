import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class User {

	private int id;
	private Socket socket;
	private String userName;
	private String password;
	private String friends;
	private int listeningPort;
	
	public void sendMessage(String msg) throws Exception {
		new PrintStream(getSocket().getOutputStream()).println(msg);
	}
	
	public void setSocket(Socket socket) {
		this.socket = socket;
	}
	
	public Socket getSocket() {
		return this.socket;
	}
	
	public void setUserName(String userName) {
		this.userName = userName;
	}
	
	public String getUserName() {
		return this.userName;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getPassword() {
		return this.password;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public int getId() {
		return this.id;
	}
	
	public void setListeningPort(int listeningPort) {
		this.listeningPort = listeningPort;
	}
	
	public int getListeningPort() {
		return this.listeningPort;
	}
	
	public void setFriends(String friends) {
		this.friends = friends;
	}
	
	public String getFriends() {
		return this.friends;
	}
}
