/**
 * 
 */
package com.feelink.nioserver.exercise1;

import java.nio.channels.SocketChannel;

/**
 *
 * @author jin ho choi, 2010. 10. 27.
 */
public class ServerDataEvent {
	
	public byte[] data;
	public NioServer nioServer;
	public SocketChannel socketChannel;
	
	public ServerDataEvent(NioServer nioServer, SocketChannel socketChannel, byte[] data){
		this.data		= data;
		this.nioServer	= nioServer;
		this.socketChannel = socketChannel; 
	}
	
}
