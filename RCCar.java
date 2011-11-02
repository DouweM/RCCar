import java.io.IOException;

import lejos.nxt.*;
import lejos.nxt.comm.*;
import lejos.robotics.navigation.*;

public class RCCar {
	private static DifferentialPilot pilot = new DifferentialPilot(2.1f, 4.4f, Motor.A, Motor.B, false);
	private static RCCarEngine carEngine = new RCCarEngine(pilot);
	  
	public static void main(String[] args) throws IOException {
		System.out.println("Waiting for connection...");
		
		BTConnection connection = carEngine.waitForConnection();
		if (connection == null) {
			return;
		}

		String friendlyDeviceName = carEngine.getRemoteDevice().getFriendlyName(false);
		
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
			
			boolean success = carEngine.waitForAndHandleCommand();
			if (!success) {
				break;
			}
		}
		
		System.out.println("Finished reading commands. Shutting down.");
		
		pilot.stop();
		carEngine.closeConnection();
		
		Button.waitForPress();
	}
}
