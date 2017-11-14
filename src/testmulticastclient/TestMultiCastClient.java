/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package testmulticastclient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ferhan
 */
public class TestMultiCastClient {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws SocketException, UnknownHostException, IOException {
        // TODO code application logic here
        ConnectionManager connectionManager = new ConnectionManager();
        connectionManager.start();
        // Now load the UI and other stuff
        // The UI will check the connectionManager.connected and allow the user to send messages
        // if connected is true. Otherwise, it will hold untill connected is established again
        ClientHandler mainHandler = new ClientHandler(connectionManager);
        mainHandler.start();
        java.awt.EventQueue.invokeLater(new Runnable() 
        {
            public void run() {
                new ClientGUI(connectionManager).setVisible(true);
            }
        });
    }
    
}
class ConnectionManager extends Thread
{
    public boolean connected;
    public Socket connectionSocket;
    public ServerSocket serverSocket;
    private void startServer() throws IOException
    {
        serverSocket = new ServerSocket(10001);
        System.out.println("Waiting for server to respond on port " 
                    + serverSocket.getLocalPort() + "...");
    }
    private void connect()
    {
        try
        {
            DatagramSocket socket;
            InetAddress group;
            byte[] buf;
            socket = new DatagramSocket();
            group = InetAddress.getByName("230.0.0.0");
            String request = "I am client please add me to the chat group if you are the server."
                    + " Listenin at port 10001";
            buf = request.getBytes();

            DatagramPacket packet = new DatagramPacket(buf, buf.length, group, 4446);
            socket.send(packet);
            System.out.println("A multicast request sent to the network!");
            socket.close();
        }
        catch (SocketException ex) 
        {
            Logger.getLogger(ConnectionManager.class.getName()).log(Level.SEVERE, null, ex);
        } 
        catch (UnknownHostException ex) 
        {
            Logger.getLogger(ConnectionManager.class.getName()).log(Level.SEVERE, null, ex);
        } 
        catch (IOException ex) 
        {
            Logger.getLogger(ConnectionManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public ConnectionManager() throws IOException 
    {
        startServer();
        ServerWaiter w = new ServerWaiter();
        w.start();
        connected=false ;
    }
    @Override
    public void run()
    {
        while(true)
        {
            while(!connected)
            {
                connect();
                try {
                    System.out.println("Going for sleep");
                    sleep(10000); // wait for 10 seconds
                    System.out.println("Woke from sleep");
                } catch (InterruptedException ex) {
                    Logger.getLogger(ConnectionManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    class ServerWaiter extends Thread
    {
        ServerWaiter()
        {}
        @Override
        public void run()
        {
            while(true)
            {
                try 
                {
                    while(!connected)
                    {
                        connectionSocket = serverSocket.accept();
                        System.out.println("Connection from server accepted");
                        connected=true;
                    }
                }
                catch (IOException ex) 
                {
                    Logger.getLogger(ConnectionManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
class ClientHandler extends Thread // this will manage both UI and interaction between this client
        // (currently server and target client (Actual server))
{
    ConnectionManager connectionMan;
    ClientHandler(ConnectionManager connectionManVal)
    {
        connectionMan= connectionManVal;
    }
    @Override
    public void run()
    {
        while(!connectionMan.connected)
        {
            //System.out.println("Running and "+connectionMan.connected);
        }
        System.out.println("Running and "+connectionMan.connected);
        try 
        {
            System.out.println("Just connected to " + connectionMan.connectionSocket.getRemoteSocketAddress());
            Scanner inputScanner= new Scanner(System.in);
            DataInputStream in = new DataInputStream(connectionMan.connectionSocket.getInputStream());
            DataOutputStream out= new DataOutputStream(connectionMan.connectionSocket.getOutputStream());
            while(true) 
            {
                try 
                {
                    String clientResponse= in.readUTF();
                    System.out.println("Server says : "+clientResponse);
                    if(clientResponse.toLowerCase().contains("bye"))
                    {
                        connectionMan.connectionSocket.close();
                        break;
                    }
                    else
                    {
                        System.out.print("Please enter a reply for the server : ");
                        String serverResponse= inputScanner.nextLine();
                        out.writeUTF(serverResponse);
                    }
                } 
                catch (SocketTimeoutException s) 
                {
                   System.out.println("Socket timed out!");
                   break;
                }
            }
        } 
        catch (IOException ex) 
        {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}