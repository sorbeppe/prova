package iit.cnr.wifidirectsocial.Model;


import android.net.wifi.p2p.WifiP2pDevice;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import iit.cnr.wifidirectsocial.Model.ReliableMessageNotSentException;
import iit.cnr.wifidirectsocial.sqlite.helper.DatabaseHelper;
import iit.cnr.wifidirectsocial.sqlite.model.Group;
import iit.cnr.wifidirectsocial.sqlite.model.Peer;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import android.content.Context;
/**
 * Created by sorbeppe84 on 14/10/2015.
 */
public class TransmissionManager {

    private final static String tag="TransmissionManager";


    private static final int RELIABLE_QUEUE_CAPACITY = 50;
    private static final int RELIABLE_PORT = 6250;
    private InetAddress localIP;
    private LinkedBlockingQueue<OutgoingReliableMsgInfo> reliableQueue;
    private ReliableSender reliableSender;
    private ReliableReceiver reliableReceiver;
    private boolean queuesAreAccepting;
    private DatabaseHelper databaseHelper;
    private Context contextTransmissionManager;
    private Group group;
    private Hashtable<String,ClientInfo> memberTable;

    public TransmissionManager(Context c){

        reliableQueue = new LinkedBlockingQueue<OutgoingReliableMsgInfo>(RELIABLE_QUEUE_CAPACITY);
        reliableSender = null;
        reliableReceiver = null;
        queuesAreAccepting = false;
        localIP=getLocalIpAddress();
        contextTransmissionManager = c;
        databaseHelper = new DatabaseHelper(contextTransmissionManager);
        group= null;
    }


    public synchronized void start(){
        queuesAreAccepting = true;
        reliableSender = new ReliableSender();
        reliableReceiver = new ReliableReceiver();;
        reliableSender.start();
        reliableReceiver.start();

    }


    public synchronized void stop(){
        queuesAreAccepting = false;
        reliableQueue.clear();
        if(reliableReceiver != null)
            reliableReceiver.interrupt();
        if (reliableSender!=null)
            reliableSender.interrupt();

    }


    public synchronized void sendReliable(Object obj, InetAddress dest_ip) throws ReliableMessageNotSentException {
        try {
            if(queuesAreAccepting)
                reliableQueue.put(new OutgoingReliableMsgInfo(obj, dest_ip, RELIABLE_PORT));
            else throw new ReliableMessageNotSentException(obj, dest_ip);
        } catch (InterruptedException e) {
            Log.e(tag, Log.getStackTraceString(e));
        }
    }

    private class ReliableSender extends Thread{
        Socket socket;

        ReliableSender(){
        }

        @Override
        public void interrupt() {
            super.interrupt();
            closeSocket();
        }

        @Override
        public void run() {
            socket = null;
            OutgoingReliableMsgInfo message = null;
            while (!Thread.currentThread().isInterrupted()){
                try{
                    message = reliableQueue.take();
                    //Log.e(tag, "ANDREA TransmissionManager Sending on socket: "+message.getObj().toString());
                    socket = new Socket(message.getDestIp(), RELIABLE_PORT);
                    ObjectOutputStream outToServer = new ObjectOutputStream(socket.getOutputStream());
                    outToServer.writeObject(message.getObj());
                    outToServer.flush();
                }
                catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    //Log.e(tag, Log.getStackTraceString(ie));
                } catch (SocketException se) {
                    Log.e(tag, Log.getStackTraceString(se));
                } catch (IOException ioe) {
                    Log.e(tag, Log.getStackTraceString(ioe));
                }
                finally{
                    closeSocket();
                }
            }
        }

