/**
 * 
 */
package com.feelink.nioserver.exercise1;

import java.util.List;
import java.io.InterruptedIOException;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

/**
 *
 * @author jin ho choi, 2010. 10. 27.
 */
public class EchoWorker implements Runnable{

	private List queue = new LinkedList();
	
	public void processData(NioServer nioServer, SocketChannel socketChannel, byte[] data, int count){
		
		byte[] dataCopy = new byte[count];
		System.arraycopy(data, 0, dataCopy, 0, count);
		
		synchronized(queue){
			queue.add(new ServerDataEvent(nioServer, socketChannel, dataCopy));
			queue.notify();
		}
		
	}
	
	public void run(){
		
		ServerDataEvent serverDataEvent;
		
		while(true){
			
			synchronized(queue){
				while(queue.isEmpty()){
					try{
						queue.wait();
					}catch(InterruptedException e){
						e.printStackTrace();
					}
				}
				serverDataEvent = (ServerDataEvent) queue.remove(0);
			}
			
			serverDataEvent.nioServer.send(serverDataEvent.socketChannel, serverDataEvent.data);
			
		}
	}
	
}
