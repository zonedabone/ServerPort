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

public class CommunicationManager {

	ServerPortServer serverPortServer = new ServerPortServer();
	
	PeerServerDatabase peerServerDatabase = new PeerServerDatabase();
	
	ChatManager chatManager = new ChatManager();
	
	PortalManager portalManager = null;
	
	LimboStore limboStore = new LimboStore();
	
	public CommandFIFO commandFIFO = new CommandFIFO();
	
	public int clientTimeout = 10000;
	
	public Boolean showServerName = true;
	
	public Integer defaultTimeToLive = 10;
	
	public Integer autoPermissionReload = -1;
	
	public Integer restartDelay = 60;

	public Integer restartThreshold = 300;
	
	public Boolean verbose = false;
	
	ParameterManager parameterManager = null;
	
	void setPortalManager(PortalManager portalManager) {
		this.portalManager = portalManager;
	}
		
	void registerParameters( ParameterManager parameterManager ) {
		
		this.parameterManager = parameterManager;
		
		parameterManager.registerParameter(new ParameterInfo( 
				this, 
				"restartDelay",
				"cpurestartwindow",
				Integer.class,
				new Integer(60),
				new String[] {
					"This sets the delay between cpu measurements for restarting"
				},
				"This sets the cpu restart delay"
		)
		);
		
		this.parameterManager = parameterManager;
		
		parameterManager.registerParameter(new ParameterInfo( 
				this, 
				"verbose",
				"networklog",
				Boolean.class,
				new Boolean(false),
				new String[] {
					"This enables verbose network logging"
				},
				"This enables verbose network logging"
		)
		);
		
		parameterManager.registerParameter(new ParameterInfo( 
				this, 
				"restartThreshold",
				"restartthreshold",
				Integer.class,
				new Integer(300),
				new String[] {
					"This sets the threshold for cpu restart in ticks per minute"
				},
				"This sets the cpu restart threshold"
		)
		);
		
		parameterManager.registerParameter(new ParameterInfo( 
				this, 
				"autoPermissionReload",
				"autopermission",
				Integer.class,
				new Integer(-1),
				new String[] {
					"This sets the delay between auto-reloads of the permission file, in seconds.  Negative numbers mean not to reload."
				},
				"period of permission file reloads"
		)
		);
		
		parameterManager.registerParameter(new ParameterInfo( 
				this, 
				"defaultTimeToLive",
				"timetolive",
				Integer.class,
				new Integer(10),
				new String[] {
					"This sets how many hops a player who logs onto the wrong server should be forwarded before giving up"
				},
				"time to live to server forwarding"
		)
		);
		
		parameterManager.registerParameter(new ParameterInfo( 
				this, 
				"clientTimeout",
				"clienttimeout",
				Integer.class,
				new Integer(10000),
				new String[] {
					"This sets the timeout in ms for the client",
					"It defaults to 10 seconds (10000)"
				},
				"timeout for serverport client"
		)
		);
		
		parameterManager.registerParameter(new ParameterInfo( 
				this, 
				"showServerName",
				"showserver",
				Boolean.class,
				new Boolean(true),
				new String[] {
					"If this parameter is set to true, the server name is show for inter server comms"
				},
				"show server name in the chat link"
		)
		);
		
		serverPortServer.registerParameters( parameterManager );
		limboStore.registerParameters(parameterManager);
		chatManager.RegisterParameters(parameterManager);

		
	}
	
	void init() {
		
		peerServerDatabase.init();
		
		serverPortServer.setPeerServerDatabase(peerServerDatabase);
		serverPortServer.setCommunicationManager(this);
		serverPortServer.init();
		
		commandFIFO.setCommunicationManager(this);
		commandFIFO.setPortalManager(portalManager);
		commandFIFO.setLimboStore(limboStore);
		
		limboStore.setCommunicationManager(this);
		limboStore.init();
		
		chatManager.setCommunicationManager(this);
		
	}
	
	void stopServer() {
		serverPortServer.stopServer();
	}
	
	void attemptInvite( String playerName , String hostname , int portnum ) {
		
		commandFIFO.runBlindCommand( "INVITE:" + playerName + "," + hostname + "," + portnum );
		
	}
	
}