        public void closeSocket() {
            if (socket!=null) {
                try {
                    socket.close();
                } catch (IOException ioe) {
                    Log.e(tag, Log.getStackTraceString(ioe));
                }
            }
        }
    }

    private class ReliableReceiver extends Thread{

        Group group;
        ReliableReceiverService relRecService;
        ServerSocket socket;

        ReliableReceiver(){
        }

        public void setGroup (Group group) {

            this.group = group;

        }


        @Override
        public void interrupt() {
            closeSocket();
            super.interrupt();
        }

        @Override
        public void run() {

            memberTable.clear();
            socket = null;
            try{
                socket = new ServerSocket(RELIABLE_PORT);
                while(!Thread.currentThread().isInterrupted()) {
                    Socket connectionSocket = socket.accept();
                    relRecService = new ReliableReceiverService(connectionSocket);
                    relRecService.start();
                }
                socket.close();
            }
            catch (IOException ioe) {
                //Log.e(tag, Log.getStackTraceString(ioe));
            }
        }

        private void closeSocket() {
            if (socket!=null)
                try {
                    socket.close();
                } catch (IOException e) {
                }
        }

        private class ReliableReceiverService extends Thread{

            private Socket connectionSocket;

            ReliableReceiverService(Socket connectionSocket){
                this.connectionSocket = connectionSocket;
            }

            @Override
            public void run(){


                ObjectInputStream inFromClient = null;
                try{
                    inFromClient = new ObjectInputStream(connectionSocket.getInputStream());
                    //Object message = inFromClient.readObject();
                    MembershipMessage membershipMessage = (MembershipMessage)inFromClient.readObject();

                    //rimuovere WAKE_UP test?
                    if (!connectionSocket.getInetAddress().getHostAddress().equals(localIP.getHostAddress())) {
                        Log.i(tag, "Reliable Receiver got a packet: " + membershipMessage.toString() + " from " + connectionSocket.getInetAddress());

                        // Recupero dal socket l'indirizzo Ip di colui che ha mandato i dati
                        InetAddress inetAddressSocket = connectionSocket.getInetAddress();

                        // sto ricevendo un membershipMessage con campo type = 1 contenente un oggetto di tipo ClientInfo
                        // da aggiungere alla lista dei client (Sono GO)
                        if (membershipMessage.getType() == 1) {


                            //Recupera le informazioni sul client che mi si e' connesso (L'indirizzo ip lo recupero dal socket)
                            ClientInfo clientInfo = (ClientInfo)membershipMessage.getPayload();
                            clientInfo.setInetAddress(inetAddressSocket);
                            Log.i(tag, "Ho ricevuto un nuovo client con mac_address: " + clientInfo.getMacAddress() + " from " + connectionSocket.getInetAddress());
                            List<Group> tableGroup = databaseHelper.getAllOccurencesOfTheSameGroup(group.getMacAddressGo(),group.getGroupName());


                            //Manda lo storico del gruppo attuale al client che si e' connesso
                            try {

                                MembershipMessage membershipMessage0 = new MembershipMessage(3,tableGroup);

                                sendReliable(membershipMessage0,clientInfo.getInetAddress());

                            }
                            catch (ReliableMessageNotSentException e) {
                                e.printStackTrace();
                            }

                            //Verifica che il peer che si e' connesso non sia gia' contenuto sul database

                            boolean isContentOnDatabase = databaseHelper.isPeerContentInDatabase(clientInfo.getMacAddress());
                            //Aggiungo il peer sul database solo se non e' gia' contenuto
                            if (!isContentOnDatabase) {
                                // Creating peers
                                Peer peer = new Peer(clientInfo.getMacAddress(),getDateTime());
                                // Inserting peer in table PEER and return peer_id
                                long peer_id = databaseHelper.createPeer(peer);
                                // Inserting group and peer in table GROUP_PEER and return group_peer_id
                                long group_peer_id = databaseHelper.createGroupPeer(group.getMacAddressGo(),group.getGroupName(),peer_id);

                            }
                            //Aggiungo il peer ricevuto alla lista da spedire a tutti gli altri client
                            memberTable.put(clientInfo.getMacAddress(),clientInfo);

                            for (ClientInfo cli : memberTable.values()){

                                try {

                                    MembershipMessage membershipMessage0 = new MembershipMessage(0,memberTable);

                                    sendReliable(membershipMessage0,cli.getInetAddress());

                                }
                                catch (ReliableMessageNotSentException e) {
                                    e.printStackTrace();
                                }

                            }

                        }

                        // sto ricevendo un membershipMessage con campo type = 0 contenente una hashtable<String,ClientInfo> (Sono Client)
                        else if (membershipMessage.getType() == 0) {

                            Hashtable <String,ClientInfo > memberTableRec = (Hashtable <String,ClientInfo >) membershipMessage.getPayload();

                            for (ClientInfo clientInfo : memberTableRec.values()){

                                boolean isContentOnDatabase = databaseHelper.isPeerContentInDatabase(clientInfo.getMacAddress());
                                //Aggiungo il peer sul database solo se non e' gia' contenuto
                                if (!isContentOnDatabase) {
                                    // Creating peers
                                    Peer peer = new Peer(clientInfo.getMacAddress(),getDateTime());
                                    // Inserting peer in table PEER and return peer_id
                                    long peer_id = databaseHelper.createPeer(peer);
                                    // Inserting group and peer in table GROUP and table GROUP_PEER and return group_id
                                    long group_peer_id = databaseHelper.createGroupPeer(group.getMacAddressGo(),group.getGroupName(),peer_id);
                                }

                            }
                            // sto ricevendo un membershipMessage con campo type = 3 contenente una List<Group> (Sono Client)
                        } else if (membershipMessage.getType() == 3) {

                            List<Group> tableGroup = (List<Group>) membershipMessage.getPayload();

                            for(int a=0; a< tableGroup.size(); a++) {
                                Group group = tableGroup.get(a);
                                // Inserisce il gruppo in TABLE_GROUP del database e ritorna il group_id
                                long group_id = databaseHelper.createGroup(group);

                            }
                        }
                        //olManager.notifyMessageReliable(message, connectionSocket.getInetAddress());

                    }
                }
                catch (OptionalDataException ode) {
                    Log.e(tag, Log.getStackTraceString(ode));
                }
                catch(ClassCastException cce){
                    Log.e(tag, Log.getStackTraceString(cce));
                }
                catch (ClassNotFoundException cnfe) {
                    Log.e(tag, Log.getStackTraceString(cnfe));
                }
                catch (IOException ioe) {
                    Log.e(tag, Log.getStackTraceString(ioe));
                }
                catch (Exception e) {
                    //Log.e(tag, Log.getStackTraceString(e));
                }

                finally{
                    try{
                        inFromClient.close();
                        connectionSocket.close();
                    }
                    catch(IOException ioe){
                        Log.e(tag, Log.getStackTraceString(ioe));
                    }
                }
            }
        }
    }

    //3-5-11 Changed to public static
    public InetAddress getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress;
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e(tag, "Unable to get ip address: "+Log.getStackTraceString(ex));
        }
        return null;
    }

    /**
     * Tale Metodo ritorna data e ora attuali
     * */
    public String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }

    public Hashtable getMemberTable() {

        return memberTable;
    }

    public void setGroup(Group group) {

        this.group = group;
        reliableReceiver.setGroup(this.group);

    }
    public void setMemberTable(Hashtable memberTable) {

        this.memberTable = memberTable;

    }



}
