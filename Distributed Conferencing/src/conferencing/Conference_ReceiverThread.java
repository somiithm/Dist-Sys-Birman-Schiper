/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package conferencing;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Ashutosh
 */
public class Conference_ReceiverThread extends Thread {

    Conference_Manager conf;
    int counter;
    boolean flag;
    ArrayList<Message> messages;

    public Conference_ReceiverThread(Conference_Manager conf) {
        this.conf = conf;
        this.counter = 0;
        flag = true;
        messages = new ArrayList<Message>();
    }

    private void handleMessage(Socket sock) throws IOException, ClassNotFoundException {
//		DataOutputStream dw = new DataOutputStream(sock.getOutputStream());
        ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
        ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
        //read the message
        String message = ois.readUTF();
        System.out.println("Received :" + message);
        String name;
        switch (message.charAt(0)) {
            case 'A':
                this.counter++;
				// acknowledgement received join
                // send the list of peers to the new guy
                oos.writeObject(conf.peers);
//                                name = message.substring(1);
//                                conf.vec_clock.put(name,0);
                oos.flush();
                break;
            case 'R':
                this.counter++;
                // remove this from the map
                name = message.substring(1);
//				conf.map.remove(name);
                break;
            case 'P':
                // Add peer request
                name = message.substring(1);
                conf.peers.put(name, (Inet4Address) sock.getInetAddress());
                conf.update_peers_list();
                conf.vec_clock.put(name, 0);
                break;
            case 'E':
                // Exit peer request
                name = message.substring(1);
                conf.peers.remove(name);
                conf.update_peers_list();
                break;
            case 'M':
                //display message
                String msg = message.substring(1);
                String n = "";
                boolean f = true;
                Map<String, Integer> time_stamp = (Map<String, Integer>) ois.readObject();
                System.out.println(time_stamp);
                if (flag) {
                    flag = false;
                    conf.vec_clock = time_stamp;
                    System.out.println(conf.vec_clock);
                    conf.ui.conf_text_area.append(msg + "\n");
                    break;
                } else {

                    n = msg.substring(msg.indexOf(">") + 1, msg.lastIndexOf(":"));
                    System.out.println(n + "  " + conf.user);
                    if (n.equals(conf.user)) {
                        System.out.println("in my own recieve" + conf.vec_clock + " " + conf.vec_clock.get(n) + " " + time_stamp.get(n));
                        if ( (int)conf.vec_clock.get(n) == (int)time_stamp.get(n)) {
                            for (Map.Entry<String, Integer> entry : conf.vec_clock.entrySet()) {
                                if (!n.equals(entry.getKey())) {
                                    if (entry.getValue() >= time_stamp.get(entry.getKey())) {
                                        continue;
                                    } else {
                                        f = false;
                                        break;
                                    }
                                }
                            }
                            if (f == false) {
                                //enter in message queue
                                System.out.println("birman second condition false0");
                                Message curr = new Message(time_stamp, msg);
                                messages.add(curr);
                                break;
                            }
                        } else {
                            System.out.println("birman first condition false0");
                            Message curr = new Message(time_stamp, msg);
                            messages.add(curr);
                            break;
                        }
                    } else {
                        if (conf.vec_clock.get(n) == time_stamp.get(n) - 1) {
                            for (Map.Entry<String, Integer> entry : conf.vec_clock.entrySet()) {
                                if (!n.equals(entry.getKey())) {
                                    if (entry.getValue() >= time_stamp.get(entry.getKey())) {
                                        continue;
                                    } else {
                                        f = false;
                                        break;
                                    }
                                }
                            }
                            if (f == false) {
                                //enter in message queue
                                System.out.println("birman second condition false1");
                                Message curr = new Message(time_stamp, msg);
                                messages.add(curr);
                                break;
                            }
                        } else {
                            System.out.println("birman first condition false1");
                            Message curr = new Message(time_stamp, msg);
                            messages.add(curr);
                            break;
                        }
                    }
                }
                if (f) {
                    //check if any messages can be delivered                                
                    conf.ui.conf_text_area.append(msg + "\n");
                    System.out.println(conf.vec_clock);
                    if (!n.equals(conf.user)) {
                        conf.vec_clock.put(n, conf.vec_clock.get(n) + 1);
                    }
                    System.out.println(conf.vec_clock);
                    check_deliver();
                }

                break;
        }
    }

    void check_deliver() {
        Map<String, Integer> clock;
        String msg;
        boolean f;
        for (int i = messages.size() - 1; i >= 0; i--) {
            f = true;
            clock = messages.get(i).clock;
            msg = messages.get(i).msg;
            String n = msg.substring(msg.indexOf(">") + 1, msg.lastIndexOf(":"));
            if (conf.vec_clock.get(n) == clock.get(n) - 1) {
                for (Map.Entry<String, Integer> entry : conf.vec_clock.entrySet()) {
                    if (!n.equals(entry.getKey())) {
                        if (entry.getValue() >= clock.get(entry.getKey())) {
                            continue;
                        } else {
                            f = false;
                            break;
                        }
                    }
                }
                if (f == true) {
                    //deliver message
                    if (!n.equals(conf.user)) {
                        conf.vec_clock.put(n, conf.vec_clock.get(n) + 1);
                    }
                    conf.ui.conf_text_area.append(msg + "\n");
                    System.out.println(conf.vec_clock);
                    messages.remove(i);
                }
            }
        }
    }

    public void run() {
        try {
            // Create a server socket that listens on the port
            ServerSocket serverSocket = new ServerSocket(conf.port);
            // read messages in while 1
            while (conf.flag) {
                Socket clientSocket = serverSocket.accept();
                this.handleMessage(clientSocket);
            }
            serverSocket.close();
            while (true) {
                sleep(1000000);
            }
        } catch (IOException ex) {
            Logger.getLogger(Conference_ReceiverThread.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(Conference_ReceiverThread.class.getName()).
                    log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Conference_ReceiverThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
