import lejos.nxt.*;
import lejos.robotics.navigation.*;

public class BTCar {
	private static DifferentialPilot pilot = new DifferentialPilot(2.1f, 4.4f, Motor.A, Motor.B, true);
	private static CarConnector carConnector = new CarConnector(pilot);
	  
	public static void main(String[] args) {
		if (carConnector.waitForConnection() == null) {
			return;
		}
		
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
