/**
 * 
 */
package com.feelink.jinho.nioems.main;

/**
 *
 * @author 최진호, 2010. 10. 19.
 */
public class RunServer {
	
	public void testMethod(int bb){
		System.out.println("테스트 성공 : " + bb);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("서버 시작됨");
		new RunServer().testMethod(1234);
	}

}
