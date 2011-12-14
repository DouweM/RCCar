import java.io.*;

import lejos.robotics.navigation.*;
import lejos.nxt.comm.*;

public class RCCarEngine {
	public static final byte COMMAND_TRAVEL		= 1 << 0; // Followed by speed: 	positive = forward, negative = backward
	public static final byte COMMAND_STEER		= 1 << 1; // Followed by turnRate:	positive = right, 	negative = left
	public static final byte COMMAND_STOP		= 1 << 2;
	
	public static enum TravelDirection { FORWARD, BACKWARD };
	
	private final DifferentialPilot _pilot;
	private final DataInputStream _dataInStream;

	
	private boolean _moving						= false;
	private TravelDirection _travelDirection 	= TravelDirection.FORWARD;
	private float _travelSpeed	 				= 0.0f;
	private float _steerTurnRate 				= 0.0f;
	
	public RCCarEngine(DifferentialPilot pilot, NXTConnection connection) {
		_pilot = pilot;
		_dataInStream = connection.openDataInputStream();
	}
	
	public void log(String message) {
		System.out.println("E: " + message);
	}
	
	public void close() {
		try {
			_moving = false;
			_travelDirection = TravelDirection.FORWARD;
			_travelSpeed = 0.0f;
			_steerTurnRate = 0.0f;
			
			_pilot.stop();
			
			_dataInStream.close();
			
			try {
				Thread.sleep(100); // Wait for data to drain
			} catch (InterruptedException e) {}
		} 
		catch (IOException e) {
			this.log("Failed to close: " + e);
			return;
		}
	}
	
	public boolean waitForAndHandleCommand() {
//		this.log("Waiting...");
		
		byte command;
		try {
			command = _dataInStream.readByte();
		} 
		catch (EOFException e) {
			this.log("EOF reached.");
			return false;
		} 
		catch (IOException e) {
			this.log("Failed to read command: " + e);
			return false;
		}
		
//		this.log("Received " + command);
		
		return handleCommand(command);
	}
	
	public boolean handleCommand(byte command) {
		switch (command) {
			case COMMAND_TRAVEL: {				
				float speed;
				try {
					speed = _dataInStream.readFloat();
				} 
				catch (IOException e) {
					this.log("Failed to read speed: " + e);
					return false;
				}
								
				this.doTravel(speed);
				
				break;
			}
			
			case COMMAND_STEER: {				
				float turnRate;
				try {
					turnRate = _dataInStream.readFloat();
				} 
				catch (IOException e) {
					this.log("Failed to read turnRate: " + e);
					return false;
				}
								
				this.doSteer(turnRate);
				
				break;
			}
			
			case COMMAND_STOP: {
				this.doStop();
				
				break;
			}
			
			default: {
				this.log("Unrecognized command: " + command);
				return false;
			}
		}
		
		return true;
	}
	
	private float getLejosSteerTurnRate() {
		float turnRate = _steerTurnRate;
		
		// direction: -1 = left, 1 = right
		int direction = (int) Math.signum(turnRate);
		
		// Absolute turnRate should be kept between 0.0 and 100.0
		turnRate = Math.abs(turnRate);
		turnRate = (float) Math.max(0.0f, (float) Math.min(100.0f, turnRate));
		
		// Our turnRate: positive => right
		// leJOS turnRate: negative => right
		turnRate = -1 * direction * turnRate;
		
		// turnRate: ratio of inside to outside motor: ratio = 1 - turnRate / 100
		// positive turnRate => left  motor drives the inside wheel => car turns left, forward  or right, backward
		// negative turnRate => right                                            right, forward or left, backward
		// turnRate == 0   => ratio = 1 - 0/100   = 1.0  => car travels in straight line
		// turnRate == 100 => ratio = 1 - 100/100 = 0.0  => inside motor stops
		// turnRate == 200 => ratio = 1 - 200/100 = -1.0 => car turns in place
		
		return turnRate;
	}
	
	private void go() {
		if (!_moving) {
			return;
		}
		
		// If turnRate == 0.0f, we're just traveling in a straight line.
		float turnRate = this.getLejosSteerTurnRate();
		
		if (_travelDirection == TravelDirection.FORWARD) {
			_pilot.steer(turnRate);
		}
		else {
			_pilot.steerBackward(turnRate);
		}
	}
	
	private void doTravel(float speed) {
		System.out.println("TRAVEL: " + speed);

		_moving = true;
		_travelSpeed = speed;
		
		if (speed < 0) {
			_travelDirection = TravelDirection.BACKWARD;
		}
		else {
			_travelDirection = TravelDirection.FORWARD;
		}
		speed = Math.abs(speed);
		
		_pilot.setTravelSpeed(speed);
		
		this.go();
	}
	
	private void doSteer(float turnRate) {
		System.out.println("STEER: " + turnRate);
		
		_steerTurnRate = turnRate;
		
		this.go();
	}
	
	private void doStop() {
		System.out.println("STOP");
		
		_moving = false;
		_travelDirection = TravelDirection.FORWARD;
		_travelSpeed = 0.0f;
		_steerTurnRate = 0.0f;
		
		_pilot.stop();
		_pilot.setTravelSpeed(0.0);
		_pilot.steer(0);
	}
}
