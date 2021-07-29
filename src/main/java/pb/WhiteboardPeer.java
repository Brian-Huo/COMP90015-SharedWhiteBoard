package pb;


import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import pb.app.WhiteboardApp;
import pb.utils.Utils;

/**
 * Just a bootstrap class for the actual whiteboard app.
 * @author aaron
 *
 */
public class WhiteboardPeer {
	private static Logger log = Logger.getLogger(WhiteboardPeer.class.getName());

	/**
	 * port to use for this peer's server
	 */
	private static int peerPort=Utils.serverPort; // default port number for this peer's server
	
	
	/**
	 * port to use when contacting the index server
	 */
	private static int whiteboardServerPort=Utils.indexServerPort; // default port number for index server

	/**
	 * host to use when contacting the index server
	 */
	private static String host=Utils.serverHost; // default host for the index server
	
	/**
	 * Print some help.
	 * @param options
	 */
	private static void help(Options options){
		String header = "Whiteboard Peer for Unimelb COMP90015\n\n";
		String footer = "\ncontact aharwood@unimelb.edu.au for issues.";
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("pb.WhiteboardPeer", header, options, footer, true);
		System.exit(-1);
	}
	
	public static void main(String[] args) {
		// set a nice log format
		System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tl:%1$tM:%1$tS:%1$tL] %2$s %4$s: %5$s%n");
		
		Options options = new Options();
        options.addOption("port",true,"peer server port, an integer");
        options.addOption("host",true,"whiteboard server hostname, a string");
        options.addOption("whiteboardServerPort",true,"whiteboard server port, an integer");
		
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
			cmd = parser.parse( options, args);
		} catch (ParseException e1) {
			help(options);
		}
        
        if(cmd.hasOption("port")){
        	try{
        		peerPort = Integer.parseInt(cmd.getOptionValue("port"));
			} catch (NumberFormatException e){
				System.out.println("-port requires a port number, parsed: "+
						cmd.getOptionValue("port"));
				help(options);
			}
        }
        
        if(cmd.hasOption("whiteboardServerPort")) {
        	try{
        		whiteboardServerPort = Integer.parseInt(cmd.getOptionValue("whiteboardServerPort"));
			} catch (NumberFormatException e){
				System.out.println("-whiteboardServerPort requires a port number, parsed: "+
						cmd.getOptionValue("whiteboardServerPort"));
				help(options);
			}
        }
        
        if(cmd.hasOption("host")) {
        	host = cmd.getOptionValue("host");
        }
        
		WhiteboardApp whiteboard = new WhiteboardApp(peerPort,host,whiteboardServerPort);
		whiteboard.waitToFinish();
		Utils.getInstance().cleanUp();
	}

}