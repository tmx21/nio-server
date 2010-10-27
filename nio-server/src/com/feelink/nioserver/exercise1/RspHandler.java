/**
 * 
 */
package com.feelink.nioserver.exercise1;

/**
 *
 * @author jin ho choi, 2010. 10. 27.
 */
public class RspHandler {

	private byte[] rsp = null;
	
	public synchronized boolean handleResponse(byte[] rsp){
		this.rsp = rsp;
		this.notify();
		return true;
	}
	
	public synchronized void waitForResponse(){
		while(this.rsp == null){
			try {
				this.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println(new String(this.rsp));
	}
	
}
