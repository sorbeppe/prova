package iit.cnr.wifidirectsocial;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONArray;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.lang.Math;
import java.util.concurrent.LinkedBlockingQueue;

import iit.cnr.wifidirectsocial.Model.ClientInfo;
import iit.cnr.wifidirectsocial.Model.DeviceWithParamGreatestGroup;
import iit.cnr.wifidirectsocial.Model.MembershipMessage;
import iit.cnr.wifidirectsocial.Model.TransmissionManager;
import iit.cnr.wifidirectsocial.Model.DeviceConIndiceDiBonta;
import iit.cnr.wifidirectsocial.Model.GOblacklist;
import iit.cnr.wifidirectsocial.Model.GoConClient;
import iit.cnr.wifidirectsocial.Model.OutgoingReliableMsgInfo;
import iit.cnr.wifidirectsocial.Model.PeersComuniGroupId;
import iit.cnr.wifidirectsocial.Model.PeersGroupIdDevice;
import iit.cnr.wifidirectsocial.Model.ReliableMessageNotSentException;
import iit.cnr.wifidirectsocial.sqlite.model.Group;
import iit.cnr.wifidirectsocial.sqlite.helper.DatabaseHelper;
import iit.cnr.wifidirectsocial.sqlite.model.Group_peer;
import iit.cnr.wifidirectsocial.sqlite.model.Peer;

public class WifidirectScan extends Activity {


    private Button start_group;
    private Button create_group;
    private Button remove_group;
    private Button remove_group_da_Go;
    private Button discover_peer;
    private Button stop_discover_peer;
    private Button request_peer;
    private Button connect_to_a_peer;
    private Button cancel_connect_to_a_peer;



    int contatore;
    // Database Helper: contiene i metodi per gestire il database
    DatabaseHelper db;

    // Contiene i 2 thread per ricevere e mandare la lista aggiornata dei client
    // attraverso dei socket
    TransmissionManager transmissionManager;



    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;


    public static final String TAG = "WifidirectScan";



    // Dichiarazione Variabili e Liste usate nel Protocollo di formazione dei gruppi

    private List <WifiP2pDevice> PEERscanlist;
    private List <WifiP2pDevice> PEERlist;
    private List <WifiP2pDevice> GOlist;
    private List <WifiP2pDevice> GOgoodlist;
    private List <GOblacklist> GOblacklist;
    private List <WifiP2pDevice> MElist;
    private List <WifiP2pDevice> PEERlistnew;


    //***** Parametri di avvio del protocollo *******//


    //int N_disc = N_disc_casuale(2,5);
    int N_disc;
    int z;
    // Unita' di decremento o incremento del numero di cicli di discovePeer Z
    public static double X;
    public static int x;


    //***** Flag che si accendono e spengono durante il funzionamento del protocollo *******//


    // diventa TRUE quando il device si connette ad un GO e FALSE quando si esegue una removeGroup
    public static boolean connesso;

    // diventa TRUE quando il device si autoelegge a GO mediante la CREATE GROUP
    public static boolean sonoGO;

    // diventa TRUE quando viene invocato esplicitamente il metodo stopPeerDiscover()
    public static boolean discStopManualmente;
    public static boolean discAvvManualmente;


    //***** Parametri e variabili usate nello stato sonoGO *******//

    private List <WifiP2pDevice> deviceList;
    private List <WifiP2pDevice> goListAvailable;
    private List <WifiP2pDevice> clientList;
    private List <WifiP2pDevice> GOscangolist;
    private List <WifiP2pDevice> PEERscangolist;
    private List <GoConClient> GOscangolistnew;
    private Group group;
    private long group_id;
    private aggiornaMemberTablePeriodicamente aggMemberTablePeriodicamente;
    private evaluationGroupDestroy evaluationGroupDestroyPeriodically;
    Hashtable<String,ClientInfo> currentMemberTable;

    // Probabilita' di distruzione originaria di un gruppo
    static double p;
    // Probabilita' di distruzione di un gruppo dopo aver valutato la situazione circostante
    static double p_destr;
    // valore casuale tra 0.0 e 1.0 da confrontare con la p_destr
    static double h;

    // Capacita' massima fissata di un gruppo
    static int C = 7;
    //
    private List <GoConClient> GOscangolistConClient;
    private List <GoConClient> MElistConClient;

    // Nello stato sonoGO, Tempo dopo il quale avvio un timerTask per valutare l'eventuale
    // distruzione del mio gruppo o unione con altri gruppi
    public int t_union = 30000;


    //*****   Parametri e variabili usate nello stato sono CLIENT  *******//

    aggiornaGOblacklistPeriodicamente aggGOblacklistPeriodicamente;
    // Tempo dopo il quale avviene la disconnessione automatica da un GO
    public int t_exit = t_exit_casuale(120000,130000);
    static InetAddress InetAddressDelGo;
    Hashtable<String,ClientInfo> memberTable;



    //***** Parametri e variabili usate nello stato non sono ne' GO ne' CLIENT *******//


    //Numero di peer totali del gruppo migliore di un device
    static int nrPeerTotGruppoMigliore;


    public WifidirectScan() {
    }


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        registerReceiver(mReceiver, mIntentFilter);
        registerReceiver(receiverProva, mIntentFilter);

        // Inizializzazione del numero random  di discovery N_disc da effettuare al primo avvio del protocollo

        N_disc = N_disc_casuale(5,35);
        z = N_disc;
        Log.i(TAG,"Il numero inziale di discover peer e' di: " + N_disc);

        contatore = 0;
        // Inizializzazione Variabili e Liste usate nel Protocollo di formazione dei gruppi

        PEERscanlist = new ArrayList<>();
        PEERlist = new ArrayList<>();
        GOlist = new ArrayList<>();
        GOgoodlist = new ArrayList<>();
        GOblacklist = new ArrayList<>();
        MElist = new ArrayList<>();
        PEERlistnew = new ArrayList<>();

        //***** Inizializzazione Parametri e variabili usate nello stato sonoGO *******//

        deviceList = new ArrayList<>();
        goListAvailable = new ArrayList<>();
        clientList = new ArrayList<>();
        GOscangolist = new ArrayList<>();
        PEERscangolist = new ArrayList<>();
        GOscangolistnew = new ArrayList<>();
        GOscangolistConClient = new ArrayList<>();
        MElistConClient = new ArrayList<>();

        // cancellazione di tutte le tabelle del database ad ogni avvio dell'applicazione

        db = new DatabaseHelper(getApplicationContext());
        //db.deleteAllGroup();
        Log.d("Group Count", "Gruppi memorizzati sul db: " + db.getAllGroups().size());
        db.deleteAllPeer();
        Log.d("Peer Count", "Peers memorizzati sul db: " + db.getAllPeers().size());
        db.deleteAllGroupPeer();
        Log.d("Group_peer Count", "Group_peer memorizzati sul db: " + db.getAllGroupPeer().size());


        // Dichiara la classe atta a fer partire i 2 thread che hanno la funzionalita' ricevere e mandare la lista dei client connessi ad ogni
        // altro client del gruppo

        transmissionManager = new TransmissionManager(getApplicationContext());
        memberTable = new Hashtable<String,ClientInfo>();
        transmissionManager.setMemberTable(memberTable);

        // Aggiunta di prova del gruppo di nome groupNexusC nel database

        // Creating groups
        //Group groupNexusC = new Group("ce:fa:00:e5:99:5a","DIRECT-z4-nexus C",getDateTime());
        // Inserting groups in db
        //long groupNexusC_id = db.createGroup(groupNexusC);

        // Aggiunta di prova del gruppo di nome groupNexus6 nel database

        // Creating groups
        //Group groupNexus6 = new Group("fa:cf:c5:a0:42:3a","DIRECT-3R-Nexus 6",getDateTime());
        // Inserting groups in db
        //long groupNexus6_id = db.createGroup(groupNexus6);



        //aggiunta di prova di un GO device in blacklist

        //WifiP2pDevice nexus6Blacklist = new WifiP2pDevice();
        //nexus6Blacklist.deviceAddress = "fa:cf:c5:a0:42:3a";
        //nexus6Blacklist.deviceName = "Nexus 6";
        //GOblacklist.add(nexus6Blacklist);

        //aggiunta di prova di un GO device in blacklist

        //WifiP2pDevice nexusCBlacklist = new WifiP2pDevice();
        //nexusCBlacklist.deviceAddress = "ce:fa:00:e5:99:5a";
        //nexusCBlacklist.deviceName = "Nexus C";
        //GOblacklist.add(nexusCBlacklist);


