package yrgo;

import static robocode.util.Utils.*;

import java.awt.*;
import robocode.*;

/**
 * Runs around randomly and tries to find enemies in it path.
 * 
 */
@SuppressWarnings("java:S110")
public class ExtraRobot5 extends AdvancedRobot {

	private static final Color body = new Color(40, 200, 60);
	private static final Color gun = new Color(100, 41, 117);
	private static final Color radar = new Color(0, 30, 255);
	private static final Color bullet = new Color(0, 200, 0);
	private static final Color scan = new Color(255, 200, 200);

	private static final double GUN_TURN = 45;

	private boolean turnLeft = true;

	@Override
	public void run() {
		setColors(body, gun, radar, bullet, scan);

		setAdjustGunForRobotTurn(false);
		turnGunRight(GUN_TURN);

		while (!Thread.currentThread().isInterrupted()) {
			gunTurn();
			move();
			execute();
		}
	}

	@Override
	public void onScannedRobot(ScannedRobotEvent e) {
		if (getGunHeat() == 0 && getEnergy() > 1.1) {
			fire(1);
		}
	}

	@Override
	public void onHitRobot(HitRobotEvent e) {
		if (Math.abs(e.getBearing()) <= 90) {
			back(50);
		}
		else {
			ahead(50);
		}

		moveTowardsCenter();
	}

	@Override
	public void onHitWall(HitWallEvent e) {
		moveTowardsCenter();
	}

	@Override
	public void onWin(WinEvent e) {
		setColors(Color.WHITE, Color.WHITE, Color.WHITE);
		ahead(0);
		turnLeft(45);
		turnRight(90);
		turnLeft(90);
		turnRight(90);
	}

	/**
	 * Move the robot towards the center of the battlefield.
	 * 
	 */
	private void moveTowardsCenter() {
		double centerX = getBattleFieldWidth() / 2.0;
		double centerY = getBattleFieldHeight() / 2.0;

		double currentX = getX();
		double currentY = getY();

		double diffX = centerX - currentX;
		double diffY = centerY - currentY;

		double angleToCenter = Math.atan2(diffX, diffY);

		double angleToCenterDegrees = Math.toDegrees(angleToCenter);

		// Calculate the turn angle by subtracting the current bearing from the angle to the center
		double turnAngle = angleToCenterDegrees - getHeading();

		turnAngle = normalRelativeAngleDegrees(turnAngle);

		double distanceToCenter = Math.sqrt(diffX * diffX + diffY * diffY);

		setTurnRight(turnAngle);
		setAhead(distanceToCenter);
		execute();
	}

	private void gunTurn() {
		if (getGunTurnRemaining() == 0) {
			final double turn = GUN_TURN * 2;
			setTurnGunRight(turnLeft ? -turn : turn);
			turnLeft = !turnLeft;
		}
	}

	private void move() {
		if (getDistanceRemaining() < 50) {
			final double turn = 180 * Math.random();
			setTurnRight(turnLeft ? -turn : turn);
			setAhead(200);
		}
	}
}
