/**
 * 
 */
package com.feelink.nioserver.exercise1;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author jin ho choi, 2010. 10. 27.
 */
public class NioClient implements Runnable{

	private InetAddress hostAddress;
	private int port;
	
	private Selector selector;
	
	private ByteBuffer readBuffer = ByteBuffer.allocate(8192);
	
	private List pendingChanges = new LinkedList();
	
	private Map pedingData = new HashMap();
	
	private Map rspHandlers = Collections.synchronizedMap(new HashMap());
	
	public NioClient(InetAddress hostAddress, int port)throws IOException{
		this.hostAddress = hostAddress;
		this.port = port;
		this.selector = this.initSelector();
	}
	
	public void send(byte[] data, RspHandler handler)throws IOException{
		SocketChannel socket = this.initiateConnection();
		
		this.rspHandlers.put(socket, handler);
		
		synchronized(this.pedingData){
			List queue = (List)this.pedingData.get(socket);
			if(queue == null){
				queue = new ArrayList();
				this.pedingData.put(socket, queue);
			}
			queue.add(ByteBuffer.wrap(data));
		}
		
		this.selector.wakeup();
	}
	
	public void run(){
		while(true){
			try{
				synchronized(this.pendingChanges){
					Iterator changes = this.pendingChanges.iterator();
					while(changes.hasNext()){
						ChangeRequest change = (ChangeRequest)changes.next();
						switch(change.type){
							case ChangeRequest.CHANGEOPS:
								SelectionKey key = change.socketChannel.keyFor(this.selector);
								key.interestOps(change.ops);
								break;
							case ChangeRequest.REGISTER:
								change.socketChannel.register(this.selector, change.ops);
								break;
						}
					}
					this.pendingChanges.clear();
				}
				
				this.selector.select();
				
				Iterator selectedKeys = this.selector.selectedKeys().iterator();
				while(selectedKeys.hasNext()){
					SelectionKey key = (SelectionKey)selectedKeys.next();
					selectedKeys.remove();
					
					if(!key.isValid()){
						continue;
					}
					
					if(key.isConnectable()){
						this.finishConnection(key);
					}else if(key.isReadable()){
						this.read(key);
					}else if(key.isWritable()){
						this.write(key);
					}
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}

	private void read(SelectionKey key)throws IOException{
		SocketChannel socketChannel = (SocketChannel)key.channel();
		
		this.readBuffer.clear();
		
		int numRead;
		try{
			numRead = socketChannel.read(this.readBuffer);
		}catch(IOException e){
			key.cancel();
			socketChannel.close();
			return;
		}
		
		if(numRead == -1){
			key.channel().close();
			key.cancel();
			return;
		}
		this.handleResponse(socketChannel, this.readBuffer.array(), numRead);
	}
	
	private void handleResponse(SocketChannel socketChannel, byte[] data, int numRead)throws IOException{
		byte[] rspData = new byte[numRead];
		System.arraycopy(data, 0, rspData, 0, numRead);
		
		RspHandler handler = (RspHandler)this.rspHandlers.get(socketChannel);
		
		if(handler.handleResponse(rspData)){
			socketChannel.close();
			socketChannel.keyFor(this.selector).cancel();
		}
	}
	
	private void write(SelectionKey key)throws IOException{
		SocketChannel socketChannel = (SocketChannel)key.channel();
		
		synchronized(this.pedingData){
			List queue = (List)this.pedingData.get(socketChannel);
			while(!queue.isEmpty()){
				ByteBuffer byteBuffer = (ByteBuffer)queue.get(0);
				socketChannel.write(byteBuffer);
				if(byteBuffer.remaining() > 0){
					break;					
				}
				queue.remove(0);				
			}
			
			if(queue.isEmpty()){
				key.interestOps(SelectionKey.OP_READ);
			}
		}
		
	}
	
	/**
	 * @param key
	 */
	private void finishConnection(SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel)key.channel();
		
		try{
			socketChannel.finishConnect();
		}catch(IOException e){
			e.printStackTrace();
			key.cancel();
			return;
		}
		
	}

	/**
	 * @return
	 */
	private SocketChannel initiateConnection() throws IOException {
		SocketChannel socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(false);
		
		socketChannel.connect(new InetSocketAddress(this.hostAddress, this.port));
		
		synchronized(this.pendingChanges){
			this.pendingChanges.add(new ChangeRequest(socketChannel, ChangeRequest.REGISTER, SelectionKey.OP_CONNECT));
		}
		return socketChannel;
	}

	/**
	 * @return
	 */
	private Selector initSelector() throws IOException{
		// TODO Auto-generated method stub
		return SelectorProvider.provider().openSelector();
	}
	
	public static void main(String[] args){
		try{
			System.out.println("클라이언트 시작됨");
			NioClient client = new NioClient(InetAddress.getByName("www.google.com"), 80);
			Thread t = new Thread(client);
			t.setDaemon(true);
			t.start();
			RspHandler handler = new RspHandler();
			client.send("GET / HTTP/1.0\r\n\r\n".getBytes(), handler);
			handler.waitForResponse();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
}
