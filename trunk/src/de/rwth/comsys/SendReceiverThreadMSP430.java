/**
 * 
 */
package de.rwth.comsys;

import java.nio.ByteBuffer;

import de.rwth.comsys.Enums.ThreadStates;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbRequest;
import android.widget.TextView;

/**
 * @author Christian
 *
 */
public class SendReceiverThreadMSP430 implements Runnable {
	private UsbDevice mDevice = null; 
	private UsbDeviceConnection mDeviceConnection = null;
	private UsbEndpoint sendingEndpointMSP430 = null;
	private UsbEndpoint receivingEndpointMSP430 = null;
	private ThreadStates state = ThreadStates.INIT;
	

	private SendReceiverThreadMSP430(){}

	

	public SendReceiverThreadMSP430(UsbDevice mDevice, UsbDeviceConnection mDeviceConnection, UsbEndpoint sendingEndpointMSP430, UsbEndpoint receivingEndpointMSP430){
		this.mDevice =  mDevice;
		this.mDeviceConnection = mDeviceConnection;
		this.sendingEndpointMSP430 = sendingEndpointMSP430;
		this.receivingEndpointMSP430 = receivingEndpointMSP430;
	}
	
	

	
	public void run() {
	
		ByteBuffer buffer = ByteBuffer.allocate(1);
		
		UsbRequest request = new UsbRequest();
		request.initialize(mDeviceConnection, receivingEndpointMSP430);
		byte status = -1;
		
		
		while (true) {
			
		}
		// TODO Auto-generated method stub

	}
	
	/**
	 * @return the state
	 */
	public synchronized ThreadStates getState() {
		return state;
	}

	/**
	 * @param state the state to set
	 */
	public synchronized void setState(ThreadStates state) {
		this.state = state;
	}

}
