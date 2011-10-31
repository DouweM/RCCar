import java.io.IOException;

import lejos.nxt.*;
import lejos.nxt.comm.*;
import lejos.robotics.navigation.*;

public class BTCar {
	private static DifferentialPilot pilot = new DifferentialPilot(2.1f, 4.4f, Motor.A, Motor.B, false);
	private static CarConnector carConnector = new CarConnector(pilot);
	  
	public static void main(String[] args) throws IOException {
		System.out.println("Waiting for connection...");
		
		BTConnection connection = carConnector.waitForConnection();
		if (connection == null) {
			return;
		}

		String friendlyDeviceName = carConnector.getRemoteDevice().getFriendlyName(false);
		
		System.out.println("Connected to " + friendlyDeviceName);
		System.out.println("Press any key to continue");
		
		if (Button.waitForPress() == Button.ID_ESCAPE) {
			return;
		}
		
		System.out.println("Reading commands in loop");
						
		while (true) {
			if (Button.ESCAPE.isPressed()) {
				break;
			}
			
			boolean success = carConnector.waitForAndHandleCommand();
			if (!success) {
				break;
			}
		}
		
		System.out.println("Finished reading commands. Shutting down.");
		
		pilot.stop();
		carConnector.closeConnection();
		
		Button.waitForPress();
	}
}
