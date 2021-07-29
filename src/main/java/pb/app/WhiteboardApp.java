package pb.app;

import pb.WhiteboardServer;
import pb.managers.ClientManager;
import pb.managers.IOThread;
import pb.managers.PeerManager;
import pb.managers.ServerManager;
import pb.managers.endpoint.Endpoint;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;


/**
 * Initial code obtained from:
 * https://www.ssaurel.com/blog/learn-how-to-make-a-swing-painting-and-drawing-application/
 */
public class WhiteboardApp {
	private static Logger log = Logger.getLogger(WhiteboardApp.class.getName());
	
	/**
	 * Emitted to another peer to subscribe to updates for the given board. Argument
	 * must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String listenBoard = "BOARD_LISTEN";

	/**
	 * Emitted to another peer to unsubscribe to updates for the given board.
	 * Argument must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String unlistenBoard = "BOARD_UNLISTEN";

	/**
	 * Emitted to another peer to get the entire board data for a given board.
	 * Argument must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String getBoardData = "GET_BOARD_DATA";

	/**
	 * Emitted to another peer to give the entire board data for a given board.
	 * Argument must have format "host:port:boardid%version%PATHS".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardData = "BOARD_DATA";

	/**
	 * Emitted to another peer to add a path to a board managed by that peer.
	 * Argument must have format "host:port:boardid%version%PATH". The numeric value
	 * of version must be equal to the version of the board without the PATH added,
	 * i.e. the current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardPathUpdate = "BOARD_PATH_UPDATE";

	/**
	 * Emitted to another peer to indicate a new path has been accepted. Argument
	 * must have format "host:port:boardid%version%PATH". The numeric value of
	 * version must be equal to the version of the board without the PATH added,
	 * i.e. the current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardPathAccepted = "BOARD_PATH_ACCEPTED";

	/**
	 * Emitted to another peer to remove the last path on a board managed by that
	 * peer. Argument must have format "host:port:boardid%version%". The numeric
	 * value of version must be equal to the version of the board without the undo
	 * applied, i.e. the current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardUndoUpdate = "BOARD_UNDO_UPDATE";

	/**
	 * Emitted to another peer to indicate an undo has been accepted. Argument must
	 * have format "host:port:boardid%version%". The numeric value of version must
	 * be equal to the version of the board without the undo applied, i.e. the
	 * current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardUndoAccepted = "BOARD_UNDO_ACCEPTED";

	/**
	 * Emitted to another peer to clear a board managed by that peer. Argument must
	 * have format "host:port:boardid%version%". The numeric value of version must
	 * be equal to the version of the board without the clear applied, i.e. the
	 * current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardClearUpdate = "BOARD_CLEAR_UPDATE";

	/**
	 * Emitted to another peer to indicate an clear has been accepted. Argument must
	 * have format "host:port:boardid%version%". The numeric value of version must
	 * be equal to the version of the board without the clear applied, i.e. the
	 * current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardClearAccepted = "BOARD_CLEAR_ACCEPTED";

	/**
	 * Emitted to another peer to indicate a board no longer exists and should be
	 * deleted. Argument must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardDeleted = "BOARD_DELETED";

	/**
	 * Emitted to another peer to indicate an error has occurred.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardError = "BOARD_ERROR";
	
	/**
	 * White board map from board name to board object 
	 */
	Map<String,Whiteboard> whiteboards;

	/**
	 * White board map from board name to board owner endpoint
	 */
	Map<String,Endpoint> whiteboardPeers;

	/**
	 * Client map from client endpoint to listened board name
	 */
	Map<Endpoint, String> clientEndpoints;
	
	/**
	 * The currently selected white board
	 */
	Whiteboard selectedBoard = null;
	
	/**
	 * The peer:port string of the peer. This is synonomous with IP:port, host:port,
	 * etc. where it may appear in comments.
	 */
	String peerport = "standalone"; // a default value for the non-distributed version
	String whiteboardServerHost = "standalone"; // a default value for the whiteboard server host
	String whiteboardServerPort = "standalone"; // a default value for the whiteboard server port

	/**
	 * The port number used for this APP to set up whiteboard share peer
	 */
	private final int asServerPort;

	/**
	 * The flag used to record a self emit when update
	 */
	private boolean selfEmit = false;

	/**
	 * The client manager is for setting up connections with whiteboard server.
	 */
	private PeerManager peerManager = null;
	private ClientManager serverConnection = null;
	private Endpoint serverEndpoint = null;
	/*
	 * GUI objects, you probably don't need to modify these things... you don't
	 * need to modify these things... don't modify these things [LOTR reference?].
	 */
	
	JButton clearBtn, blackBtn, redBtn, createBoardBtn, deleteBoardBtn, undoBtn;
	JCheckBox sharedCheckbox ;
	DrawArea drawArea;
	JComboBox<String> boardComboBox;
	boolean modifyingComboBox=false;
	boolean modifyingCheckBox=false;
	