        /*

        // Creating peers
        Peer peer1 = new Peer("f8:a9:d0:65:10:0f",getDateTime());
        Peer peer2 = new Peer("8a:b5:40:45:13:0a",getDateTime());
        Peer peer3 = new Peer("77:a6:d0:1D:10:07",getDateTime());
        Peer peer4 = new Peer("a9:a9:47:65:17:b9",getDateTime());

        // Inserting peers in db
        long peer1_id = db.createPeer(peer1);
        long peer2_id = db.createPeer(peer2);
        long peer3_id = db.createPeer(peer3);


        Log.d("Peer Count", "Peer memorizzati sul db: " + db.getAllPeers().size());
        System.out.println(peer1_id);
        System.out.println(peer2_id);
        System.out.println(peer3_id);

        // Creating groups

        Group group1 = new Group("f8:a9:d0:65:10:0f","DIRECT-ZR-NEXUS A",getDateTime());
        Group group2 = new Group("f8:a9:d0:65:10:0f","DIRECT-ab-NEXUS E",getDateTime());
        Group group3 = new Group("f8:a9:d0:65:10:0f","DIRECT-QR-NEXUS B",getDateTime());
        Group group4 = new Group("f8:a9:d0:65:10:0f","DIRECT-ce-NEXUS D",getDateTime());


        // Inserting groups in db
        long group1_id = db.createGroup(group1, new long[] { peer1_id });
        long group2_id = db.createGroup(group2, new long[] { peer2_id });
        long group3_id = db.createGroup(group3, new long[] { peer3_id});
        long group4_id = db.createGroup(group4, new long[] { peer1_id,peer2_id,peer3_id });


        Log.d("Group Count", "Gruppi memorizzati sul db: " + db.getAllGroups().size());
        System.out.println(group1_id);
        System.out.println(group2_id);
        System.out.println(group3_id);
        System.out.println(group4_id);

        Log.d("Numero Assciazioni Group_peer", "Associazioni group_peer memorizzati sul db: " + db.getAllGroupPeer().size());
        db.stampAllGroupPeer();


        // "Post new Article" - assigning this under "Important" Tag
        // Now this will have - "Androidhive" and "Important" Tags

        //db.createTodoTag(todo10_id, tag2_id);
        */



        // Buttons


