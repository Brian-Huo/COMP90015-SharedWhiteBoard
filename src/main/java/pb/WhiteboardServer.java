package pb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import pb.managers.IOThread;
import pb.managers.PeerManager;
import pb.managers.ServerManager;
import pb.managers.endpoint.Endpoint;
import pb.utils.Utils;

/**
 * Simple whiteboard server to provide whiteboard peer notifications.
 * @author aaron
 *
 */
public class WhiteboardServer {
	private static Logger log = Logger.getLogger(WhiteboardServer.class.getName());
	
	/**
	 * Emitted by a client to tell the server that a board is being shared. Argument
	 * must have the format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String shareBoard = "SHARE_BOARD";

	/**
	 * Emitted by a client to tell the server that a board is no longer being
	 * shared. Argument must have the format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String unshareBoard = "UNSHARE_BOARD";

	/**
	 * The server emits this event:
	 * <ul>
	 * <li>to all connected clients to tell them that a board is being shared</li>
	 * <li>to a newly connected client, it emits this event several times, for all
	 * boards that are currently known to be being shared</li>
	 * </ul>
	 * Argument has format "host:port:boardid"
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String sharingBoard = "SHARING_BOARD";

	/**
	 * The server emits this event:
	 * <ul>
	 * <li>to all connected clients to tell them that a board is no longer
	 * shared</li>
	 * </ul>
	 * Argument has format "host:port:boardid"
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String unsharingBoard = "UNSHARING_BOARD";

	/**
	 * Emitted by the server to a client to let it know that there was an error in a
	 * received argument to any of the events above. Argument is the error message.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String error = "ERROR";
	
	/**
	 * Default port number.
	 */
	private static int port = Utils.indexServerPort;
	
	/**
	 * Storage of a list of sharing board
	 * we will emit all the board to a new connected peer
	 * host:port:boardid
	 */
	public static final ArrayList<String> sharingBoards=new ArrayList<String>();

	/**
	 * Keep a track of endpoints that
	 * have not yet terminated, so that we can wait/ask/force for them to finish
	 * before completely terminating. This object can be called by multiple
	 * endpoint threads and this server manager thread; so synchronized is needed.
	 */
	private static final Set<Endpoint> liveEndpoints = new HashSet<Endpoint>();
	
	private static void help(Options options){
		String header = "PB Whiteboard Server for Unimelb COMP90015\n\n";
		String footer = "\ncontact aharwood@unimelb.edu.au for issues.";
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("pb.IndexServer", header, options, footer, true);
		System.exit(-1);
	}
	
	public static void main( String[] args ) throws IOException, InterruptedException
    {
    	// set a nice log format
		System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tl:%1$tM:%1$tS:%1$tL] [%4$s] %2$s: %5$s%n");
        
