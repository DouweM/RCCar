import java.io.IOException;

import javax.bluetooth.RemoteDevice;

import lejos.nxt.*;
import lejos.nxt.comm.*;
import lejos.robotics.navigation.*;

public class BTCar {
	private static DifferentialPilot pilot = new DifferentialPilot(2.1f, 4.4f, Motor.A, Motor.B, true);
	private static CarConnector carConnector = new CarConnector(pilot);
	  
	public static void main(String[] args) throws IOException {
		LCD.drawString("Waiting for", 0, 0);
		LCD.drawString("connection...", 0, 1);
		
		BTConnection connection = carConnector.waitForConnection();
		if (connection == null) {
			return;
		}

		String friendlyDeviceName = RemoteDevice.getRemoteDevice(connection).getFriendlyName(false);
		
		LCD.clear();
		LCD.drawString("Connected to", 0, 0);
		LCD.drawString(friendlyDeviceName, 0, 1);
		LCD.drawString("Press key to cont.", 0, 3);
		
		if (Button.waitForPress() == Button.ID_ESCAPE) {
			return;
		}
						
		while (true) {
			if (Button.ESCAPE.isPressed()) {
				break;
			}
			
			carConnector.waitForAndHandleCommand();
		}
		
		pilot.stop();
		
		carConnector.closeConnection();
	}
}
