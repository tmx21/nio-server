/**
 * NIO server의 주요 기능을 담당하는 클래스들
 */
package com.feelink.nioserver.exercise1;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * NIO server main class
 * @author jin ho choi, 2010. 10. 19.
 */
public class NioServer implements Runnable {
	
	private InetAddress hostAddress;
	private int port;
	
	private ServerSocketChannel serverSocketChannel;
	
	private Selector selector;
	
	private ByteBuffer readBuffer = ByteBuffer.allocateDirect(8192);
	
	private EchoWorker echoWorker;
	
	private List pendingChanges = new LinkedList();
	
	private Map pendingData = new HashMap();
	
	public NioServer(InetAddress hostAddress, int prot, EchoWorker echoWorker) throws IOException{
		this.hostAddress = hostAddress;
		this.port = port;
		this.selector = this.initSelector();
		this.echoWorker = echoWorker;
	}

	public void send(SocketChannel socketChannel, byte[] data){
		synchronized(this.pendingChanges){
			this.pendingChanges.add(new ChangeRequest(socketChannel, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));
			synchronized(this.pendingData){
				List queue = (List)this.pendingData.get(socketChannel);
				if(queue == null){
					queue = new ArrayList();
					this.pendingData.put(socketChannel, queue);
				}
				queue.add(ByteBuffer.wrap(data));
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		while(true){
			try{
				synchronized(this.pendingChanges){
					Iterator changeIterator = this.pendingChanges.iterator();
					while(changeIterator.hasNext()){
						ChangeRequest changeRequst = (ChangeRequest)changeIterator.next();
						switch(changeRequst.type){
							case ChangeRequest.CHANGEOPS:
								SelectionKey key = changeRequst.socketChannel.keyFor(this.selector);
								key.interestOps(changeRequst.ops);
						}
					}
					this.pendingChanges.clear();
				}
				
				this.selector.select();
				
				Iterator selectedKeys = this.selector.selectedKeys().iterator();
				while(selectedKeys.hasNext()){
					SelectionKey key = (SelectionKey) selectedKeys.next();
					selectedKeys.remove();
					
					if(!key.isValid()){
						continue;
					}
					
					if(key.isAcceptable()){
						this.accept(key);
					}else if(key.isReadable()){
						this.read(key);
					}else if(key.isWritable()){
						this.write(key);
					}	
				}
			}catch(IOException e){
				e.printStackTrace();
			}		
		}
	}
	
	private void accept(SelectionKey key)throws IOException{
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel)key.channel();
		
		SocketChannel socketChannel = serverSocketChannel.accept();
		Socket socekt = socketChannel.socket();
		socketChannel.configureBlocking(false);
		
		socketChannel.register(this.selector, SelectionKey.OP_READ);
	}
	
	private void read(SelectionKey key)throws IOException{
		SocketChannel socketChannel = (SocketChannel)key.channel();
		
		this.readBuffer.clear();
		
		int numRead;
		try{
			numRead = socketChannel.read(this.readBuffer);
		}catch(IOException e){
			e.printStackTrace();
			key.cancel();
			socketChannel.close();
			return;
		}
		
		if(numRead == -1){
			key.channel().close();
			key.cancel();
		}
		
		this.echoWorker.processData(this, socketChannel, this.readBuffer.array(), numRead);
		
	}
	
	private void write(SelectionKey key)throws IOException{
		SocketChannel socketChannel = (SocketChannel)key.channel();
		
		synchronized(this.pendingData){
			List queue = (List)this.pendingData.get(socketChannel);
			
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
	 * @return
	 */
	private Selector initSelector() throws IOException {
		Selector socketSelector = SelectorProvider.provider().openSelector();
		
		this.serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.configureBlocking(false);
		
		InetSocketAddress inetSocketAddress = new InetSocketAddress(this.hostAddress, this.port);
		serverSocketChannel.socket().bind(inetSocketAddress);
		
		serverSocketChannel.register(socketSelector, SelectionKey.OP_ACCEPT);
		
		return socketSelector;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("서버 시작됨");
		try{
			EchoWorker echoWorker = new EchoWorker();
			new Thread(echoWorker).start();
			new Thread(new NioServer(null, 9090, echoWorker)).start();
		}catch(IOException e){
			e.printStackTrace();
		}
	}

}