	/**
	 * Initialize the white board app.
	 */
	public WhiteboardApp(int peerPort,String whiteboardServerHost, 
			int whiteboardServerPort) {

		// record local whiteboards and remote whiteboard shared with this peer
		whiteboards = new HashMap<>();
		whiteboardPeers = new HashMap<>();
		clientEndpoints = new ConcurrentHashMap<>();

		// record the IP address of the whiteboard server
		this.asServerPort = peerPort;
		this.whiteboardServerHost = whiteboardServerHost;
		this.whiteboardServerPort = Integer.toString(whiteboardServerPort);
		this.peerport = "127.0.0.1:"+asServerPort;
	}
	
	/******
	 * 
	 * Utility methods to extract fields from argument strings.
	 * 
	 ******/
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return peer:port:boardid
	 */
	public static String getBoardName(String data) {
		String[] parts=data.split("%",2);
		return parts[0];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return boardid%version%PATHS
	 */
	public static String getBoardIdAndData(String data) {
		String[] parts=data.split(":");
		return parts[2];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return version%PATHS
	 */
	public static String getBoardData(String data) {
		String[] parts=data.split("%",2);
		return parts[1];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return version
	 */
	public static long getBoardVersion(String data) {
		String[] parts=data.split("%",3);
		return Long.parseLong(parts[1]);
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return PATHS
	 */
	public static String getBoardPaths(String data) {
		String[] parts=data.split("%",3);
		return parts[2];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return peer
	 */
	public static String getIP(String data) {
		String[] parts=data.split(":");
		return parts[0];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return port
	 */
	public static int getPort(String data) {
		String[] parts=data.split(":");
		return Integer.parseInt(parts[1]);
	}
	
	/******
	 * 
	 * Methods called from events.
	 * 
	 ******/

	/**
	 * Connect to the whiteboard server, trigger asPeer() when receive a sharing board notification
	 */
	public void connectWhiteboardSever () {
		// From whiteboard server
		int serverPort = Integer.parseInt(whiteboardServerPort);

		try {
			serverConnection = peerManager.connect(serverPort, whiteboardServerHost);

			// Connect to the whiteboard server
			serverConnection.on(PeerManager.peerStarted, (eventArgs) -> {
				Endpoint endpoint = (Endpoint) eventArgs[0];
				serverEndpoint = endpoint;
				// Blinding to SHARING_BOARD message or UNSHARING_BOARD

				endpoint.on(WhiteboardServer.sharingBoard, (Args) -> {
					String peerBoard = (String) Args[0];
					log.info("Receiving sharing board from whiteboard server");
					connectPeerBoard(peerBoard);
				}).on(WhiteboardServer.unsharingBoard, (Args) -> {
					String peerBoard = (String) Args[0];
					log.info("Removing whiteboard: " + peerBoard);
					deleteBoard(peerBoard);
					log.info("Whiteboard: " + peerBoard + " removed");
				}).on(WhiteboardServer.error, (Args) -> {
					log.info("Whiteboard Server error detected.");
					System.out.println((String) Args[0]);
				});
			}).on(PeerManager.peerStopped, (eventArgs)->{
				Endpoint endpoint = (Endpoint)eventArgs[0];
				log.info("Whiteboard server connection session stopped: " + endpoint.getOtherEndpointId());
				System.out.println("Disconnected from whiteboard server: " + endpoint.getOtherEndpointId());
			}).on(PeerManager.peerError, (eventArgs)->{
				Endpoint endpoint = (Endpoint)eventArgs[0];
				log.warning("Whiteboard server connection session error: " + endpoint.getOtherEndpointId());
				System.out.println("There was error while communication with whiteboard server: "
						+endpoint.getOtherEndpointId());
			});
			show(peerport);
			serverConnection.start();
			serverConnection.join();

		} catch (UnknownHostException e) {
			log.info("The whiteboard server could not be found: " + whiteboardServerHost + ":" + whiteboardServerPort);
			System.out.println("The whiteboard server could not be found: " + whiteboardServerHost + ":" + whiteboardServerPort);
		} catch (InterruptedException e) {
			log.warning("Interrupted while trying to connect with whiteboard server.");
			System.out.println("Interrupted while trying to connect with whiteboard server.");
		}
	}


	/**
	 * Set up a connection with peer board, and blind the whiteboard events
	 * @param peerBoard = peer:port:boardid
	 */
	public void connectPeerBoard(String peerBoard){
		// From whiteboard peer
		String host = getIP(peerBoard);
		int port = getPort(peerBoard);

		try {
			// connecting with peer board, return client manager
			ClientManager peerConnection = peerManager.connect(port, host);

			// listen to the whiteboard peer event when connection established
			peerConnection.on(PeerManager.peerStarted, (eventArgs) -> {
				Endpoint endpoint = (Endpoint) eventArgs[0];
				// listen to the whiteboard peers' operation or query update
				endpoint.on(boardData, (Args) -> {
					// listen to BOARD_DATA then update the whiteboard
					Whiteboard peerWhiteboard = whiteboards.get(peerBoard);
					String currentBoard = getBoardName((String) Args[0]);
					String peerData = getBoardData((String) Args[0]);
					// Check if gets the correct board
					if (!currentBoard.equals(peerBoard)) {
						endpoint.emit(getBoardData, peerBoard);
						log.warning("Incorrect board received.");
						log.warning("Required board: " + peerBoard + "while received board" + currentBoard);
						return;
					}
					peerWhiteboard.whiteboardFromString(peerBoard, peerData);
					whiteboards.replace(currentBoard,whiteboards.get(currentBoard), peerWhiteboard);
					log.info("Getting new whiteboard data");

				}).on(boardPathUpdate, (Args) -> {
					if (selfEmit){
						selfEmit = false;
						return;
					}
					// listen to BOARD_PATH_UPDATE
					long version = getBoardVersion((String) Args[0]);
					String boardName = getBoardName((String) Args[0]);
					Whiteboard board = whiteboards.get(boardName);
					if (whiteboards.containsKey(boardName)&&board!=null) {
						WhiteboardPath newPath = new WhiteboardPath(getBoardPaths((String) Args[0]));
						//find the board

						////add the newPath data to that board and update the version
						if (board.addPath(newPath,version)) {
							endpoint.emit(boardPathAccepted, (String) Args[0]);
							System.out.println("get path update of: "+(String) Args[0]);
							if (selectedBoard.getName().equals(board.getName())) {
								selectedBoard.draw(drawArea);
							}
						} else {
							endpoint.emit(boardError, "Add path failed:" + (String) Args[0]);
						}
						whiteboards.replace(boardName, board);
					}
				}).on(boardPathAccepted, (Args) -> {
					// New board path accepted
					System.out.print("new path add successfully on board" + (String) Args[0]);
				}).on(boardUndoUpdate, (Args) -> {
					if (selfEmit){
						selfEmit = false;
						return;
					}
					// Board path undo query
					long version = getBoardVersion((String) Args[0]);
					String boardName = getBoardName((String) Args[0]);
					//find the board
					Whiteboard board = whiteboards.get(boardName);
					if (board!=null) {					
						////add the newPath data to that board and update the version
						if (board.undo(version)) {
							endpoint.emit(boardUndoAccepted, (String) Args[0]);
							System.out.println("get path undo of: "+(String) Args[0]);
							if (selectedBoard.getName().equals(board.getName())) {
								selectedBoard.draw(drawArea);
							}
						} else {
							endpoint.emit(boardError, "undo path failed:" + (String) Args[0]);
						}
						whiteboards.replace(boardName, board);
					}
				}).on(boardUndoAccepted, (Args) -> {
					// Undo Update Accepted
					System.out.print("undo successfully on board" + (String) Args[0]);
				}).on(boardClearUpdate, (Args) -> {
					if (selfEmit){
						selfEmit = false;
						return;
					}
					// Clear Update query
					long version = getBoardVersion((String) Args[0]);
					String boardName = getBoardName((String) Args[0]);
					Whiteboard board = whiteboards.get(boardName);
					if (board!=null) {
						//find the board
						
						////add the newPath data to that board and update the version
						if (board.clear(version)) {
							endpoint.emit(boardClearAccepted, (String) Args[0]);
							System.out.println("get path clear of: "+(String) Args[0]);
							if (selectedBoard.getName().equals(board.getName())) {
								selectedBoard.draw(drawArea);
							}
						} else {
							endpoint.emit(boardError, "clear path failed:" + (String) Args[0]);
						}
						whiteboards.replace(boardName, board);
					}
				}).on(boardClearAccepted, (Args) -> {
					// Clear Update Accepted
					System.out.print("clear successfully on board" + (String) Args[0]);
				}).on(boardError, (Args) -> {
					// Board error occurred
					log.info("whiteboard operation failed");
					String boardDetail = (String) Args[0];
					String boardName = getBoardName(boardDetail);
					endpoint.emit(getBoardData, boardName);
					log.info("trying to re-synchronize the whiteboard data");
				}).on(boardDeleted, (Args) -> {
					// Board Deleted
					String boardname=(String) Args[0];
					log.info("deleting board " + boardname);
					if (whiteboards.containsKey(boardname)&&whiteboards.get(boardname)!=null) {
						deleteBoard(boardname);
					}else {
						log.info("board: " + boardname + " does not exist.");
					}
					System.out.print("whiteboard " + (String) Args[0] + " has been deleted.");
					peerConnection.shutdown();
					log.info("whiteboard " + boardname + " connection shutting down");
				});

				// emit BOARD_LISTEN to peer, request for whiteboard update
				endpoint.emit(listenBoard, peerBoard);
				//	check if has already haven the board, if not, create one board
				if (whiteboards.get(peerBoard)!=null) {
					log.info("Duplicated remote whiteboard received: " + peerBoard);
				} else {
					if(peerBoard!=null) {
						Whiteboard peerWhiteboard = new Whiteboard(peerBoard, true);
						addBoard(peerWhiteboard, false);
						synchronized (whiteboardPeers) {
							whiteboardPeers.put(peerBoard, endpoint);
						}
						log.info("new board added: " + peerBoard);
						log.info("querying a new whiteboard " + peerBoard);
						// emit GET_BOARD_DATA
						endpoint.emit(getBoardData, peerBoard);
						log.info("getting data for whiteboard " + peerBoard);
					}
				}
			}).on(PeerManager.peerStopped, (args)->{
				Endpoint endpoint = (Endpoint)args[0];
				log.info("Peer connection session stopped: " + endpoint.getOtherEndpointId());
				System.out.println("Disconnected from whiteboard peer: " + endpoint.getOtherEndpointId());
			}).on(PeerManager.peerError, (args)->{
				Endpoint endpoint = (Endpoint)args[0];
				log.warning("Peer connection session error: " + endpoint.getOtherEndpointId());
				System.out.println("There was error while communication with whiteboard peer: " +endpoint.getOtherEndpointId());
			});
			peerConnection.start();

		} catch (UnknownHostException e) {
			log.info("The remote whiteboard could not be found: " + whiteboardServerHost + ":" + whiteboardServerPort);
			System.out.println("The remote whiteboard could not be found: " + whiteboardServerHost + ":" + whiteboardServerPort);
		} catch (InterruptedException e) {
			log.warning("Interrupted while trying to connect with remote whiteboard.");
			System.out.println("Interrupted while trying to connect with remote whiteboard.");
		}
	}

	/**
	 * Method broadcast self whiteboard updates to others
	 * @param sourceClient - original update client (null if the update source is the whiteboard owner)
	 * @param whiteboard - the whiteboard need to be updated
	 * @param boardUpdates - the update contents of the whiteboard
	 * @param updateMessage - the type event names of this update
	 */
	public void broadcastUpdate(Endpoint sourceClient, Whiteboard whiteboard, String boardUpdates, String updateMessage){
		// emitting whiteboard updates to all listening clients
		synchronized(clientEndpoints) {
			log.info("broadcasting new updates");
			if (sourceClient != null) {
				clientEndpoints.remove(sourceClient, whiteboard.getName());
				clientEndpoints.forEach((client, boardName) -> {
					if(boardName.equals(whiteboard.getName())) {
						selfEmit = true;
						client.emit(updateMessage, boardUpdates);
						log.info("emitting board updates " + boardUpdates + "to all clients " + client.getOtherEndpointId());
					}
				});
				clientEndpoints.put(sourceClient, whiteboard.getName());
			}else {
				clientEndpoints.forEach((client, boardName) -> {
					if(boardName.equals(whiteboard.getName())) {
						selfEmit = true;
						client.emit(updateMessage, boardUpdates);
						log.info("emitting board updates " + boardUpdates + "to all clients " + client.getOtherEndpointId());
					}
				});
			}
			
		}
	}
	
	
	/******
	 * 
	 * Methods to manipulate data locally. Distributed systems related code has been
	 * cut from these methods.
	 * 
	 ******/
	
	/**
	 * Wait for the peer manager to finish all threads.
	 */
	public void waitToFinish() {
		// create new peer manager
		peerManager = new PeerManager(asServerPort);

		// listen for the peer started event for setting up connection with peers
		peerManager.on(PeerManager.peerStarted, (eventArgs)->{
			Endpoint endpoint = (Endpoint) eventArgs[0];
			System.out.println("Connection from peer: "+endpoint.getOtherEndpointId());

			endpoint.on(listenBoard, (Args) -> {
				// listen to listen board query
				synchronized (clientEndpoints) {
					clientEndpoints.put(endpoint, (String) Args[0]);
				}
				log.info("peer " + endpoint.getOtherEndpointId() + " is now listen to whiteboard: " + (String) Args[0]);
			}).on(unlistenBoard, (Args) ->{
				// unlisten to listen board query
				synchronized (clientEndpoints) {
					clientEndpoints.remove(endpoint, (String) Args[0]);
				}
				log.info("peer " + endpoint.getOtherEndpointId() + " is now unlisten to whiteboard: " + (String) Args[0]);
			}).on(getBoardData, (Args) -> {
				// listen to get board data query
				String boardName=(String)Args[0];
				System.out.println("emitting board " + boardName + " data to "+ endpoint.getOtherEndpointId());
				Whiteboard board=whiteboards.get(boardName);
				if (board!=null) {
					endpoint.emit(boardData, board.toString());
					System.out.println("board" + boardName + " data send.");
				}
			}).on(boardPathUpdate, (Args) -> {
				if (selfEmit){
					selfEmit = false;
					return;
				}
				// listen to BOARD_PATH_UPDATE
				long version = getBoardVersion((String) Args[0]);
				String boardName = getBoardName((String) Args[0]);
				WhiteboardPath newPath = new WhiteboardPath(getBoardPaths((String) Args[0]));
				Whiteboard board = whiteboards.get(boardName);
				if (whiteboards.containsKey(boardName)&& board!=null) {				
					//find the board
					////add the newPath data to that board and update the version
					if (board.addPath(newPath, version)) {
						endpoint.emit(boardPathAccepted, (String) Args[0]);
						System.out.println("get path update of: "+(String) Args[0]);
						if (selectedBoard.getName().equals(board.getName())) {
							selectedBoard.draw(drawArea);
						}
						if (!board.isRemote()) {
							broadcastUpdate(endpoint, board, (String) Args[0], boardPathUpdate);
						}
					} else {
						endpoint.emit(boardError, "Add path failed:" + (String) Args[0]);
					}
					whiteboards.replace(boardName, board);
				}
			}).on(boardPathAccepted, (Args) -> {
				// New board path accepted
				System.out.print("new path " + (String) Args[0] + " is added successfully on client: " + endpoint.getOtherEndpointId());
			}).on(boardUndoUpdate, (Args) -> {
				if (selfEmit){
					selfEmit = false;
					return;
				}
				// Board path undo query
				long version = getBoardVersion((String) Args[0]);
				String boardName = getBoardName((String) Args[0]);
				Whiteboard board = whiteboards.get(boardName);
				if (board!=null) {
					//find the board
					////add the newPath data to that board and update the version
					if (board.undo(version)) {
						endpoint.emit(boardUndoAccepted, (String) Args[0]);
						System.out.println("get path undo of: "+(String) Args[0]);
						if (selectedBoard.getName().equals(board.getName())) {
							selectedBoard.draw(drawArea);
						}
						if (!board.isRemote()) {
							broadcastUpdate(endpoint, board, (String) Args[0], boardUndoUpdate);
						}
					} else {
						endpoint.emit(boardError, "undo path failed:" + (String) Args[0]);
					}
					whiteboards.replace(boardName, board);
				}
			}).on(boardUndoAccepted, (Args) -> {
				// Undo Update Accepted
				System.out.print("undo " + (String) Args[0] + "  successfully on client" + endpoint.getOtherEndpointId());
			}).on(boardClearUpdate, (Args) -> {
				if (selfEmit){
					selfEmit = false;
					return;
				}
				// Clear Update query
				long version = getBoardVersion((String) Args[0]);
				String boardName = getBoardName((String) Args[0]);
				//find the board
				Whiteboard board = whiteboards.get(boardName);
				if (board!=null) {
					////add the newPath data to that board and update the version
					if (board.clear(version)) {
						endpoint.emit(boardClearAccepted, (String) Args[0]);
						System.out.println("get path clear of: "+(String) Args[0]+version+board.getVersion());
						if (selectedBoard.getName().equals(board.getName())) {
							selectedBoard.draw(drawArea);
						}
						if (!board.isRemote()) {
							broadcastUpdate(endpoint, board, (String) Args[0], boardClearUpdate);
						}
					} else {
						endpoint.emit(boardError, "clear path failed:" + (String) Args[0]);
					}
					whiteboards.replace(boardName, board);
				}
			}).on(boardClearAccepted, (Args) -> {
				// Clear Update Accepted
				System.out.print("clear " + (String) Args[0] + "  successfully on client" + endpoint.getOtherEndpointId());
			}).on(boardError, (Args) -> {
				// Board error occupied
				String errorMessage = ((String) Args[0]).split(":", 2)[0];
				String boardDetail = ((String) Args[0]).split(":", 2)[1];
				String boardName = getBoardName(boardDetail);
				Whiteboard board=whiteboards.get(boardName);
				log.severe("Error raised on board: " + boardName);
				System.out.println(errorMessage + "from client " + endpoint.getOtherEndpointId());
				if(board != null) {
					log.info("Re-transmit all data of board: " + boardName);
					Endpoint whiteboardPeer = whiteboardPeers.get(board.getName());
					if(whiteboardPeer!=null) {
						whiteboardPeer.emit(boardData, board.toString());
					}else {
						System.out.print("Transmitting board data");
					}
				} else {
					log.info("Inform whiteboard server not such board " + boardName + " is sharing");
					serverEndpoint.emit(WhiteboardServer.unshareBoard, boardName);
				}
			});

		}).on(PeerManager.peerStopped, (eventArgs)->{
			Endpoint endpoint = (Endpoint) eventArgs[0];
			System.out.println("Disconnected from peer: "+endpoint.getOtherEndpointId());
		}).on(PeerManager.peerError, (eventArgs)->{
			Endpoint endpoint = (Endpoint) eventArgs[0];
			System.out.println("There was an error communicating with the peer: "
					+endpoint.getOtherEndpointId());
		}).on(PeerManager.peerServerManager, (eventArgs)->{
			ServerManager serverManager = (ServerManager) eventArgs[0];
			serverManager.on(IOThread.ioThread, (Args)->{
				this.peerport = (String) Args[0];
			});
		});
		
		peerManager.start();
		connectWhiteboardSever();
	}
	
	/**
	 * Add a board to the list that the user can select from. If select is
	 * true then also select this board.
	 * @param whiteboard
	 * @param select
	 */
	public void addBoard(Whiteboard whiteboard,boolean select) {
		synchronized(whiteboards) {
			whiteboards.put(whiteboard.getName(), whiteboard);
		}
		updateComboBox(select?whiteboard.getName():null);
	}
	
	/**
	 * Delete a board from the list.
	 * @param boardname must have the form peer:port:boardid
	 */
	public void deleteBoard(String boardname) {
		synchronized(whiteboards) {
			Whiteboard whiteboard = whiteboards.get(boardname);
			if(whiteboard!=null) {
				whiteboards.remove(boardname);
				log.info("whiteboard deleted : " + boardname);
				// further operation to the board owner
				if (whiteboard.isRemote()) {
					Endpoint whiteboardPeer = whiteboardPeers.get(boardname);
					if (whiteboardPeer!=null)
						{
						whiteboardPeer.emit(unlistenBoard, boardname);
						log.info("Unlistening peer whiteboards");
						// delete records on whiteboard peer
						synchronized (whiteboardPeers) {
							whiteboardPeers.remove(boardname);
							log.info("remove board record " + boardname);
						}
						log.info("Unlistening board " + boardname);
					}
				} else {
					// unshare sharing boards
					if (whiteboard.isShared()) {
						serverEndpoint.emit(WhiteboardServer.unshareBoard, boardname);
						log.info("Unsharing board " + boardname);
					}
				}
				// delete client record on clientEndpoint
				synchronized (clientEndpoints) {
					ArrayList<Endpoint> deletedBoards = new ArrayList<>();
					for (Endpoint endpoint : clientEndpoints.keySet()){
						String name = clientEndpoints.get(endpoint);
						if (boardname.equals(name)) {
							deletedBoards.add(endpoint);
							System.out.println("Deleting board " + name);
							log.info("Emitting client " + endpoint.getOtherEndpointId() + " to delete whiteboard " + name);
							endpoint.emit(boardDeleted, name);
							log.info("Informing whiteboard clients");
						}
					}

					for (Endpoint client : deletedBoards) {
						clientEndpoints.remove(client);
						log.info("Deleting client records" + client.getOtherEndpointId() );
					}
				}
			}
		}
		updateComboBox(null);
	}
	
	/**
	 * Create a new local board with name peer:port:boardid.
	 * The boardid includes the time stamp that the board was created at.
	 */
	public void createBoard() {
		String name = peerport+":board"+Instant.now().toEpochMilli();
		Whiteboard whiteboard = new Whiteboard(name,false);
		addBoard(whiteboard,true);
	}

	/**
	 * Add a path to the selected board. The path has already
	 * been drawn on the draw area; so if it can't be accepted then
	 * the board needs to be redrawn without it.
	 * @param currentPath
	 */
	public void pathCreatedLocally(WhiteboardPath currentPath) {
		if(selectedBoard!=null) {
			String whiteboardUpdate = selectedBoard.getName()+"%"+(selectedBoard.getVersion())+ '%' + currentPath.toString();//selectedBoard.getNameAndVersion() + '%' + currentPath.toString();
			Endpoint whiteboardPeer = whiteboardPeers.get(selectedBoard.getName());
			
			Whiteboard temp=selectedBoard;
			if(!selectedBoard.addPath(currentPath,selectedBoard.getVersion())) {
				// some other peer modified the board in between
				drawSelectedWhiteboard();
				log.warning("Receiving wrong updates, re-synchronizing board " + selectedBoard.getName());
				whiteboardPeer.emit(boardData, selectedBoard.toString());
			} else {
				// was accepted locally, so do remote stuff if needed
				drawSelectedWhiteboard();
				if(selectedBoard.isRemote()) {
					selfEmit = true;
					whiteboardPeer.emit(boardPathUpdate, whiteboardUpdate);
					log.info("Emitting board update to the remote whiteboard owner");
				}else {
					broadcastUpdate(null, temp, whiteboardUpdate, boardPathUpdate);
					log.info("Broadcasting new updates on board " + selectedBoard.getName());
				}
			}
		} else {
			log.severe("Path created without a selected board: "+currentPath);
		}
	}
	
	/**
	 * Clear the selected whiteboard.
	 */
	public void clearedLocally() {
		if(selectedBoard!=null) {
			String whiteboardUpdate = selectedBoard.getNameAndVersion() + '%';
			Endpoint whiteboardPeer = whiteboardPeers.get(selectedBoard.getName());
			
			Whiteboard temp=selectedBoard;
			if(!selectedBoard.clear(selectedBoard.getVersion())) {
				// some other peer modified the board in between
				drawSelectedWhiteboard();
				log.warning("Receiving wrong updates, re-synchronizing board " + selectedBoard.getName());
				whiteboardPeer.emit(boardData, selectedBoard.toString());
			} else {
				// was accepted locally, so do remote stuff if needed
				drawSelectedWhiteboard();
				if(selectedBoard.isRemote()) {
					selfEmit = true;
					whiteboardPeer.emit(boardClearUpdate, whiteboardUpdate);
					log.info("Emitting board clear update to the remote whiteboard owner");
				}else {
					broadcastUpdate(null, temp, whiteboardUpdate, boardClearUpdate);
					log.info("Broadcasting new clear update on board " + selectedBoard.getName());
				}
			}
		} else {
			log.severe("Cleared without a selected board");
		}
	}

	/**
	 * Undo the last path of the selected whiteboard.
	 */
	public void undoLocally() {
		if(selectedBoard!=null) {
			String whiteboardUpdate = selectedBoard.getNameAndVersion() + '%';
			Endpoint whiteboardPeer = whiteboardPeers.get(selectedBoard.getName());
			
			Whiteboard temp=selectedBoard;
			if(!selectedBoard.undo(selectedBoard.getVersion())) {
				// some other peer modified the board in between
				drawSelectedWhiteboard();
				log.warning("Receiving wrong updates, re-synchronizing board " + selectedBoard.getName());
				whiteboardPeer.emit(boardData, selectedBoard.toString());
			} else {
				// was accepted locally, so do remote stuff if needed
				drawSelectedWhiteboard();
				if(selectedBoard.isRemote()) {
					selfEmit = true;
					whiteboardPeer.emit(boardUndoUpdate, whiteboardUpdate);
					log.info("Emitting board undo update to the remote whiteboard owner");
				}else {
					broadcastUpdate(null, temp, whiteboardUpdate, boardUndoUpdate);
					log.info("Broadcasting new undo update on board " + selectedBoard.getName());
				}
			}
		} else {
			log.severe("Undo without a selected board");
		}
	}
	
	/**
	 * The variable selectedBoard has been set.
	 */
	public void selectedABoard() {
		drawSelectedWhiteboard();
		log.info("selected board: "+selectedBoard.getName());
	}
	
	/**
	 * Set the share status on the selected board.
	 */
	public void setShare(boolean share) {
		if(selectedBoard!=null && serverEndpoint != null) {
        	selectedBoard.setShared(share); 
        	////emit shareBoard to WhiteboardServer
        	String boardname=selectedBoard.getName();
        	if (share) {
        		log.info("board sharing");
        		serverEndpoint.emit(WhiteboardServer.shareBoard, boardname);
        	}else {
        		log.info("board unsharing");
        		serverEndpoint.emit(WhiteboardServer.unshareBoard, boardname);
        	}
        } else {
        	log.severe("board setShare error: no board selected or server connection failed");
        }
	}
	
	/**
	 * Called by the gui when the user closes the app.
	 */
	public void guiShutdown() {
		// cleanup all listened boards
		log.info("Unlistening peer whiteboards");
		synchronized (whiteboardPeers) {
			for (String name : whiteboardPeers.keySet()){
				System.out.println("Deleting board " + name);
				log.info("Emitting unlisten whiteboard " + name);
				whiteboardPeers.get(name).emit(unlistenBoard, name);
				
			}
			whiteboardPeers.clear();
		}

		HashSet<Whiteboard> existingBoards= new HashSet<>(whiteboards.values());
		existingBoards.forEach((board)->{
			deleteBoard(board.getName());
		});
    	whiteboards.values().forEach((whiteboard)->{
    		serverEndpoint.emit(WhiteboardServer.unshareBoard, whiteboard.getName());
			log.info("Unsharing board " +  whiteboard.getName());	
    	});
    	whiteboards.clear();
	
    	// exist program
		if(serverEndpoint != null) {
			peerManager.shutdown();

		} else {
			System.exit(0);
		}
	}
	
	

	/******
	 * 
	 * GUI methods and callbacks from GUI for user actions.
	 * You probably do not need to modify anything below here.
	 * 
	 ******/
	
	/**
	 * Redraw the screen with the selected board
	 */
	public void drawSelectedWhiteboard() {
		drawArea.clear();
		if(selectedBoard!=null) {
			selectedBoard.draw(drawArea);
		}
	}
	
	/**
	 * Setup the Swing components and start the Swing thread, given the
	 * peer's specific information, i.e. peer:port string.
	 */
	public void show(String peerport) {
		// create main frame
		JFrame frame = new JFrame("Whiteboard Peer: "+peerport);
		Container content = frame.getContentPane();
		// set layout on content pane
		content.setLayout(new BorderLayout());
		// create draw area
		drawArea = new DrawArea(this);

		// add to content pane
		content.add(drawArea, BorderLayout.CENTER);

		// create controls to apply colors and call clear feature
		JPanel controls = new JPanel();
		controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));

		/**
		 * Action listener is called by the GUI thread.
		 */
		ActionListener actionListener = new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == clearBtn) {
					clearedLocally();
				} else if (e.getSource() == blackBtn) {
					drawArea.setColor(Color.black);
				} else if (e.getSource() == redBtn) {
					drawArea.setColor(Color.red);
				} else if (e.getSource() == boardComboBox) {
					if(modifyingComboBox) return;
					if(boardComboBox.getSelectedIndex()==-1) return;
					String selectedBoardName=(String) boardComboBox.getSelectedItem();
					if(whiteboards.get(selectedBoardName)==null) {
						log.severe("selected a board that does not exist: "+selectedBoardName);
						return;
					}
					selectedBoard = whiteboards.get(selectedBoardName);
					// remote boards can't have their shared status modified
					if(selectedBoard.isRemote()) {
						sharedCheckbox.setEnabled(false);
						sharedCheckbox.setVisible(false);
					} else {
						modifyingCheckBox=true;
						sharedCheckbox.setSelected(selectedBoard.isShared());
						modifyingCheckBox=false;
						sharedCheckbox.setEnabled(true);
						sharedCheckbox.setVisible(true);
					}
					selectedABoard();
				} else if (e.getSource() == createBoardBtn) {
					createBoard();
				} else if (e.getSource() == undoBtn) {
					if(selectedBoard==null) {
						log.severe("there is no selected board to undo");
						return;
					}
					undoLocally();
				} else if (e.getSource() == deleteBoardBtn) {
					if(selectedBoard==null) {
						log.severe("there is no selected board to delete");
						return;
					}
					deleteBoard(selectedBoard.getName());
				}
			}
		};
		
		clearBtn = new JButton("Clear Board");
		clearBtn.addActionListener(actionListener);
		clearBtn.setToolTipText("Clear the current board - clears remote copies as well");
		clearBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		blackBtn = new JButton("Black");
		blackBtn.addActionListener(actionListener);
		blackBtn.setToolTipText("Draw with black pen");
		blackBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		redBtn = new JButton("Red");
		redBtn.addActionListener(actionListener);
		redBtn.setToolTipText("Draw with red pen");
		redBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		deleteBoardBtn = new JButton("Delete Board");
		deleteBoardBtn.addActionListener(actionListener);
		deleteBoardBtn.setToolTipText("Delete the current board - only deletes the board locally");
		deleteBoardBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		createBoardBtn = new JButton("New Board");
		createBoardBtn.addActionListener(actionListener);
		createBoardBtn.setToolTipText("Create a new board - creates it locally and not shared by default");
		createBoardBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		undoBtn = new JButton("Undo");
		undoBtn.addActionListener(actionListener);
		undoBtn.setToolTipText("Remove the last path drawn on the board - triggers an undo on remote copies as well");
		undoBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		sharedCheckbox = new JCheckBox("Shared");
		sharedCheckbox.addItemListener(new ItemListener() {    
	         public void itemStateChanged(ItemEvent e) { 
	            if(!modifyingCheckBox) setShare(e.getStateChange()==1);
	         }    
	      }); 
		sharedCheckbox.setToolTipText("Toggle whether the board is shared or not - tells the whiteboard server");
		sharedCheckbox.setAlignmentX(Component.CENTER_ALIGNMENT);
		

		// create a drop list for boards to select from
		JPanel controlsNorth = new JPanel();
		boardComboBox = new JComboBox<String>();
		boardComboBox.addActionListener(actionListener);
		
		
		// add to panel
		controlsNorth.add(boardComboBox);
		controls.add(sharedCheckbox);
		controls.add(createBoardBtn);
		controls.add(deleteBoardBtn);
		controls.add(blackBtn);
		controls.add(redBtn);
		controls.add(undoBtn);
		controls.add(clearBtn);

		// add to content pane
		content.add(controls, BorderLayout.WEST);
		content.add(controlsNorth,BorderLayout.NORTH);

		frame.setSize(600, 600);
		
		// create an initial board
		createBoard();
		
		// closing the application
		frame.addWindowListener(new WindowAdapter() {
		    @Override
		    public void windowClosing(WindowEvent windowEvent) {
		        if (JOptionPane.showConfirmDialog(frame, 
		            "Are you sure you want to close this window?", "Close Window?", 
		            JOptionPane.YES_NO_OPTION,
		            JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)
		        {
		        	guiShutdown();
		            frame.dispose();
		        }
		    }
		});
		
		// show the swing paint result
		frame.setVisible(true);
		
	}
	
	/**
	 * Update the GUI's list of boards. Note that this method needs to update data
	 * that the GUI is using, which should only be done on the GUI's thread, which
	 * is why invoke later is used.
	 * 
	 * @param select, board to select when list is modified or null for default
	 *                selection
	 */
	private void updateComboBox(String select) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				modifyingComboBox=true;
				boardComboBox.removeAllItems();
				int anIndex=-1;
				synchronized(whiteboards) {
					ArrayList<String> boards = new ArrayList<String>(whiteboards.keySet());
					Collections.sort(boards);
					for(int i=0;i<boards.size();i++) {
						String boardname=boards.get(i);
						boardComboBox.addItem(boardname);
						if(select!=null && select.equals(boardname)) {
							anIndex=i;
						} else if(anIndex==-1 && selectedBoard!=null && 
								selectedBoard.getName().equals(boardname)) {
							anIndex=i;
						} 
					}
				}
				modifyingComboBox=false;
				if(anIndex!=-1) {
					boardComboBox.setSelectedIndex(anIndex);
				} else {
					if(whiteboards.size()>0) {
						boardComboBox.setSelectedIndex(0);
					} else {
						drawArea.clear();
						createBoard();
					}
				}
				
			}
		});
	}
	
}
