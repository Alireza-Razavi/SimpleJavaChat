/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

/**
 *
 * @author Alireza
 */
public class Message {
    
    private String senderUserName;
    private String message;
    
    public void setSenderUserName(String userName) {
        this.senderUserName = userName;
    }
    
    public String getSenderUserName() {
        return this.senderUserName;
    }
    
    public void setMessage(String msg) {
        this.message = msg;
    }
    
    public String getMessage() {
        return this.message;
    }
}
