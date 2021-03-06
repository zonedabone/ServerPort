/*******************************************************************************
 * Copyright (C) 2012 Raphfrk
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package com.raphfrk.bukkit.serverport;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

public class PeerServerDatabase {
	
	protected static final Logger log = Logger.getLogger("Minecraft");
	
	static final String slash = System.getProperty("file.separator");
	
	String serverDirectory = "serverport";
	String filename = "servers.txt";
	
	HashMap<String,PeerServerInfo> serverDatabase = new HashMap<String,PeerServerInfo>();
	
	PeerServerInfo getServer( String name ) {
		
		return serverDatabase.get(name);
		
	}
	
	synchronized Set<String> getServerNameList() {
		return new HashSet<String>(serverDatabase.keySet());
	}
	
	synchronized void init() {
		loadDatabase();
	}
	
	synchronized void loadDatabase() {
		
		serverDatabase = new HashMap<String,PeerServerInfo>();

		File databaseFile = new File( MyServer.getBaseFolder() + slash + filename );

		if( !databaseFile.exists() ) {
			MiscUtils.safeLogging(log, "[Serverport] peer server database file doesn't exist");
			MiscUtils.safeLogging(log, "[Serverport] no servers have been configured");
			return;
		}
		
		BufferedReader databaseReader;
		
		try {
			databaseReader = new BufferedReader(new FileReader(MyServer.getBaseFolder() + slash + filename ));
		} catch (FileNotFoundException fnfe ) {
			MiscUtils.safeLogging(log, "[Serverport] unable to open peer server database for reading");
			return;
		}
		
		String line;
		
		try {
			while( (line = databaseReader.readLine()) != null ) {
				PeerServerInfo peerServer = new PeerServerInfo( line );
				if( peerServer.name.equals("none") ) {
					MiscUtils.safeLogging(log, "[ServerPort] Error processing peer server file");
				} else {
					serverDatabase.put(peerServer.name, peerServer);
				}
			}
		} catch (IOException ioe) {
			MiscUtils.safeLogging(log, "[ServerPort] Error processing peer server file");
		} finally {
			try {
				databaseReader.close();
			} catch (IOException ioe) {
				MiscUtils.safeLogging(log, "[ServerPort] Error closing peer server file");
			}
		}
		
		
	}
	
	synchronized void saveDatabase()  {
	
		BufferedWriter databaseWriter;
		
		try {
			databaseWriter = new BufferedWriter(new FileWriter(MyServer.getBaseFolder() + slash + filename ));
		} catch (IOException e) {
			MiscUtils.safeLogging(log, "[Serverport] unable to open peer server database for writing");
			return;
		}
				
		Iterator<String> itr = serverDatabase.keySet().iterator();
		
		while( itr.hasNext() ) {
			String current = itr.next();
			
			try {
				databaseWriter.write(serverDatabase.get(current).toString());
				databaseWriter.newLine();
			} catch (IOException e) {
				MiscUtils.safeLogging(log, "[Serverport] error writing to server database file");
				try {
					databaseWriter.close();
				} catch (IOException e1) {
					MiscUtils.safeLogging(log, "[Serverport] error closing server database file");
					return;
				}
				return;
			}
			
		}
		
		try {
			databaseWriter.close();
		} catch (IOException e1) {
			MiscUtils.safeLogging(log, "[Serverport] error closing server database file");
			return;
		}
		
	}
	
	synchronized String setServer( PeerServerInfo peerServer ) {
		
		if( MiscUtils.checkText( peerServer.name ) ) {
			if( serverDatabase.containsKey(peerServer.name)) {
				serverDatabase.put(peerServer.name, peerServer);
				saveDatabase();
				return "[ServerPort] " + peerServer.name + " information updated";
			} else {
				serverDatabase.put(peerServer.name, peerServer);
				saveDatabase();
				return "[ServerPort] " + peerServer.name + " added to server list";
			}
		} else {
			return MiscUtils.checkRules("[Serverport] Server name"); 
		}
		
		
	}

}
