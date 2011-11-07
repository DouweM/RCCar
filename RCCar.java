import java.io.IOException;

import javax.bluetooth.RemoteDevice;

import lejos.nxt.*;
import lejos.nxt.comm.*;
import lejos.robotics.navigation.*;

public class RCCar {
	public static void main(String[] args) throws IOException {
		while (true) {
			System.out.println("Waiting for connection...");
			
			BTConnection connection = Bluetooth.waitForConnection();
			if (connection == null) {
				System.out.println("Failed to get connection");
				return;
			}
	
			String friendlyDeviceName = RemoteDevice.getRemoteDevice(connection).getFriendlyName(false);
			
			System.out.println("Connected to " + friendlyDeviceName);
			
			DifferentialPilot pilot = new DifferentialPilot(2.9, 13.7, Motor.A, Motor.B, false);
			RCCarEngine carEngine = new RCCarEngine(pilot, connection);
			
			System.out.println("Reading commands in loop");
							
			while (true) {
				if (Button.ESCAPE.isPressed()) {
					break;
				}
				
				boolean success = carEngine.waitForAndHandleCommand();
				if (!success) {
					break;
				}
			}
			
			System.out.println("Finished reading commands. Shutting down.");
			
			carEngine.close();
			connection.close();
			
			System.out.println("Another round?");
			
			if (Button.waitForPress() == Button.ID_ESCAPE) {
				break;
			}
		}
	}
}