    	// parse command line options
        Options options = new Options();
        options.addOption("port",true,"server port, an integer");
        options.addOption("password",true,"password for server");
        
       
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
			cmd = parser.parse( options, args);
		} catch (ParseException e1) {
			help(options);
		}
        
        if(cmd.hasOption("port")){
        	try{
        		port = Integer.parseInt(cmd.getOptionValue("port"));
			} catch (NumberFormatException e){
				System.out.println("-port requires a port number, parsed: "+cmd.getOptionValue("port"));
				help(options);
			}
        }

        // create a server manager and setup event handlers
        ServerManager serverManager;
        
        if(cmd.hasOption("password")) {
        	serverManager = new ServerManager(port,cmd.getOptionValue("password"));
        } else {
        	serverManager = new ServerManager(port);
        }
        
        /**
         * TODO: Put some server related code here.
         *  when new connection established, emit SHARING_BOARD or ERROR to the new connected peer
         *  listen SHARE_BOARD then emit SHARING_BOARD or ERROR to every client        
         *  listen UNSHARE_BOARD then emit UNSHARING_BOARD or ERROR to every client       
         */
        serverManager.on(ServerManager.sessionStarted,(eventArgs)->{
        	Endpoint endpoint = (Endpoint)eventArgs[0];

        	// add endpoint to the live endpoint list
			synchronized(liveEndpoints) {
				liveEndpoints.add(endpoint);
			}
			log.info("Receiving new peer connection "+endpoint.getOtherEndpointId());

        	// share all sharing board will new peer
			synchronized (sharingBoards) {
				for (String board : sharingBoards) {
					endpoint.emit(sharingBoard, board);
				}
			}

        	endpoint.on(shareBoard,(Args)->{
        		// listen on the all peers whether they want to share a board
				log.info("Receiving share board query " + (String) Args[0]);
        		log.info("Setting board " + (String) Args[0] + " as sharing board");
        		setShareBoard(endpoint, (String) Args[0]);
        	}).on(unshareBoard,(Args)->{
				// listen on the all peers whether they want to unshare a board
				log.info("Receiving unshare board query " + (String) Args[0]);
				log.info("Setting board " + (String) Args[0] + " as unsharing board");
				setUnshareBoard(endpoint, (String) Args[0]);
        	});
		}).on(ServerManager.sessionStopped, (eventArgs)->{
			Endpoint endpoint = (Endpoint) eventArgs[0];
			// delete endpoint from the live endpoint list
			synchronized(liveEndpoints) {
				liveEndpoints.remove(endpoint);
			}
			System.out.println("Disconnected from the whiteboard peer: "+endpoint.getOtherEndpointId());
			// TODO: delete relate boards
		}).on(ServerManager.sessionError, (eventArgs)->{
			Endpoint endpoint = (Endpoint) eventArgs[0];
			// delete endpoint from the live endpoint list
			synchronized(liveEndpoints) {
				liveEndpoints.remove(endpoint);
			}
			System.out.println("There was an error communicating with the whiteboard peer: "
					+endpoint.getOtherEndpointId());
			// TODO: delete relate boards
		}).on(IOThread.ioThread, (eventArgs)->{
			String peerport = (String) eventArgs[0];
			// we don't need this info, but let's log it
			log.info("Whiteboard Server is using Internet address: "+peerport);
		});
        
        // start up the server
        log.info("Whiteboard Server starting up");
        serverManager.start();
        // nothing more for the main thread to do
        serverManager.join();
        Utils.getInstance().cleanUp();
        
    }

	private static void setUnshareBoard(Endpoint endpoint, String board) {
		// delete board if exist
		synchronized (sharingBoards) {
			if (!sharingBoards.contains(board)) {
				endpoint.emit(error, "there is no such board on server " + board);
				log.warning("not exist board " + board + " is tried to be deleted by client " + endpoint.getOtherEndpointId());
			} else {
				sharingBoards.remove(board);
				log.info("whiteboard " + board + " removed");
			}
		}

		// emit to all peers except the one who is unsharing board about the unsharing board
		synchronized(liveEndpoints) {
			liveEndpoints.remove(endpoint);
			for (Endpoint livePeer : liveEndpoints) {
				log.info("unsharing board with all peers");
				livePeer.emit(unsharingBoard, board);
				log.info("successfully notify others unsharing board: "+board);
			}
			liveEndpoints.add(endpoint);
			log.info("board " + board + " has been unshared");
		}
	}

	private static void setShareBoard(Endpoint endpoint, String board) {
		// add board if not exist
		synchronized (sharingBoards) {
			if (!sharingBoards.contains(board)) {
				sharingBoards.add(board);
				log.info("whiteboard " + board + " added");
			}
		}

		// emit to all peers except the one who is sharing board about the sharing board
		synchronized(liveEndpoints) {
			liveEndpoints.remove(endpoint);
			for (Endpoint livePeer : liveEndpoints) {
				log.info("sharing board with all peers");
				livePeer.emit(sharingBoard, board);
				log.info("successfully notify others sharing board: "+board);
			}
			liveEndpoints.add(endpoint);
			log.info("board " + board + " has been shared");
		}
	}


}
