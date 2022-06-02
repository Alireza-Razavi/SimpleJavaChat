## Directories
-    **release**<br />
The release version of the project (no need to build project from source)
-    **client**<br />
The source code for client app written in NetBeans IDE using the java swing framework.
-    **server**<br />
The source code for server app written in Eclipse IDE without any GUI and should be run using the command line ```<path-to-your-java> -jar server.jar``` in a command-line interface (CLI)<br />

## Project Architecture (Diagram)
![Diagram](https://github.com/Alireza-Razavi/SimpleJavaChat/blob/main/diagram.png)<br /><br />
-    **The project uses TCP as transfer layer (third layer in TCP/IP)**
-    **JSON is used as data interchange format in this project (Application layer in TCP/IP)**
-    **The connection between clients and server is clear but in additional each client randomly chooses a listening port for incoming messages from other clients.<br />When a client wants to establish a connection with another one, it requests the listening port of the target client from server and then connects to it. (Clients are directly chatting with each other and the server doesn't route the messages, It is possible until clients have static IP addresses and also they are all in one network.)**
## Demonstration
![Demo](https://github.com/Alireza-Razavi/SimpleJavaChat/blob/main/demo.gif)
