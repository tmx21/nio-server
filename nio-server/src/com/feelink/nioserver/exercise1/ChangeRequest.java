/**
 * 
 */
package com.feelink.nioserver.exercise1;

import java.nio.channels.SocketChannel;

/**
 *
 * @author jin ho choi, 2010. 10. 27.
 */
public class ChangeRequest {
	
	public static final int REGISTER	= 1;
	public static final int CHANGEOPS	= 2;
	
	public int type;
	public int ops;
	public SocketChannel socketChannel;
	
	public ChangeRequest(SocketChannel socketChannel, int type, int ops){
		this.type	= type;
		this.ops	= ops;
		this.socketChannel = socketChannel;
	}
	
}