        start_group = (Button) findViewById(R.id.start_group);
        start_group.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Do something in response to button click
                Intent intent=new Intent(WifidirectScan.this,listViewJson.class);
                startActivity(intent);
                Log.i(TAG, "parson Json avviato");
                // startProtocol(0);
                //Log.i(TAG, "Il protocollo e' stato avviato per la prima volta: ");
            }
        });



        create_group = (Button) findViewById(R.id.create_group);
        create_group.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Do something in response to button click
                createGroup();
            }
        });
        remove_group = (Button) findViewById(R.id.remove_group);
        remove_group.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Do something in response to button click
                removeGroup();
            }
        });

        remove_group_da_Go = (Button) findViewById(R.id.remove_group_da_Go);
        remove_group_da_Go.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Do something in response to button click
                removeGroupDaGo();
            }
        });



        discover_peer = (Button) findViewById(R.id.discover_peer);
        discover_peer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Do something in response to button click
                discoverPeers();
            }
        });

        stop_discover_peer = (Button) findViewById(R.id.stop_discover_peer);
        stop_discover_peer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Do something in response to button click
                stopPeerDiscovery();

            }
        });

        request_peer = (Button) findViewById(R.id.request_peer);
        request_peer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Do something in response to button click
                requestPeersDaGo();

            }
        });

        connect_to_a_peer = (Button) findViewById(R.id.connect_to_a_peer);
        connect_to_a_peer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Do something in response to button click
                connect2();
                //diffTraDueDate();
            }
        });

        cancel_connect_to_a_peer = (Button) findViewById(R.id.cancel_connect_to_a_peer);
        cancel_connect_to_a_peer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Do something in response to button click
                //cancelConnect();
                //System.out.println("Il mio MAC-ADDRESS e': " + getMyMacAddress());


            }
        });

        //Toast.makeText(this, "Creazione gruppo WifiDirect avviato", Toast.LENGTH_LONG).show();
        //Log.i(TAG, "Servizio Creato");


    }



    public void startProtocol(int x) {

        //int N_disc = N_disc_casuale(2,5);
        //int N_disc = 2;
        //System.out.println(" Il numero iniziale di N_disc e' pari a: " + N_disc);
        //z = N_disc;
        z = z + x;
        if (z>0){
            discoverPeers();

        }  else {

            // Nel metodo createGroup setto il flag sonoGo = TRUE
            createGroup();

        }
    }



    // Tale Broadcaster Receiver riceve alcuni INTENT mandati dal sistema operativo quando cambia
    // lo stato della connessione relativa a Wifi-Direct ( aggiunta nuovi PEERS etc)

    BroadcastReceiver receiverProva = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)){


                WifiP2pInfo wifiP2pInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
                System.out.println(wifiP2pInfo);
                //WifiP2pGroup wifiP2pGroup = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);
                //System.out.println(wifiP2pGroup);


                if (wifiP2pInfo.groupFormed) {

                    // Fa partire i 2 thread di ricezione e invio (attraversi i relativi socket) di informazioni
                    // tra il GO e i client

                    transmissionManager.start();

                    WifiP2pGroup wifiP2pGroup = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);
                    WifiP2pDevice deviceGo = wifiP2pGroup.getOwner();
                    String passPhrase = wifiP2pGroup.getPassphrase();
                    Log.i(TAG,"Passphrase of this group is: " + passPhrase );

                    // Istanzio l'oggetto Group
                    group = new Group(deviceGo.deviceAddress,wifiP2pGroup.getNetworkName(),getDateTime(),"","");
                    // Inserisce il gruppo in TABLE_GROUP del database e ritorna il group_id (Sia da Go che da Client)
                    group_id = db.createGroup(group);

                    transmissionManager.setGroup(group);


                    if (wifiP2pInfo.isGroupOwner) {

                        //Faccio ripartie la discover quando divento GO perche' gli altri mi vedano
                        discoverPeers();
                        Log.i(TAG, "sono diventato GO");
                        // TimerTask che aggiorna la memberTable ogni 10 secondi e la spedisce a tutti i client

                        aggMemberTablePeriodicamente = new aggiornaMemberTablePeriodicamente();
                        aggMemberTablePeriodicamente.startAggiornaMemberTablePeriodicamente();
                        // TimerTask che valuta le condizioni per l'eventuale distruzione del gruppo ogni t_union secondi
                        evaluationGroupDestroyPeriodically = new evaluationGroupDestroy();
                        evaluationGroupDestroyPeriodically.startEvaluationGroupDestroy();

                        currentMemberTable = new Hashtable<String,ClientInfo>();



                    } else if (!wifiP2pInfo.isGroupOwner)  {


                        connesso = true;
                        // Fa partire il timerTask che controlla e aggiorna automaticamente e periodicamente la GOblacklist
                        aggGOblacklistPeriodicamente = new aggiornaGOblacklistPeriodicamente();
                        aggGOblacklistPeriodicamente.startAggiornaGOblacklistPeriodicamente();
                        Log.i(TAG, "sono diventato CLIENT");
                        // Recupera l'indirizzo iP del GO
                        InetAddressDelGo = wifiP2pInfo.groupOwnerAddress;

                        // Recupero le informazioni su me stesso

                        WifiP2pDevice ME = MElist.get(0);
                        String myMac = ME.deviceAddress;
                        String myName = ME.deviceName;

                        ClientInfo clientInfo = new ClientInfo(myMac,myName,null);
                        MembershipMessage membershipMessage = new MembershipMessage(1,clientInfo);

                        try {
                            transmissionManager.sendReliable(membershipMessage, InetAddressDelGo);

                        }

                        catch (ReliableMessageNotSentException rmnse)   {

                        }
                        //Group groupDeviceGo = new Group(deviceGo.deviceAddress,wifiP2pGroup.getNetworkName(),getDateTime());
                        //long groupDeviceGo_id = db.createGroup(groupDeviceGo);
                        //Log.i(TAG, "Il GO: " + deviceGo.deviceName + " e' stato aggiunto sul database");
                        //db.stampAllGroup();
                    }

                }  else  {

                    transmissionManager.stop();
                    // Sei il Go a cui ero connesso distrugge il gruppo, il mio flag Connesso e' rimasto settato a TRUE,
                    //  devo fare ripartire il protocollo con x=0
                    if (connesso) {

                        Log.i(TAG, "Il GO a cui ero connesso ha distrutto il gruppo");
                        N_disc = N_disc_casuale(5,15);
                        z = N_disc;
                        Log.i(TAG, "Il nuovo numero di cicli di discover Peers da eseguire e' pari a: " + N_disc);
                        startProtocol(0);
                    }

                }


            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

                System.out.println("aggiornamento lista peer in zona");

                //WifiP2pDeviceList PEERscanlist = intent.getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST);
                // PEERscanlist.addAll(wifip2pdevicelist);
                //System.out.println(PEERscanlist);
                //requestPeersNew();
                //protocol();
            }

            else if(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)){
                //do somethign here
                WifiP2pDevice device = (WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);

                if (!MElist.contains(device)){
                    //Crea una lista formata solo da me stesso per recuperare il mio Mac address e il mio nome
                    MElist.add(device);
                }

                String myMac = device.deviceAddress;
                String myName = device.deviceName;
                Log.d(TAG, "Il mio nome WifiDirect e': " + myName + " ,Il mio MAC ADDRESS e': " + myMac);

            }  else if(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)){

                int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE,-1);
                if (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED) {

                    //Log.i(TAG, "WIFI_P2P_DISCOVERY_STARTED");

                } else if (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED) {

                    if (sonoGO) {
                        //Rilancio la discover peer al termine dei 2 minuti per essere visibili agli altri
                        discoverPeers();
                        Log.i(TAG, "Sono GO, sono trascorsi i 2 minuti rilancio la discover Peer");
                    }
                    //Log.i(TAG, "WIFI_P2P_DISCOVERY_STOPPED");

                }
                //Necessario per evitare che venga eseguita la condizione dentro l'if al primo intent ricevuto (avvio dell'applicazione)
                contatore = contatore + 1;
                System.out.println("DISCOVERY AVVIATA O STOPPATA");
                if (!discStopManualmente && contatore > 2) {
                    // Fa ripartire il protocollo decrementando Z di un'unita' alla scadenza dei 2 minuti
                    startProtocol(-1);
                    Log.i(TAG, "Sono trascorsi 2 minuti e non ci sono peers nelle vicinanze: riavvio il protocollo decrementando z di una unita'");
                }
            }

        }
    };




    // Tale metodo mi ritorna il mio MAC-ADDRESS relativo al Wifi-Direct (e' diverso dal MaC-Address del normale Wifi)
    // N.B. Solo sul NEXUS 6 l'interfaccia di rete del wifi-direct si crea solo dopo aver fatto una create group o dopo
    //che si e' connessi ad un gruppo



    public String getWFDMacAddress(){

        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface ntwInterface : interfaces) {

                if (ntwInterface.getName().contains("p2p")) {
                    byte[] byteMac = ntwInterface.getHardwareAddress();
                    if (byteMac==null){
                        return null;
                    }
                    StringBuilder strBuilder = new StringBuilder();
                    for (int i=0; i<byteMac.length; i++) {
                        strBuilder.append(String.format("%02X:", byteMac[i]));
                    }

                    if (strBuilder.length()>0){
                        strBuilder.deleteCharAt(strBuilder.length()-1);
                    }

                    System.out.println(strBuilder.toString());
                    return strBuilder.toString();
                }

            }
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }

        return null;
    }


    //Initiates peer discovery

    public void discoverPeers() {

        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

                Log.i(TAG, "Discovery Peers OK");
                Toast.makeText(WifidirectScan.this, "Discover Peers avviata con successo", Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onFailure(int reasonCode) {
                Log.i(TAG, "Discovery Peers Not OK");

            }
        });


    }

    // Stoppa esplicitamente la fase di discoveryPeers

    public void stopPeerDiscovery() {

        mManager.stopPeerDiscovery(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

                Log.i(TAG, "stopPeerDiscovery OK");
                Toast.makeText(WifidirectScan.this, "Discover Peers fermata con successo", Toast.LENGTH_SHORT).show();
                discStopManualmente = true;
            }

            @Override
            public void onFailure(int reasonCode) {
                Log.i(TAG, "stopPeerDiscovery NOT OK");

            }
        });

    }

    // Metodo che ritorna la lista di device scansionati all'istante attuale, richiamato ogni t_union secondi
    // quando mi trovo nello stato sono GO

    public void requestPeersDaGo() {

        if (mManager != null) {
            mManager.requestPeers(mChannel, peerListListener);
            Log.i(TAG, "Request Peers avviata con successo");

        }

    }

    private PeerListListener peerListListener = new PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {

            // Out with the old, in with the new.
            deviceList.clear();
            //goListAvailable.clear();
            // il metodo getDeviceList ritorna una Collection <WifiP2pDevice>
            deviceList.addAll(peerList.getDeviceList());


            //WifiP2pDevice device0 = PEERscanlist.get(0);
            //String booleano String.valueOf(device0.isGroupOwner());

            //System.out.println(PEERscanlist);

            // If an AdapterView is backed by this data, notify it
            // of the change.  For instance, if you have a ListView of available
            // peers, trigger an update.

        }
    };

    // The requestPeers() method is also asynchronous and can notify your activity when a
    // list of peers is available with onPeersAvailable(),

    // Ritorna la lista di peers disponibili dopo la fase di discoverPeers, e quando si
    // aggiunge o se ne va un nuovo peer

    public void requestPeersNew() {

        if (mManager != null) {
            mManager.requestPeers(mChannel, peerListListener2);
            Log.i(TAG, "Request Peers avviata con successo");

        }

    }

    private PeerListListener peerListListener2 = new PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {

            // Out with the old, in with the new.
            PEERscanlist.clear();
            // il metodo getDeviceList ritorna una Collection <WifiP2pDevice>
            PEERscanlist.addAll(peerList.getDeviceList());
            //WifiP2pDevice device0 = PEERscanlist.get(0);
            //String booleano String.valueOf(device0.isGroupOwner());

            //System.out.println(PEERscanlist);

            // If an AdapterView is backed by this data, notify it
            // of the change.  For instance, if you have a ListView of available
            // peers, trigger an update.
            if (PEERscanlist.size() == 0) {
                Log.i(WifidirectScan.TAG, "Nessun dispositivo trovato");

            } else if(PEERscanlist.size() != 0){


                for(int i=0;i<PEERscanlist.size();i++){
                    WifiP2pDevice device = PEERscanlist.get(i);
                    boolean isGroupOwner = device.isGroupOwner();
                    if (isGroupOwner){
                        if (!GOlist.contains(device)) {
                            GOlist.add(device);
                            Log.i(WifidirectScan.TAG, "Il dispositivo " + device.deviceName + " e' stato aggiunto alla lista GOlist");
                        }

                    }   else if (!isGroupOwner){
                        if (!PEERlist.contains(device)) {
                            PEERlist.add(device);
                            Log.i(WifidirectScan.TAG, "Il dispositivo " + device.deviceName + " e' stato aggiunto alla lista PEERlist");
                        }

                    }
                }
                Log.i(WifidirectScan.TAG, "Il numero di dispositivi contenuti nella GOList e' di: " + GOlist.size());
                Log.i(WifidirectScan.TAG, "Il numero di dispopsitivi contenuti nella PEERList e' di: " + PEERlist.size());
            }

            // Controlli effettuati solo nel caso in cui il device si trova nello stato di GO ovvero quando il flag sonoGO = TRUE
            /*
            if (sonoGO) {

                // Pulizia delle liste memorizzate nei cicli precedenti
                GOscangolist.clear();
                PEERscangolist.clear();

                for(int i=0;i<PEERscanlist.size();i++){
                    WifiP2pDevice device = PEERscanlist.get(i);
                    boolean isGroupOwner = device.isGroupOwner();
                    if (isGroupOwner == true){

                        GOscangolist.add(device);
                        Log.i(WifidirectScan.TAG, "Il dispositivo " + device.deviceName + " e' stato aggiunto alla lista GOscangolist");


                    }   else if (isGroupOwner == false){

                        PEERscangolist.add(device);
                        Log.i(WifidirectScan.TAG, "Il dispositivo " + device.deviceName + " e' stato aggiunto alla lista PEERscangolist");


                    }
                }

                Log.i(WifidirectScan.TAG, "Il numero di dispositivi contenuti nella GOscangoList e' di: " + GOscangolist.size());
                Log.i(WifidirectScan.TAG, "Il numero di dispopsitivi contenuti nella PEERscangoList e' di: " + PEERscangolist.size());


                    if (GOscangolist.size() == 0) {

                        //stopPeerDiscovery();
                        // Tale metodo esegue la removeGroup settando il flag sonoGO = FALSE e fa ripartire il protocollo settando una nova z
                        //removeGroupDaGo();


                    } else if (GOscangolist.size() != 0) {


                        // Ricevo la lista aggiornata dei client a me connessi
                        Hashtable<String, ClientInfo> listaClient = (Hashtable) transmissionManager.getMemberTable();

                        // Probabilita' di distruzione originaria del gruppo
                        p = (1 - (listaClient.size() / C));

                        double NrClientIpotizzatiPerGo = ((PEERscangolist.size() - listaClient.size()) / GOscangolist.size());

                        // Scorro la lista dei GO scansionati e aggiungo il numero di Client ipotizzati per ogni GO creando una nuova lista GOscangolistConClient
                        for (int i = 0; i < GOscangolist.size(); i++) {
                            WifiP2pDevice device = GOscangolist.get(i);
                            GoConClient goconclient = new GoConClient(NrClientIpotizzatiPerGo);
                            goconclient.deviceAddress = device.deviceAddress;
                            goconclient.deviceName = device.deviceName;
                            GOscangolistConClient.add(goconclient);

                        }

                        // Aggiungo a me stesso il numero di Client a me connessi creando una nuova lista MElistConClient
                        WifiP2pDevice ME = MElist.get(0);
                        GoConClient meconclient = new GoConClient(listaClient.size());
                        meconclient.deviceAddress = ME.deviceAddress;
                        meconclient.deviceName = ME.deviceName;
                        MElistConClient.add(meconclient);


                        // Aggiungo alla lista GOscangolistConClient le informazioni su me stesso (memorizzate nella MElistConClient)
                        GOscangolistnew.clear();
                        GOscangolistnew.addAll(GOscangolistConClient);
                        GOscangolistnew.addAll(MElistConClient);
                        Log.i(WifidirectScan.TAG, "Il numero di device nella Goscangolistnew e' di: " + GOscangolistnew.size());

                        // Ordina la lista GOscangolistnew creando una Classifica in cui Il GO migliore e' l'ultimo elemento
                        // della lista ordinata

                        Collections.sort(GOscangolistnew, new comparatorGoConClient());


                        // Caso in cui Io sono il migliore in classifica
                        if (meconclient.deviceAddress == GOscangolistnew.get(GOscangolistnew.size() - 1).deviceAddress) {

                            // d e' la distanza tra me ed il secondo in classifica nel caso in cui Io sono il migliore
                            double d = meconclient.getNrClient() - GOscangolistnew.get(GOscangolistnew.size() - 2).getNrClient();
                            // d_max e' la distanza tra me e l'ultimo in classifica nel caso in cui Io sono il migliore
                            double d_max = meconclient.getNrClient() - GOscangolistnew.get(0).getNrClient();
                            // Torna il valore assoluto di d_max
                            double val_abs_d_max = Math.abs(d_max);
                            // Torna la distanza normalizzata
                            double d_norm = d / val_abs_d_max;
                            // Essendo il migliore DECREMENTO la mia probabilita' di distruzione originaria p secondo la formula seguente
                            p_destr = p - (p * d_norm);


                        }
                        // Caso in cui Io non sono il migliore in classifica
                        else if (meconclient.deviceAddress != GOscangolistnew.get(GOscangolistnew.size() - 1).deviceAddress) {

                            // d e' la distanza tra me ed il migliore in classifica nel caso in cui Io non sono il migliore
                            double d = meconclient.getNrClient() - GOscangolistnew.get(GOscangolistnew.size() - 1).getNrClient();
                            // d_max e' la distanza tra l'ultimo in classifica ed il migliore in classifica
                            double d_max = GOscangolistnew.get(0).getNrClient() - GOscangolistnew.get(GOscangolistnew.size() - 1).getNrClient();
                            // Torna il valore assoluto di d_max
                            double val_abs_d_max = Math.abs(d_max);
                            // Torna la distanza normalizzata
                            double d_norm = d / val_abs_d_max;

                            // Non essendo il migliore in classifica INCREMENTO la mia probabilita' di distruzione originaria p secondo la formula seguente
                            p_destr = p + ((1 - p) * Math.abs(d_norm));

                        }

                        // Tale metodo torna un numero double casuale h compreso tra 0.0 e 1.0
                        Random r = new Random();
                        // valore casuale tra 0.0 e 1.0 da confrontare con la p_destr
                        double h = r.nextDouble();


                        if (h <= p_destr) {

                            //stopPeerDiscovery();
                            // Tale metodo esegue la removeGroup settando il flag sonoGO = FALSE e fa ripartire il protocollo settando una nova z
                            removeGroupDaGo();


                        }

                    }


            }

            */

            if (GOlist.size() != 0 && !sonoGO){

                for(int i=0;i<GOlist.size();i++){
                    WifiP2pDevice device = GOlist.get(i);
                    if (GOblacklist.contains(device)) {
                        Log.i(WifidirectScan.TAG, "Il device " + device.deviceName + " e' presente nella lista GOblacklist, non lo considero");
                        GOgoodlist.remove(device);
                    } else if (!GOblacklist.contains(device)){

                        GOgoodlist.add(device);
                        Log.i(WifidirectScan.TAG, "Il device " + device.deviceName + " e' stato aggiunto nella Lista GOgoodlist");

                    }
                }
                Log.i(WifidirectScan.TAG, "Il numero di dispositivi presenti nella GOgoodlist e' pari a: " + GOgoodlist.size());




                if (!connesso) {

                    List <Group> gruppiDatabase = db.getAllGroups();
                    int a = GOgoodlist.size();


                    if (gruppiDatabase.size()!= 0) {

                        while(a>0){

                            WifiP2pDevice device = GOgoodlist.get(a-1);

                            for(int b=0;b<gruppiDatabase.size();b++){
                                Group group = gruppiDatabase.get(b);
                                if(group.getMacAddressGo().equals(device.deviceAddress)){
                                    WifiP2pConfig config = new WifiP2pConfig();
                                    config.deviceAddress = device.deviceAddress;
                                    config.wps.setup = WpsInfo.PBC;
                                    config.groupOwnerIntent = 0;

                                    // mi connetto al GO appartenente alla Gogoodlist (non attulamente in blacklist) e gia' memorizzato sul database
                                    connect(config);

                                    // Tale metodo permette al DEVICE CLIENT una volta connesso, di disconnettersi dopo un certo tempo t_exit tramite l'invocazione del metodo
                                    // removeGroup ritardato per l'appunto di t_exit_casuale secondi;
                                    removeGroupDelayed rgd = new removeGroupDelayed();
                                    rgd.startRemoveGroupDelayed();

                                    //Aggiunta del GO in GOblacklist con un timestamp ritardato di t_exit_casuale secondi

                                    //String timeStampGOblacklistDelayed = timeStampGOblacklistDelayed(getDateTime());
                                    //GOblacklist goblacklist = new GOblacklist(timeStampGOblacklistDelayed);
                                    //goblacklist.deviceAddress = device.deviceAddress;
                                    //goblacklist.deviceName = device.deviceName;
                                    //GOblacklist.add(goblacklist);

                                    Log.i(WifidirectScan.TAG, "Mi sono connesso al device " + device.deviceName + " appartenente alla GOgoodlist gia' memorizzato nel database");
                                    a --;

                                } else {

                                    a --;

                                }
                            }

                        }

                        // Se nessuno dei Go scansionati e' presente sul mio database e quindi ancora non sono connesso
                        if (!connesso) {

                            // mi connetto al primo Go della lista GOlist, poiche' non ci sono Go memorizzati sul database
                            WifiP2pDevice device = GOlist.get(0);
                            WifiP2pConfig config = new WifiP2pConfig();
                            config.deviceAddress = device.deviceAddress;
                            config.wps.setup = WpsInfo.PBC;
                            config.groupOwnerIntent = 0;
                            System.out.println("Il device " + device.deviceName + " ha questo mac " + device.deviceAddress);
                            connect(config);


                            // Tale metodo permette al DEVICE CLIENT una volta connesso, di disconnettersi dopo un certo tempo t_exit tramite l'invocazione del metodo
                            // removeGroup ritardato per l'appunto di t_exit_casuale;
                            removeGroupDelayed rgd = new removeGroupDelayed();
                            rgd.startRemoveGroupDelayed();


                            //Aggiunta del GO in GOblacklist con un timestamp ritardato di t_exit_casuale secondi

                            //String timeStampGOblacklistDelayed = timeStampGOblacklistDelayed(getDateTime());
                            //GOblacklist goblacklist = new GOblacklist(timeStampGOblacklistDelayed);
                            //goblacklist.deviceAddress = device.deviceAddress;
                            //goblacklist.deviceName = device.deviceName;
                            //GOblacklist.add(goblacklist);


                            Log.i(WifidirectScan.TAG, "Caso gruppiDatabase != 0,nessun go scansionato e' nel databse, Mi sono connesso al device " + device.deviceName + " appartenente alla GOlist (non ci sono GO nelle vicinanze gia' memorizzati sul database)");

                        }



                    }  else if(gruppiDatabase.size() == 0) {


                        if (GOgoodlist.size()!= 0)  {
                            // mi connetto al primo Go della lista GOgoodlist, poiche' non ci sono Go memorizzati sul database

                            WifiP2pDevice device = GOgoodlist.get(0);
                            WifiP2pConfig config = new WifiP2pConfig();
                            config.deviceAddress = device.deviceAddress;
                            config.wps.setup = WpsInfo.PBC;
                            config.groupOwnerIntent = 0;
                            //System.out.println("Il device " + device.deviceName + " ha questo mac " + device.deviceAddress);
                            connect(config);

                            // Tale metodo permette al DEVICE CLIENT una volta connesso, di disconnettersi dopo un certo tempo t_exit tramite l'invocazione del metodo
                            // removeGroup ritardato per l'appunto di t_exit_casuale;
                            removeGroupDelayed rgd = new removeGroupDelayed();
                            rgd.startRemoveGroupDelayed();

                            //Aggiunta del GO in GOblacklist con un timestamp ritardato di t_exit_casuale secondi

                            String timeStampGOblacklistDelayed = timeStampGOblacklistDelayed(getDateTime());
                            GOblacklist goblacklist = new GOblacklist(timeStampGOblacklistDelayed);
                            goblacklist.deviceAddress = device.deviceAddress;
                            goblacklist.deviceName = device.deviceName;
                            //GOblacklist.add(goblacklist);

                            Log.i(WifidirectScan.TAG, "Caso GOgoodlist != 0, Mi sono connesso al device " + device.deviceName + " appartenente alla GOgoodlist (non ci sono GO nelle vicinanze gia' memorizzati sul database)");

                        }
                    /*
                    else if (GOgoodlist.size() == 0) {

                        WifiP2pDevice device = GOlist.get(0);
                        WifiP2pConfig config = new WifiP2pConfig();
                        config.deviceAddress = device.deviceAddress;
                        config.wps.setup = WpsInfo.PBC;
                        config.groupOwnerIntent = 0;
                        //System.out.println("Il device " + device.deviceName + " ha questo mac " + device.deviceAddress);
                        connect(config);
                        Log.i(WifidirectScan.TAG, "Mi sono connesso al device " + device.deviceName + " appartenente alla GOlist (tutti idevice sono in blacklist)");
                        //connesso = true;
                        // Tale metodo permette al DEVICE CLIENT una volta connesso, di disconnettersi dopo un certo tempo t_exit tramite l'invocazione del metodo
                        // removeGroup ritardato per l'appunto di t_exit_casuale;
                        removeGroupDelayed rgd = new removeGroupDelayed();
                        rgd.startRemoveGroupDelayed();

                        //Aggiunta del GO in GOblacklist con un timestamp ritardato di t_exit_casuale secondi

                        String timeStampGOblacklistDelayed = timeStampGOblacklistDelayed(getDateTime());
                        GOblacklist goblacklist = new GOblacklist(timeStampGOblacklistDelayed);
                        goblacklist.deviceAddress = device.deviceAddress;
                        goblacklist.deviceName = device.deviceName;
                        GOblacklist.add(goblacklist);

                        Log.i(WifidirectScan.TAG, "CAso Gogoolist == 0, Mi sono connesso al device " + device.deviceName + " appartenente alla GOlist (non ci sono GO nelle vicinanze gia' memorizzati sul database)");



                    }
                   */

                    }




                }


            }


            else if (GOlist.size() == 0 && !sonoGO) {

                // Aggiungo alla lista PEERlist le informazioni su me stesso (memorizzate nella MElist)
                PEERlistnew.clear();
                PEERlistnew.addAll(PEERlist);
                PEERlistnew.addAll(MElist);
                Log.i(WifidirectScan.TAG, "Sono nel ramo in cui NON ESISTONO GO ");
                Log.i(WifidirectScan.TAG, "Il numero di device nella Peerlistnew e' di: " + PEERlistnew.size());

                // Creo una lista di oggetti PEER a partire dalla lista di oggetti WIFIP2PDEVICE per un confronto successivo tra
                // liste di oggetti PEER

                List <Peer> PEERlistDaConfrontare = new ArrayList<>();

                for(int b=0;b<PEERlistnew.size();b++){
                    WifiP2pDevice device = PEERlistnew.get(b);
                    Peer peer = new Peer();
                    peer.setMacAddress(device.deviceAddress);
                    PEERlistDaConfrontare.add(peer);

                }

                // Ritorna una lista con tutti i gruppi memorizzati sul database
                List <Group> gruppiDatabase = db.getAllGroups();
                // Ritorna la lista dei peer di uno specifico gruppo di uno specifio GO
                List <Peer> peerDiUnGruppoDiUnGo = new ArrayList<>();

                // Ritorna la lista di tutti gruppi e relativi peers creati da un solo device
                List <PeersGroupIdDevice> PeersGroupIdDevice = new ArrayList<>();


                List <DeviceWithParamGreatestGroup> listaDeviceWithParamGreatestGroup = new ArrayList<>();

                // Ritorna la lista di Device con rispettivi Indici di bonta' per la valutazione della classifica finale

                List <DeviceConIndiceDiBonta> listaDeviceConIndiceDiBonta = new ArrayList<>();

                int j = PEERlistnew.size();
                //Vettore che contiene tutti gli indici di variabilita' da cui estrarre successivamente il valore massimo necessario per la normalizzazione
                double[] indiciDiVariabilita = new double[j];
                //Vettore che contiene tutti i valori di recency da cui estrarre successivamente il valore massimo necessario per la normalizzazione
                long[] valoriDiRecency = new long[j];
                // Variabile che controlla se qualcuno dei peers scansionati e' stato Go in passato
                boolean wasThereAGoInThePast = false;

                while (j>0) {

                    WifiP2pDevice device = PEERlistnew.get(j-1);

                    for(int b=0;b<gruppiDatabase.size();b++){
                        Group group = gruppiDatabase.get(b);

                        // Se il peer scansionato e' stato Go di qualche gruppo memorizzato sul database
                        if (group.getMacAddressGo().equals(device.deviceAddress)) {
                            wasThereAGoInThePast = true;
                            long group_id = group.getId();
                            String mac_address_go = group.getMacAddressGo();
                            String group_name = group.getGroupName();

                            // Ritorna ua lista con tutte le associazioni Group_peer presenti sul database valide per il GRUPPO caratterizzato dalla chiave
                            // mac_address_go,group_name attualmente processato
                            List <Group_peer> group_PeerDatabase = db.getGroupPeer(mac_address_go,group_name);
                            for(int a=0;a<group_PeerDatabase.size();a++){
                                Group_peer group_Peer = group_PeerDatabase.get(a);
                                long peer_id = group_Peer.getPeerId();
                                Peer peer = db.getPeer(peer_id);

                                peerDiUnGruppoDiUnGo.add(peer);

                            }

                            PeersGroupIdDevice pgid = new PeersGroupIdDevice(device.deviceAddress,mac_address_go,group_name,peerDiUnGruppoDiUnGo);
                            PeersGroupIdDevice.add(pgid);



                        }

                    }

                    if (wasThereAGoInThePast) {
                        // Per ciascun device, prendo la lista dei peers di ogni suo gruppo e faccio l'intersezione (prendo gli elementi in comune) con la
                        // lista dei peer scansionati di nome PEERlistDaConfrontare, scegliendo alla fine solo il gruppo che presenta piu? peers in comune con quelli
                        // scansionati

                        // Lista di piu' elementi caratterizzati dalla coppia di valori: group_id <-> Nr. peers in comune del gruppo con la lista peers attualmente scansionata
                        List <PeersComuniGroupId> PeersComuniGroupId = new ArrayList<>();

                        for(int a=0;a< PeersGroupIdDevice.size();a++){
                            PeersGroupIdDevice pgid = PeersGroupIdDevice.get(a);
                            String mac_address_go = pgid.getMacAddressGo();
                            String group_name = pgid.getGroupName();
                            List <Peer> listapeers = pgid.getListaPeers();
                            List <Peer> listaPeersComuni = intersezioneTraDueListe(listapeers, PEERlistDaConfrontare);
                            int nrPeersComuni = listaPeersComuni.size();
                            PeersComuniGroupId pcgi = new PeersComuniGroupId(mac_address_go,group_name,nrPeersComuni);
                            PeersComuniGroupId.add(pcgi);

                        }

                        // Ordina la lista PeersComuniGroupId in base al campo intero Nr.PeersComuni

                        Collections.sort(PeersComuniGroupId, new comparatorPeersComuniGroupId());

                        // Ritorna il gruppo migliore (quello col nrPeersComuni maggiore) del device attualmente scansionato

                        String macAddressGoGruppoMigliore = PeersComuniGroupId.get(PeersComuniGroupId.size()-1).getMacAddressGo();
                        String groupNameGruppoMigliore = PeersComuniGroupId.get(PeersComuniGroupId.size()-1).getGroupName();


                        // Si deve valutare una certa funzione di bonta' per il gruppo migliore di questo device e creare

                        // Ritorna il Numero totale di Peers del gruppo migliore di questo device attraverso
                        // la variabile intera di nome nrPeerTotGruppoMigliore

                        for(int a=0;a< PeersGroupIdDevice.size();a++){
                            PeersGroupIdDevice pgid = PeersGroupIdDevice.get(a);
                            if (pgid.getMacAddressGo().equals(macAddressGoGruppoMigliore) && pgid.getGroupName().equals(groupNameGruppoMigliore)){
                                List <Peer> listaPeersGruppoMigliore = pgid.getListaPeers();
                                nrPeerTotGruppoMigliore = listaPeersGruppoMigliore.size();


                            }

                        }

                        // Calcolo dell'indice di variabilita' e della recency del gruppo migliore di questo device


                        int nrOccurrencesGreatestGroup = db.getNrOccurrencesGreatestGroup(macAddressGoGruppoMigliore, groupNameGruppoMigliore);
                        //Espresso in secondi
                        long totalLifeTimeGreatestGroup = db.getTotalLifeTimeGreatestGroup(macAddressGoGruppoMigliore, groupNameGruppoMigliore);
                        //Indice di varibilita' del gruppo migliore
                        double indiceVariabilita = nrOccurrencesGreatestGroup/totalLifeTimeGreatestGroup;

                        //Ritorna la Recency in secondi (Quanto recentemente ho visto il gruppo migliore di questo device)
                        long recency = db.getRecencyGreatestGroup(macAddressGoGruppoMigliore, groupNameGruppoMigliore);

                        // Aggiunta del device corrente con i relativi indici alla lista che poi dovra' essere normalizzata
                        DeviceWithParamGreatestGroup dwpgp = new DeviceWithParamGreatestGroup(device.deviceAddress,nrPeerTotGruppoMigliore,indiceVariabilita,recency);
                        listaDeviceWithParamGreatestGroup.add(dwpgp);


                        //Inserimento dei parametri del gruppo migliore di questo device nei vettori necessario per il successivo calcolo del valore massimo
                        // contenuto nei vettori
                        indiciDiVariabilita[j] = indiceVariabilita;
                        valoriDiRecency[j] =  recency;



                    }

                    j --;

                }

                if (wasThereAGoInThePast) {

                    // Se io non sono mai stato GO in passato, il mio indice di bonta' restera' pari a 0
                    double indiceDiBontaMio = 0;
                    // Creazione della lista dei device con i relativi indici di bonta'

                    for(int a=0;a< listaDeviceWithParamGreatestGroup.size();a++){
                        DeviceWithParamGreatestGroup dev = listaDeviceWithParamGreatestGroup.get(a);
                        String devAddress = dev.getDeviceAddress();
                        double nrTotPeersNorm = dev.getNrPeersTotGreatestGroup()/C;
                        double indVariabilitaNorm = dev.getIndVariabilitaGreatestGroup()/maxValueDoubleArray(indiciDiVariabilita);
                        double recencyNorm = dev.getRecencyGreatestGroup()/maxValueLongArray(valoriDiRecency);
                        //Indice di bonta' normalizzato tra 0 ed 1
                        double indiceDiBonta = (0.33 * nrTotPeersNorm) + (0.33 * indVariabilitaNorm) + (0.33 * recencyNorm);
                        if (devAddress.equals(MElist.get(0).deviceAddress)){
                            indiceDiBontaMio = indiceDiBonta;

                        }
                        DeviceConIndiceDiBonta dci = new DeviceConIndiceDiBonta(devAddress,indiceDiBonta);
                        listaDeviceConIndiceDiBonta.add(dci);

                    }



                    // Ordina la lista listaDeviceConIndiceDiBonta in base al campo double indiceDiBonta'

                    Collections.sort(listaDeviceConIndiceDiBonta, new comparatorDeviceConIndiceDiBonta());
                    double indiceDiBontaMiglioreInClassifica = listaDeviceConIndiceDiBonta.get(listaDeviceConIndiceDiBonta.size()-1).getIndiceBonta();
                    double indiceDiBontaPeggioreInClassifica = listaDeviceConIndiceDiBonta.get(0).getIndiceBonta();
                    //Calcolo la distanza tra me stesso ed il migliore in classifica
                    double d = indiceDiBontaMio - listaDeviceConIndiceDiBonta.get(listaDeviceConIndiceDiBonta.size()-1).getIndiceBonta();
                    //Calcolo la distanza tra l'ultimo in classifica ed il migliore in classifica
                    double dmax = indiceDiBontaPeggioreInClassifica - indiceDiBontaMiglioreInClassifica;
                    //Calcolo la distanza normalizzata
                    double dnorm = d/dmax;
                    // X e' una variabile di tipo double
                    X = -(Math.ceil(z * dnorm));
                    // x e' una variabile intera e rappresenta l'unita' di decremento del numero di cicli di discovePeer Z
                    x = (int)(X);
                    stopPeerDiscovery();
                    startProtocol(x);

                } else {

                    // Tra i peers scansionati non c'e' nessuno che e stato Go in passato quindi faccio ripartire il protocollo con x = 0
                    stopPeerDiscovery();
                    //Decremento z di una unita'
                    x = -1;
                    //Riavvia una nuova discovery decrementando Z
                    startProtocol(x);

                }

            }



        }



    };



    public void requestGroupInfo() {

        if (mManager != null) {
            mManager.requestGroupInfo(mChannel, groupInfoListener);
            Log.i(TAG, "Request Group info avvenuta con successo");
        }

    }

    private GroupInfoListener groupInfoListener = new GroupInfoListener() {
        @Override
        public void onGroupInfoAvailable(WifiP2pGroup group) {

            if (sonoGO) {
                WifiP2pDevice deviceGo = group.getOwner();
                Log.i(TAG, "Il GO del gruppo e': " + deviceGo.deviceName + "" + deviceGo.deviceAddress);
                clientList.clear();
                clientList.addAll(group.getClientList());
            }

        }
    };



    private ConnectionInfoListener connectionInfoListener = new ConnectionInfoListener() {

        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {

            // Out with the old, in with the new.

            // il metodo getDeviceList ritorna una Collection <WifiP2pDevice>
            //WifiP2pInfo InfoConnessione = new WifiP2pInfo();

            //InfoConnessione.groupFormed = info.groupFormed;
            //InfoConnessione.groupOwnerAddress = info.groupOwnerAddress;
            //InfoConnessione.isGroupOwner = info.isGroupOwner;


        }
    };


    public void requestConnectionInfo() {

        if (mManager != null) {
            mManager.requestConnectionInfo(mChannel,connectionInfoListener);
            Log.i(TAG, "Request Connection info OK");
        }

    }

    public void connect2() {
        // Picking the first device found on the network.

        WifiP2pDevice device = (WifiP2pDevice) deviceList.get(0);


        WifiP2pConfig config2 = new WifiP2pConfig();
        config2.deviceAddress = device.deviceAddress;
        config2.wps.setup = WpsInfo.PBC;
        config2.groupOwnerIntent = 15;

        mManager.connect(mChannel, config2, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.

                Log.i(WifidirectScan.TAG, "Connessione al GO avvenuta con successo");
                Toast.makeText(WifidirectScan.this, "Connessione al peer avvenuta con successo",Toast.LENGTH_SHORT).show();
                //stopPeerDiscovery();
                //WifiP2pInfo wifiP2pInfo2 = new WifiP2pInfo();
                //System.out.println(wifiP2pInfo2);
                //WifiP2pGroup wifiP2pGroup2 = new WifiP2pGroup();
                //System.out.println(wifiP2pGroup2);
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(WifidirectScan.this, "Connect failed. Retry on reason code nr.: " + reason,Toast.LENGTH_SHORT).show();
                Log.i(WifidirectScan.TAG, "Connessione fallita con reason code nr.: " + reason);
            }
        });
    }







    public void connect(WifiP2pConfig config) {
        // Picking the first device found on the network.

        WifiP2pDevice device = (WifiP2pDevice) PEERscanlist.get(0);

        WifiP2pConfig config2 = new WifiP2pConfig();
        config2.deviceAddress = device.deviceAddress;
        config2.wps.setup = WpsInfo.PBC;
        config2.groupOwnerIntent = 0;

        mManager.connect(mChannel, config2, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.

                Log.i(WifidirectScan.TAG, "Connessione al GO avvenuta con successo");
                Toast.makeText(WifidirectScan.this, "Connessione al peer avvenuta con successo",Toast.LENGTH_SHORT).show();
                //stopPeerDiscovery();
                //WifiP2pInfo wifiP2pInfo2 = new WifiP2pInfo();
                //System.out.println(wifiP2pInfo2);
                //WifiP2pGroup wifiP2pGroup2 = new WifiP2pGroup();
                //System.out.println(wifiP2pGroup2);
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(WifidirectScan.this, "Connect failed. Retry on reason code nr.: " + reason,Toast.LENGTH_SHORT).show();
                Log.i(WifidirectScan.TAG, "Connessione fallita con reason code nr.: " + reason);
            }
        });
    }




    public void cancelConnect() {

        //Cancel any ongoing p2p group negotiation

        WifiP2pDevice device = PEERscanlist.get(0);

        mManager.cancelConnect(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
                Log.i(WifidirectScan.TAG, "Disconnessione OK");
                Toast.makeText(WifidirectScan.this, "Disconnessione avvenuta con successo",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(WifidirectScan.this, "Disconnect failed. Retry.",Toast.LENGTH_SHORT).show();
            }
        });
    }




    //Start a p2p connection to a device with the specified configuration specificata
    //nell'oggetto WfiP2pConfig
    //If the current device is not part of a p2p group, a connect request initiates a group negotiation with the peer

    public void createGroup() {

        mManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

                Log.i(TAG, "CreateGroup OK ");
                Toast.makeText(WifidirectScan.this, "Gruppo creato con successo", Toast.LENGTH_SHORT).show();
                sonoGO = true;

            }

            @Override
            public void onFailure(int reasonCode) {
                Log.i(TAG, "CreateGroup NOT OK");
                String codiceErrore = String.valueOf(reasonCode);
                Log.i(TAG, codiceErrore);
            }
        });

    }

    // Metodo richiamato per rimuovere un gruppo quando sono in modalita' CLIENT:
    // Setta il flag connesso = false
    public void removeGroup() {

        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

                Log.i(TAG, "removeGroup OK");
                Toast.makeText(WifidirectScan.this, "Gruppo rimosso con successo", Toast.LENGTH_SHORT).show();
                // Inserisco il campo KEY_LAST_TIME_SEEN sul database relativo al gruppo rimosso
                db.updateGroupLastTimeSeen(group.getMacAddressGo(),group.getGroupName());
                connesso = false;
                aggGOblacklistPeriodicamente.stopAggiornaGOblacklistPeriodicamente();
                //Fa ripartire il protocollo con un nuovo numero di cicli di discover Peers
                N_disc = N_disc_casuale(5,15);
                z = N_disc;
                Log.i(TAG, "Il nuovo numero di cicli di discover Peers da eseguire e' pari a: " + N_disc);
                startProtocol(0);
            }

            @Override
            public void onFailure(int reasonCode) {
                Log.i(TAG, "removeGroup NOT OK");

            }
        }); ;

    }

    // Metodo richiamato per rimuovere un gruppo quando sono in modalita' GO:
    //Setta il flag sonoGO = false

    public void removeGroupDaGo() {

        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

                Log.i(TAG, "removeGroup OK");
                Toast.makeText(WifidirectScan.this, "Gruppo rimosso con successo", Toast.LENGTH_SHORT).show();
                // Inserisco il campo KEY_DESTROYED_AT sul database relativo al gruppo rimosso
                db.updateGroupDestroyedAt(group.getMacAddressGo(),group.getGroupName());
                sonoGO = false;
                // Stoppa il timerTask che aggiorna i client sullo stato del gruppo
                aggMemberTablePeriodicamente.stopAggiornaMemberTablePeriodicamente();
                // Stoppa il timerTask che valuta le condizioni per l'eventuale distruzione del gruppo
                evaluationGroupDestroyPeriodically.stopEvaluationGroupDestroy();
                //Fa ripartire il protocollo con un nuovo numero di cicli di discover Peers
                N_disc = N_disc_casuale(5,15);
                z = N_disc;
                Log.i(TAG, "Il nuovo numero di cicli di discover Peers da eseguire e' pari a: " + N_disc);
                startProtocol(0);


            }

            @Override
            public void onFailure(int reasonCode) {
                Log.i(TAG, "removeGroup NOT OK");

            }
        }); ;

    }



    @Override
    public void onDestroy ()
    {
        super.onDestroy();
        Toast.makeText(this, "Servizio distrutto", Toast.LENGTH_SHORT).show();
        unregisterReceiver(mReceiver);
        unregisterReceiver(receiverProva);
        Log.i(TAG, "Servizio Distrutto");
    }



    public IBinder onBind (Intent intent){

        throw new UnsupportedOperationException("Not yet implemented");
    }



    /**
     * Tal Metodo ritorna data e ora attuali
     * */
    private String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);

    }


    // Calcola la differenza tra due Date (sottoforma di stringhe) in millisecondi

    public void diffTraDueDate() {

        try {
            String strDate1 = "2009/08/02 12:35:05";
            String strDate2 = "2009/08/02 12:34:05";

            SimpleDateFormat fmt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss",  Locale.getDefault());
            fmt.setLenient(false);

            // Parses the two strings.
            Date d1 = fmt.parse(strDate1);
            Date d2 = fmt.parse(strDate2);

            // Calculates the difference in milliseconds.

            long millisDiff = d2.getTime() - d1.getTime();
            //System.out.println(millisDiff);
        }  catch (Exception e) {

            System.err.println(e);

        }

    }

    // Calcola la differenza tra due Date (sottoforma di stringhe) date in ingresso in millisecondi

    public long diffTraDueDate(String strDate1,String strDate2 ) {

        try {


            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            fmt.setLenient(false);

            // Parses the two strings.
            Date d1 = fmt.parse(strDate1);
            Date d2 = fmt.parse(strDate2);

            // Calculates the difference in milliseconds.

            long millisDiff = d2.getTime() - d1.getTime();
            //System.out.println(millisDiff);
            return millisDiff;

        }  catch (Exception e) {

            System.err.println(e);

        }
        return 0;

    }

    // Prende ingresso una data, la ritarda di t_exit secondi, restituisce in uscita la data aggiornata
    public String timeStampGOblacklistDelayed(String strDate1) {

        try {


            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            fmt.setLenient(false);

            // Parses the two strings.
            System.out.println(strDate1);
            Date d1 = fmt.parse(strDate1);

            // Calculates the difference in milliseconds.

            long millis = d1.getTime();
            System.out.println(millis);
            long millisDelayed = millis + t_exit;
            System.out.println(millisDelayed);
            Date now = new Date(millisDelayed);
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            String timeStampDelayed = df.format(now);
            System.out.println(timeStampDelayed);
            return timeStampDelayed;


        }  catch (Exception e) {

            System.err.println(e);

        }
        return "";

    }



    // Tale metodo setta il valore di N_disc tra un valore max e uno minimo dati in ingresso

    public static int N_disc_casuale(int N_disc_min, int N_disc_max) {
        Random rand = new Random();
        int N_disc_casuale = rand.nextInt((N_disc_max - N_disc_min) + 1) + N_disc_min;
        return N_disc_casuale;
    }

    // Tale metodo setta il valore di t_exit (tempo dopo il quale ci si disconnette da un gruppo espresso in millisecondi)
    // tra un valore max e uno minimo dati in ingresso

    public static int t_exit_casuale(int t_exit_min, int t_exit_max) {
        Random rand = new Random();
        int t_exit_casuale = rand.nextInt((t_exit_max - t_exit_min) + 1) + t_exit_min;
        return t_exit_casuale;
    }




    // Tale classe ritarda l'esecuzione del metodo requestGroupInfo di 20000 ms per concedere il
    // tempo di accettare la richiesta di connessione tramite il popup

    public class requestGroupInfoDelayed extends TimerTask {

        public void startRequestGroupInfoDelayed(){
            Timer timer = new Timer();
            timer.schedule(this,10000);
        }
        @Override
        public void run() {

            requestGroupInfo();


        }

    }

    // Tale classe ritarda l'esecuzione del metodo removeGroup del valore t_exit_casuale (espresso in millisecondi) ms e viene invocato da un device
    // CLIENT per disconnettersi da un gruppo

    public class removeGroupDelayed extends TimerTask {

        public void startRemoveGroupDelayed(){
            Timer timer = new Timer();
            timer.schedule(this,t_exit);
        }
        @Override
        public void run() {

            removeGroup();

        }

    }

    // Tale timerTask viene eseguito con un ritardo iniziale di t_union secondi, ed eseguito periodicamente ogni t_union
    // secondi; serve a valutare se sussisotno le condizioni per distruggere il gruppo o meno

    public class evaluationGroupDestroy extends TimerTask {

        Timer timer;

        public void startEvaluationGroupDestroy(){
            timer = new Timer();
            timer.schedule(this, t_union, t_union);
        }

        public void stopEvaluationGroupDestroy(){

            timer.cancel();
        }
        @Override
        public void run() {

            evaluationGroupDestroy();

        }

    }

    public void evaluationGroupDestroy(){

        Log.i(TAG, "TIMERTASK: Valutazione condizioni di distruzione del gruppo");
        //Ritorna una lista dei devices scansionati al momento attuale di nome deviceList
        requestPeersDaGo();
        // Pulizia delle liste memorizzate nei cicli precedenti
        GOscangolist.clear();
        PEERscangolist.clear();
        //deviceList e' la lista di peers attualmente scansionata
        for(int i=0;i<deviceList.size();i++){
            WifiP2pDevice device = deviceList.get(i);
            boolean isGroupOwner = device.isGroupOwner();
            if (isGroupOwner){

                GOscangolist.add(device);
                Log.i(WifidirectScan.TAG, "Il dispositivo " + device.deviceName + " e' stato aggiunto alla lista GOscangolist");


            }   else if (!isGroupOwner){

                PEERscangolist.add(device);
                Log.i(WifidirectScan.TAG, "Il dispositivo " + device.deviceName + " e' stato aggiunto alla lista PEERscangolist");


            }
        }

        Log.i(WifidirectScan.TAG, "Il numero di dispositivi contenuti nella GOscangoList e' di: " + GOscangolist.size());
        Log.i(WifidirectScan.TAG, "Il numero di dispopsitivi contenuti nella PEERscangoList e' di: " + PEERscangolist.size());


        if (GOscangolist.size() == 0) {

            //stopPeerDiscovery();
            Log.i(WifidirectScan.TAG, "Non ci sono altri GO nelle vicinanze,non ci sono valutazioni da fare, permango nello stato sonoGO ");



        } else if (GOscangolist.size() != 0) {


            // Ricevo la lista aggiornata dei client a me connessi
            Hashtable<String, ClientInfo> listaClient = (Hashtable) transmissionManager.getMemberTable();

            // Probabilita' di distruzione originaria del gruppo
            p = (1 - (listaClient.size() / C));

            double NrClientIpotizzatiPerGo = ((PEERscangolist.size() - listaClient.size()) / GOscangolist.size());

            // Scorro la lista dei GO scansionati e aggiungo il numero di Client ipotizzati per ogni GO creando una nuova lista GOscangolistConClient
            for (int i = 0; i < GOscangolist.size(); i++) {
                WifiP2pDevice device = GOscangolist.get(i);
                GoConClient goconclient = new GoConClient(NrClientIpotizzatiPerGo);
                goconclient.deviceAddress = device.deviceAddress;
                goconclient.deviceName = device.deviceName;
                GOscangolistConClient.add(goconclient);

            }

            // Aggiungo a me stesso il numero di Client a me connessi creando una nuova lista MElistConClient
            WifiP2pDevice ME = MElist.get(0);
            GoConClient meconclient = new GoConClient(listaClient.size());
            meconclient.deviceAddress = ME.deviceAddress;
            meconclient.deviceName = ME.deviceName;
            MElistConClient.add(meconclient);


            // Aggiungo alla lista GOscangolistConClient le informazioni su me stesso (memorizzate nella MElistConClient)
            GOscangolistnew.clear();
            GOscangolistnew.addAll(GOscangolistConClient);
            GOscangolistnew.addAll(MElistConClient);
            Log.i(WifidirectScan.TAG, "Il numero di device nella Goscangolistnew e' di: " + GOscangolistnew.size());

            // Ordina la lista GOscangolistnew creando una Classifica in cui Il GO migliore e' l'ultimo elemento
            // della lista ordinata

            Collections.sort(GOscangolistnew, new comparatorGoConClient());


            // Caso in cui Io sono il migliore in classifica
            if (meconclient.deviceAddress == GOscangolistnew.get(GOscangolistnew.size() - 1).deviceAddress) {

                // d e' la distanza tra me ed il secondo in classifica nel caso in cui Io sono il migliore
                double d = meconclient.getNrClient() - GOscangolistnew.get(GOscangolistnew.size() - 2).getNrClient();
                // d_max e' la distanza tra me e l'ultimo in classifica nel caso in cui Io sono il migliore
                double d_max = meconclient.getNrClient() - GOscangolistnew.get(0).getNrClient();
                // Torna il valore assoluto di d_max
                double val_abs_d_max = Math.abs(d_max);
                // Torna la distanza normalizzata
                double d_norm = d / val_abs_d_max;
                // Essendo il migliore DECREMENTO la mia probabilita' di distruzione originaria p secondo la formula seguente
                p_destr = p - (p * d_norm);


            }
            // Caso in cui Io non sono il migliore in classifica
            else if (meconclient.deviceAddress != GOscangolistnew.get(GOscangolistnew.size() - 1).deviceAddress) {

                // d e' la distanza tra me ed il migliore in classifica nel caso in cui Io non sono il migliore
                double d = meconclient.getNrClient() - GOscangolistnew.get(GOscangolistnew.size() - 1).getNrClient();
                // d_max e' la distanza tra l'ultimo in classifica ed il migliore in classifica
                double d_max = GOscangolistnew.get(0).getNrClient() - GOscangolistnew.get(GOscangolistnew.size() - 1).getNrClient();
                // Torna il valore assoluto di d_max
                double val_abs_d_max = Math.abs(d_max);
                // Torna la distanza normalizzata
                double d_norm = d / val_abs_d_max;

                // Non essendo il migliore in classifica INCREMENTO la mia probabilita' di distruzione originaria p secondo la formula seguente
                p_destr = p + ((1 - p) * Math.abs(d_norm));

            }

            // Tale metodo torna un numero double casuale h compreso tra 0.0 e 1.0
            Random r = new Random();
            // valore casuale tra 0.0 e 1.0 da confrontare con la p_destr
            double h = r.nextDouble();


            if (h <= p_destr) {

                //stopPeerDiscovery();
                // Tale metodo esegue la removeGroup settando il flag sonoGO = FALSE e fa ripartire il protocollo settando una nova z
                //removeGroupDaGo();


            }

        }

    }


    // Tale metodo rimuove Un GO dalla GOblacklist se sono trascorsi 60 secondi dalla data di aggiunta
    // alla lista

    public void aggiornaGOblacklist()  {

        Log.i(TAG, "TimerTask: Aggiornamento della lista GOblacklist ");
        Log.i(TAG, "I GO in blacklist sono: " + GOblacklist.size());
        for(int i=0;i<GOblacklist.size();i++){
            GOblacklist goblacklist = GOblacklist.get(i);
            //System.out.println(goblacklist);
            String addedAt = goblacklist.getAddedAt();
            long diffInMs = diffTraDueDate(addedAt,getDateTime());
            if (diffInMs > 30000) {

                GOblacklist.remove(goblacklist);
                Log.i(TAG, "Il GO " + goblacklist.deviceName + " e' stato rimosso dalla GOblacklist, e' di nuovo disponibile ");
            }
        }

    }



    // Tale classe esegue il metodo aggiornaGOblacklist periodicamente, ogni 30 secondi

    public class aggiornaGOblacklistPeriodicamente extends TimerTask {

        Timer timer = new Timer();
        public void startAggiornaGOblacklistPeriodicamente(){

            timer.schedule(this, 0, 30000);
        }

        public void stopAggiornaGOblacklistPeriodicamente(){

            timer.cancel();
        }
        @Override
        public void run() {

            aggiornaGOblacklist();

        }

    }

    // Tale classe esegue il metodo aggiornaMemberTableESpedisciAiClient periodicamente, ogni 10 secondi

    public class aggiornaMemberTablePeriodicamente extends TimerTask {

        Timer timer = new Timer();

        public void startAggiornaMemberTablePeriodicamente(){

            timer.schedule(this, 0, 10000);
        }

        public void stopAggiornaMemberTablePeriodicamente(){

            timer.cancel();
        }
        @Override
        public void run() {

            aggiornaMemberTableESpedisciAiClient();

        }


    }




    // Controlla se ci sono state disconessioni di qualche client e invia la memberTable aggiornata
    // a tutti i client

    public void aggiornaMemberTableESpedisciAiClient(){

        Log.i(TAG, "TimerTask: aggionra la membertable e spediscila ai client ");
        // Ritorna la lista dei client di nome clientList connessi al momento attuale
        requestGroupInfo();
        Hashtable<String,ClientInfo> memberTable;
        memberTable = transmissionManager.getMemberTable();
        if(clientList.size()< memberTable.size()) {
            boolean sendToAll = false;
            for (ClientInfo cli : memberTable.values()){
                boolean found = false;
                for(int a=0; a< clientList.size(); a++){
                    WifiP2pDevice device = clientList.get(a);
                    String mac_address = device.deviceAddress;
                    String device_name = device.deviceName;
                    if(cli.getMacAddress()== mac_address){
                        found = true;
                    }
                }
                if(!found) {

                    memberTable.remove(cli.getMacAddress());
                    sendToAll = true;
                }

            }
            if (sendToAll) {

                MembershipMessage membershipMessage0 = new MembershipMessage(0,memberTable);
                for (ClientInfo cli : memberTable.values()) {

                    try {

                        transmissionManager.sendReliable(membershipMessage0,cli.getInetAddress());
                    } catch (ReliableMessageNotSentException e) {
                        e.printStackTrace();
                    }


                }
            }

        }


    }



