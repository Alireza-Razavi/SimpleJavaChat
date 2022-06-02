/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import java.awt.Button;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Alireza
 */
public class MainForm extends javax.swing.JFrame {

    /**
     * Creates new form MainForm
     */
    private PrintStream printToServer;
    private Socket socketToServer;
    private Map<String, List<String>> map = new HashMap<>();
    private List<User> inChatUsers = new ArrayList<>();
    
    private void notifyListView() throws Exception {
        int index = jComboBox1.getSelectedIndex();
        JSONObject jo = new JSONObject();
        if (index == 0)
            jo.put("action", "online_users");   
        else if (index == 1)
            jo.put("action", "my_friends");
        else
            jo.put("action", "online_friends");
        jo.put("id", Singleton.id);
        printToServer.println(jo.toString());
        Scanner scanner = new Scanner(socketToServer.getInputStream());
        JSONArray arr = new JSONObject(scanner.nextLine()).getJSONArray("array");
        notifyListView(arr);
        if (index == 0) Singleton.onlines = arr;
        else if (index == 1) Singleton.friends = arr;
    }
    
    public MainForm() {
        initComponents();
    }
    
    public MainForm(PrintStream printStream, Socket socketToServer, JSONArray savedMessages) throws Exception {
        this.printToServer = printStream;
        this.socketToServer = socketToServer;
        initComponents();
        
        this.setLocationRelativeTo(null);
        this.setTitle(Singleton.userName);
        
        try {
            notifyOnlineUsers();
            List<String> users = new ArrayList<>();
            for (int i = 0; i < savedMessages.length(); i++) {
                JSONObject saved = savedMessages.getJSONObject(i);
                if (!users.contains(saved.getString("senderUserName")))
                    users.add(saved.getString("senderUserName"));
            }
            for (int i = 0; i < users.size(); i++) {
                String userName = users.get(i);
                List<String> messages = new ArrayList<>();
                for (int j = 0; j < savedMessages.length(); j++) {
                    JSONObject o = savedMessages.getJSONObject(j);
                    if (o.getString("senderUserName").equals(userName)) {
                        messages.add(o.getString("message"));
                    }
                }
                System.out.println(userName);
                map.put(userName, messages);
            }
        } catch (Exception e) { showError(e); }
       
        listView.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                try {
                    String userName = String.valueOf(listView.getSelectedValue());
                    if (userName.equals(Singleton.userName)) {
                        messageBox.setText("");
                        return;
                    }
                    updateMessageBox(userName);
                } catch (Exception ex) { System.out.println(ex.toString()); }
            }
        });
        
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                try {
                    JSONObject jo = new JSONObject();
                    jo.put("action", "do_offline");
                    jo.put("id", Singleton.id);
                    jo.put("left_username", inChatUsers);
                    printToServer.println(jo.toString());
                } catch (Exception ex) {
                    showError(ex);
                }
            }
        });
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Scanner sc = new Scanner(socketToServer.getInputStream());
                    while (sc.hasNext()) {
                    String s = sc.nextLine();
                    if (!s.startsWith("{")) continue;
                    JSONObject response = new JSONObject(s);
                    String action = response.getString("action");
                    switch (action) {
                        case "add_as_friend":
                            int result = JOptionPane.showConfirmDialog(null, "Dear " + Singleton.userName + "!\n\n" + response.getString("sender_username") + " wants to chat with you.\nDo you agree?","Chat permission", JOptionPane.YES_NO_OPTION);
                            if (result == JOptionPane.YES_OPTION) {
                                printToServer.println(response.put("action", "result_add_as_friend").put("result", true).toString());
                            } else {
                                printToServer.println(response.put("action", "result_add_as_friend").put("result", false).toString());
                            }
                            break;
                        case "result_add_as_friend":
                            boolean flag = response.getBoolean("result");
                            if (flag) {
                                showMessage("Dear " + Singleton.userName + "!\n\n" + response.getString("target_username") + " accepted your request.");
                                notifyListView();
                            } else {
                                showMessage(response.getString("target_username") + " rejected your request.");
                            }
                            break;
                        case "update_online_users":
                            Singleton.onlines = response.getJSONArray("array");
                            String leftUserName = response.getString("left_username");
                            for (int i = 0; i < inChatUsers.size(); i++) {
                                User user = inChatUsers.get(i);
                                if (user.getUserName().equals(leftUserName)) {
                                    inChatUsers.remove(i);
                                    user.setSocket(socketToServer);
                                    user.setPrintStream(printToServer);
                                    inChatUsers.add(user);
                                    break;
                                }
                            }
                            notifyOnlineUsers();
                            break;
                        case "my_friends":
                            Singleton.friends = response.getJSONArray("array");
                            notifyListView(Singleton.friends);
                            break;
                        case "online_friends":
                            notifyListView(response.getJSONArray("array"));
                            break;
                        case "get_listening_port":
                            boolean isOnline = response.getBoolean("is_online");
                            if (!isOnline) {
                                User currentUser = new User();
                                currentUser.setUserName(response.getString("target_username"));
                                currentUser.setPrintStream(printToServer);
                                inChatUsers.add(currentUser);
                                sendMessage(response.getString("message"), currentUser);
                            } else {
                                int port = response.getInt("listening_port");
                                Socket socket = new Socket("localhost", port);
                                JSONObject info = new JSONObject();
                                info.put("connector_id", Singleton.id);
                                info.put("connector_username", Singleton.userName);
                                info.put("message", response.getString("message"));
                                User currentUser = new User();
                                currentUser.setUserName(response.getString("target_username"));
                                currentUser.setPrintStream(new PrintStream(socket.getOutputStream()));
                                currentUser.setSocket(socket);
                                currentUser.setId(response.getInt("target_id"));
                                currentUser.getPrintStream().println(info.toString());
                                inChatUsers.add(currentUser);
                                for (User x : inChatUsers) {
                                    if (x.getUserName().equals(currentUser.getUserName())) {
                                        x.addMessage(currentUser.getUserName(), info.getString("message"));
                                        if (String.valueOf(listView.getSelectedValue()).equals(currentUser.getUserName()))
                                            updateMessageBox(currentUser.getUserName());
                                        break;
                                    }
                                }
                                new Thread(new FriendMessageHandler(socket.getInputStream(), currentUser)).start();
                            }
                        break;
                    }
                }   
            } catch (Exception e) { showError(e); }
        }}).start();
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int port = new Random().nextInt(65535);
                    JSONObject jo = new JSONObject();
                    jo.put("action", "set_listening_port");
                    jo.put("id", Singleton.id);
                    jo.put("listening_port", port);
                    printToServer.println(jo.toString());
                    ServerSocket server = new ServerSocket(port) {
                        protected void finalize() throws IOException {
                          this.close();
                        }
                    };
                    while (true) {
                        Socket socket = server.accept();
                        Scanner scanner = new Scanner(socket.getInputStream());
                        JSONObject info = new JSONObject(scanner.nextLine());
                        User user = new User();
                        user.setId(info.getInt("connector_id"));
                        user.setUserName(info.getString("connector_username"));
                        user.setSocket(socket);
                        user.setPrintStream(new PrintStream(socket.getOutputStream()));
                        inChatUsers.add(user);
                        for (User x : inChatUsers) {
                            if (x.getUserName().equals(user.getUserName())) {
                                x.addMessage(user.getUserName(), info.getString("message"));
                                if (String.valueOf(listView.getSelectedValue()).equals(user.getUserName()))
                                    updateMessageBox(user.getUserName());
                                break;
                            }
                        }
                        new Thread(new FriendMessageHandler(socket.getInputStream(), user)).start();
                    }
                } catch (Exception e) { showError(e); }
            }
        }).start();
        
    }
    
    class FriendMessageHandler implements Runnable {

        private User user;
        private InputStream inputStream;
        
        public FriendMessageHandler(InputStream inputStream, User user) {
            this.user = user;
            this.inputStream = inputStream;
        }
        
        @Override
        public void run() {
            try {
                Scanner scanner = new Scanner(inputStream);
                while (scanner.hasNext()) {
                    JSONObject jo = new JSONObject(scanner.nextLine());
                    if (!jo.getString("action").equals("new_message")) continue;
                    String senderUserName = jo.getString("sender_username");
                    for (User user : inChatUsers) {
                        if (user.getUserName().equals(senderUserName)) {
                            user.addMessage(senderUserName, jo.getString("message"));
                            if (String.valueOf(listView.getSelectedValue()).equals(senderUserName))
                                updateMessageBox(senderUserName);
                            break;
                        }
                    }
                }
            } catch (Exception e) { showError(e); }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        etMessage = new javax.swing.JTextField();
        btnSendMessage = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        listView = new javax.swing.JList();
        jPanel3 = new javax.swing.JPanel();
        btnAddAsFriend = new javax.swing.JButton();
        jComboBox1 = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        messageBox = new javax.swing.JTextPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setResizable(false);

        btnSendMessage.setText("Send");
        btnSendMessage.setEnabled(false);
        btnSendMessage.setName(""); // NOI18N
        btnSendMessage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSendMessageActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(etMessage, javax.swing.GroupLayout.PREFERRED_SIZE, 230, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnSendMessage, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(etMessage)
            .addComponent(btnSendMessage, javax.swing.GroupLayout.DEFAULT_SIZE, 35, Short.MAX_VALUE)
        );

        listView.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { " " };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        listView.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(listView);

        btnAddAsFriend.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        btnAddAsFriend.setText("Chat Request");
        btnAddAsFriend.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddAsFriendActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnAddAsFriend, javax.swing.GroupLayout.PREFERRED_SIZE, 157, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(33, 33, 33))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(btnAddAsFriend, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Online Users", "My Friends", "Online Friends" }));
        jComboBox1.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jComboBox1ItemStateChanged(evt);
            }
        });
        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Tahoma", 2, 11)); // NOI18N
        jLabel1.setText("You can only chat with your friends (They are previously accepted your Chat Request)");

        messageBox.setEditable(false);
        messageBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        messageBox.setFocusable(false);
        jScrollPane2.setViewportView(messageBox);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jScrollPane2))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jScrollPane1)
                            .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, 174, Short.MAX_VALUE)
                            .addComponent(jComboBox1, 0, 174, Short.MAX_VALUE))))
                .addGap(20, 20, 20))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 227, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 286, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 11, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnAddAsFriendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddAsFriendActionPerformed
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String userName = String.valueOf(listView.getSelectedValue());
                    if (userName == null || userName.isEmpty() || userName.equalsIgnoreCase("null")) {
                        showMessage("You must select a user.");
                        return;
                    }
                    
                    if (userName.equals(Singleton.userName)) {
                        showError(userName + " is your username!");
                        return;
                    }
                    if (Singleton.friends != null) {
                        boolean flag = false;
                        for (int i = 0; i < Singleton.friends.length(); i++) {
                            JSONObject friend = Singleton.friends.getJSONObject(i);
                            if (friend.getString("userName").equals(userName)) {
                                flag = true;
                                break;
                            }
                        }
                        if (flag) {
                            showError(userName + " is already in your friends list.");
                            return;
                        }
                    }
                    JSONObject jo = new JSONObject();
                    jo.put("action", "add_as_friend");
                    jo.put("sender_id", Singleton.id);
                    jo.put("sender_username", Singleton.userName);
                    jo.put("target_username", userName);
                    printToServer.println(jo.toString());
                } catch (Exception e) { showError(e); }
            }
        }).start();
    }//GEN-LAST:event_btnAddAsFriendActionPerformed

    private void jComboBox1ItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jComboBox1ItemStateChanged

    }//GEN-LAST:event_jComboBox1ItemStateChanged

    private void notifyOnlineUsers() throws Exception {
        DefaultListModel<String> model = new DefaultListModel<>();
        for (int i = 0; i < Singleton.onlines.length(); i++) {
            JSONObject user = Singleton.onlines.getJSONObject(i);
            model.addElement(user.getString("userName"));
        }
        listView.setModel(model);
    }
    
    private void notifyListView(JSONArray ja) throws Exception {
        DefaultListModel<String> model = new DefaultListModel<>();
        for (int i = 0; i < ja.length(); i++) {
            model.addElement(ja.getJSONObject(i).getString("userName"));
        }
        listView.setModel(model);
    }
    
    
    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
        // TODO add your handling code here:
        try {
            int index = jComboBox1.getSelectedIndex();
            JSONObject jo = new JSONObject();
            switch (index) {
                case 0:
                    btnSendMessage.setEnabled(false);
                    btnAddAsFriend.setEnabled(true);
                    jo.put("action", "online_users");
                    jo.put("id", Singleton.id);
                    break;
                case 1:
                    btnSendMessage.setEnabled(true);
                    btnAddAsFriend.setEnabled(false);
                    jo.put("action", "my_friends");
                    jo.put("id", Singleton.id);
                    break;
                case 2:
                    btnSendMessage.setEnabled(true);
                    btnAddAsFriend.setEnabled(false);
                    jo.put("action", "online_friends");
                    jo.put("id", Singleton.id);
                    break;
            } 
            printToServer.println(jo.toString());
        } catch (Exception e) { showError(e); }
    }//GEN-LAST:event_jComboBox1ActionPerformed

    
    private void updateMessageBox(String targetUserName) {
        StringBuilder sb = new StringBuilder("");
        List<String> saved = map.get(targetUserName);
        if (saved == null) saved = new ArrayList<>();
        for (String msg : saved) 
            sb.append(targetUserName).append(": ").append(msg).append("\n");
        User user = null;
        for (User x : inChatUsers) {
            if (x.getUserName().equals(targetUserName)) {
                user = x;
                break;
            }
        }
        if (user == null) {
            messageBox.setText(sb.toString());
            return;
        }
        for (Message msg : user.getMessages()) {
            sb.append(msg.getSenderUserName())
                    .append(": ")
                    .append(msg.getMessage())
                    .append("\n");
        }
        messageBox.setText(sb.toString());
    }
    
    private void btnSendMessageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSendMessageActionPerformed
        // TODO add your handling code here:
        try {
            
            String userName = String.valueOf(listView.getSelectedValue());
            if (userName == null || userName.isEmpty() || userName.equalsIgnoreCase("null")) {
                showMessage("You must select a friend.");
                return;
            }
            
            boolean isInChatList = false;
            User currentUser = null;
            for (User x : inChatUsers) {
                if (x.getUserName().equals(userName)) {
                    isInChatList = true;
                    currentUser = x;
                    break;
                }
            }
            String msg = etMessage.getText().toString();
            if (!isInChatList) {
                JSONObject jo = new JSONObject();
                jo.put("action", "get_listening_port");
                jo.put("target_username", userName);
                jo.put("sender_id", Singleton.id);
                jo.put("sender_username", Singleton.userName);
                jo.put("message", msg);
                printToServer.println(jo.toString());
            } else {
                sendMessage(msg, currentUser);
            }
            
        } catch (Exception e) { showError(e); }
    }//GEN-LAST:event_btnSendMessageActionPerformed

    private void sendMessage(String msg, User user) throws Exception {
        JSONObject jo = new JSONObject();
        jo.put("action", "new_message");
        jo.put("message", msg);
        jo.put("target_username", String.valueOf(listView.getSelectedValue()));
        jo.put("sender_id", Singleton.id);
        jo.put("sender_username", Singleton.userName);
        user.getPrintStream().println(jo.toString());
        user.addMessage(Singleton.userName, msg);
        etMessage.setText("");
        updateMessageBox(user.getUserName());
    }
    
    private void showError(String msg) {
        JOptionPane.showMessageDialog(null, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    private void showError(Exception e) {
        JOptionPane.showMessageDialog(null, "Unknown error: " + e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    private void showMessage(String msg) {
        JOptionPane.showMessageDialog(null, msg, "Message", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(MainForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(MainForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(MainForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MainForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new MainForm().setVisible(true);
                
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddAsFriend;
    private javax.swing.JButton btnSendMessage;
    private javax.swing.JTextField etMessage;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JList listView;
    private javax.swing.JTextPane messageBox;
    // End of variables declaration//GEN-END:variables
}
