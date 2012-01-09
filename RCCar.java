import java.io.IOException;

import lejos.nxt.*;
import lejos.nxt.comm.*;
import lejos.robotics.navigation.*;

public class RCCar {
    public static void main(String[] args) throws IOException {
        while (true) {
            System.out.println("Waiting...");

            BTConnection connection = Bluetooth.waitForConnection(0, NXTConnection.RAW);
            if (connection == null) {
                System.out.println("Failed to get connection");
                return;
            }

            System.out.println("Connected");

            DifferentialPilot pilot = new DifferentialPilot(2.9, 17.4, Motor.A, Motor.B, false);
            RCCarEngine carEngine = new RCCarEngine(pilot, connection);

            System.out.println("Reading commands");

            while (true) {
                boolean success = carEngine.waitForAndHandleCommand();
                if (!success) {
                    break;
                }
            }

            System.out.println("Finished reading");

            carEngine.close();
            connection.close();

            System.out.println("Another round?");

            if (Button.waitForPress() == Button.ID_ESCAPE) {
                break;
            }
        }
    }
}
