/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Alireza
 */
public class User {

    private int id;
    private String userName;
    private Socket socket;
    private PrintStream printStream;
    private List<Message> messages = new ArrayList<>();
    
    public void setPrintStream(PrintStream printStream) {
        this.printStream = printStream;
    }

    public PrintStream getPrintStream() {
        return this.printStream;
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

    public void setId(int id) {
            this.id = id;
    }

    public int getId() {
            return this.id;
    }
    
    public void addMessage(String senderUserName, String msg) {
        Message message = new Message();
        message.setMessage(msg);
        message.setSenderUserName(senderUserName);
        this.messages.add(message);
    }
    
    public List<Message> getMessages() {
        return this.messages;
    }
}