// Esegue il metodo discoverPeer posticipato del tempo t_union rispetto all'istante di invocazione
// del metodo

    public class discoverPeersDelayed extends TimerTask {

        public void startdiscoverPeerDelayed(){
            Timer timer = new Timer();
            timer.schedule(this,t_union);
        }
        @Override
        public void run() {

            discoverPeers();

        }

    }

// Implementa l'interfaccia Comparator, necessaria per l'ordinamento dell lista Goscangolistnew

    public class comparatorGoConClient implements Comparator <GoConClient> {
        @Override
        public int compare(GoConClient g1, GoConClient g2) {
            return g1.getNrClient().compareTo(g2.getNrClient());
        }


    }


// Implementa l'interfaccia Comparator, necessaria per l'ordinamento della lista PeersComuniGroupId

    public class comparatorPeersComuniGroupId implements Comparator <PeersComuniGroupId> {
        @Override
        public int compare(PeersComuniGroupId p1, PeersComuniGroupId p2) {
            return p1.getNrPeersComuni().compareTo(p2.getNrPeersComuni());
        }


    }

// Implementa l'interfaccia Comparator, necessaria per l'ordinamento della lista listaDeviceConIndicediBonta'

    public class comparatorDeviceConIndiceDiBonta implements Comparator <DeviceConIndiceDiBonta> {
        @Override
        public int compare(DeviceConIndiceDiBonta p1, DeviceConIndiceDiBonta p2) {
            return p1.getIndiceBonta().compareTo(p2.getIndiceBonta());
        }


    }


    // Tale metodo ritorna una Lista data dall'intersezione (elementi in comune) tra due Liste date in ingresso al metodo

    public List intersezioneTraDueListe(List a, List b){
        List listIntersectAB = new ArrayList(a);
        //Nella lista A tieni solamente  tutti gli elementi contenuti nella lista B
        listIntersectAB.retainAll(b);
        return listIntersectAB;
    }

    public double maxValueDoubleArray(double[] doubleArray) {
        double x[]= doubleArray;
        int i;
        double massimo;

        massimo=x[0];

        for(i=0; i<=x.length-1; i=i+1) {
            if( x[i]>massimo ) {
                massimo=x[i];
            }
        }
        return massimo;
        //System.out.println("Il massimo e' "+massimo);
    }

    public long maxValueLongArray(long[] longArray) {
        long x[]= longArray;
        int i;
        long massimo;

        massimo=x[0];

        for(i=0; i<=x.length-1; i=i+1) {
            if( x[i]>massimo ) {
                massimo=x[i];
            }
        }
        return massimo;
        //System.out.println("Il massimo e' "+massimo);
    }



}




