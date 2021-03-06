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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;


public class ServerPortClient {
	
	String hostname;
	int portnum;
	String serverName;
	int timeout;
	
	String peerServerName = null;
	String passcode = null;
	
	boolean isValid = false;
	
	Socket connection = null;
	BufferedReader in = null;
	BufferedWriter out = null;
	
	String session = null;
	String peerSession = null;

	void registerParameters( ParameterManager parameterManager ) {
		
	}
	
	void init() {
		
	}
	
	ServerPortClient( CommunicationManager communicationManager , String hostname , int portnum ) {
		this.hostname = hostname;
		this.portnum = portnum;
		
		synchronized( communicationManager.serverPortServer.parameterManager ) {
			this.serverName = communicationManager.serverPortServer.serverName;
			this.timeout = communicationManager.clientTimeout;
		}
		passcode = "";
		isValid = true;
	}
	
	ServerPortClient( CommunicationManager communicationManager , String peerServerName ) {
		
		this.peerServerName = peerServerName;
		
		if( peerServerName == null ) {
			isValid = false;
			return;
		}
		
		PeerServerInfo peerServerInfo = communicationManager.peerServerDatabase.getServer(peerServerName);
				
		if( peerServerInfo != null ) {
			this.hostname = peerServerInfo.hostname;
			this.portnum = peerServerInfo.portnum;
		} else {
			isValid = false;
			return;
		}
		
		synchronized( communicationManager.serverPortServer.parameterManager ) {
			this.serverName = communicationManager.serverPortServer.serverName;
			this.timeout = communicationManager.clientTimeout;
		}
		passcode = "";
		isValid = true;		
	}
	
	String connect() {
		
		if( !isValid ) {
			if( peerServerName == null ) {
				return "no peer server name provided";
			} else {
				return "unknown target server \"" + peerServerName + "\"";
			}
		}
		
		if(portnum <= 0) {
			return "Port number for " + peerServerName + " not set correctly";
		}
		
		try {
			connection = new Socket( hostname , portnum );
			connection.setSoTimeout( timeout );
			
			try {
				in = new BufferedReader(
				        new InputStreamReader(
				            connection.getInputStream()));
			} catch (IOException e1) {
				return "Unable to open input stream";

			}
			
			try {
				out = new BufferedWriter(
						new OutputStreamWriter(
								connection.getOutputStream()));
			} catch (IOException ioe) {
				MiscUtils.safeLogging("[ServerPort] Unable to open out connection from " + connection.getInetAddress().getHostAddress() );
				try {
					if( out != null ) {
						out.close();
					}
				} catch (IOException e) {
					MiscUtils.safeLogging("[ServerPort] Connection close failed");
				}
				return "Unable to open output stream";
			}
			
			return null;
			
			
		} catch (SocketException skt ) {

			if( skt.getLocalizedMessage().equals("Connection refused: connect" ) ) {
				return "Target server port number not open";
			} else {
				return "Request timed out when trying to connect";
			}
		} catch (IOException ioe) {
			return "IO error ";
		} 
		
	}
	
	String getPeerServerInfo( 
			PeerServerInfo peerServerInfoFromConnection , 
			PeerServerDatabase peerServerDatabase ) {
		
		if( connection==null ) {
			return "Connection is null";
		} else if( in == null || out == null ) {
			return "Stream links are null";
		}
		
		try {
			
			out.write(serverName);
			out.newLine();
			out.flush();
			
			peerServerInfoFromConnection.name = in.readLine();
			
			PeerServerInfo peerServerInfoFromDatabase = peerServerDatabase.getServer(peerServerInfoFromConnection.name);
						
			session = MiscUtils.genRandomCode();
			out.write(session);
			out.newLine();
			out.flush();
			
			peerSession = in.readLine();
			
			if( peerServerInfoFromDatabase == null ) {
				out.write("pending");
				peerServerInfoFromConnection.connected = false;
				peerServerInfoFromConnection.passcode = "";

			} else {			
				boolean connected = peerServerInfoFromDatabase.connected;
				out.write(connected?"connected":"pending");
				peerServerInfoFromConnection.connected = connected;
				peerServerInfoFromConnection.passcode = peerServerInfoFromDatabase.passcode;
			}
			out.newLine();
			out.flush();
			
			peerServerInfoFromConnection.hostname = hostname;
			
			peerServerInfoFromConnection.portnum = portnum;
			
			if( serverName.equals(peerServerInfoFromConnection.name)) {
				return "Server attempted to connect to itself.  Make " +
						"sure you have used the correct hostname/port for the other server " + 
						"and that the other server is using a different name";
			}
			
			return null;
			
		} catch (SocketTimeoutException ste) {
			
			return "Error: Connection timed during connection header";
			
		} catch (IOException e) {
			return "Error: Error with server information handshake";
		}
		
		
		
	}
	
	String sendRequest(String command, String vars, PeerServerInfo peerServerInfo , boolean log ) {
		
		if(log) {
			MiscUtils.safeLogging("[ServerPort] Sending request: " + command + ":" + vars );
		}
				
		try {
			String line = command + ":" + vars;
			String toHash = peerServerInfo.passcode + line + peerSession + session;
			String hash = MiscUtils.sha1Hash(toHash);
			
			out.write(line);
			out.newLine();
			out.write(hash);
			out.newLine();
			out.flush();
			
		} catch (SocketTimeoutException ste) {
			
			return "Error: [ServerPort] Connection timed out";
			
		} catch (IOException e) {
		
			return "Error: [ServerPort] I/O Error with connection";
		}
		
		String reply = null;
		String replyHash = null;
		
		try {
		reply = in.readLine();
		replyHash = in.readLine();
		if(log) {
			MiscUtils.safeLogging("[ServerPort] Reply received: " + reply );
		}
		} catch (SocketTimeoutException ste) {
			
			return "Error: [ServerPort] Connection timed out waiting for reply";
			
		} catch (IOException e) {
		
			return "Error: [ServerPort] I/O Error with connection";
		}
		
		if( reply == null || replyHash == null ) {
			return "Error: [ServerPort] No reply sent from peer server";
		}
		
		String expectedReplyHash = MiscUtils.sha1Hash( peerServerInfo.passcode + reply + peerSession + session);
		if( replyHash.equals(expectedReplyHash) ) {
			if(log) {
				MiscUtils.safeLogging("[ServerPort] Reply verified" );
			}
			return reply;
		} else {
			return "Error: [ServerPort] Hash mismatch with connection";
		}
		
	}
	
	String close(PeerServerInfo peerServerInfo, boolean log ) {
		
		sendRequest( "END" , "END" , peerServerInfo , log );
		
		try {
			out.flush();
			out.close();
		} catch (IOException e) {
			return "Error: [ServerPort] Unable to close connection";
		}
		
		return null;

		
	}
	
}
