package yrgo;

import java.awt.*;
import robocode.*;

/**
 * Runs forwards and back along the X axis and simply tries to fire at anything that gets close
 * enough.
 * 
 */
@SuppressWarnings("java:S110")
public class ExtraRobot4 extends AdvancedRobot {

	private static final Color body = new Color(108, 88, 76);
	private static final Color gun = new Color(169, 132, 103);
	private static final Color radar = new Color(173, 193, 120);
	private static final Color bullet = new Color(0, 0, 0);
	private static final Color scan = new Color(221, 229, 182);

	@Override
	public void run() {
		setColors(body, gun, radar, bullet, scan);

		double middleY = getBattleFieldHeight() / 2.0;
		double currentY = getY();

		if (currentY < middleY) {
			turnLeft(getHeading());
		}
		else {
			turnLeft(getHeading() - 180);
		}

		ahead(Math.abs(currentY - middleY));
		turnLeft(90);

		while (!Thread.currentThread().isInterrupted()) {
			gunTurn();
			move();
			execute();
		}
	}

	@Override
	public void onScannedRobot(ScannedRobotEvent e) {
		final double MAX_FIRING_RANGE = 300.0;
		final double MIN_FIRING_RANGE = 0.0;
		final double MAX_BULLET_POWER = Rules.MAX_BULLET_POWER;
		final double MIN_BULLET_POWER = 1;
		final double SLOPE =
				(MAX_BULLET_POWER - MIN_BULLET_POWER) / (MIN_FIRING_RANGE - MAX_FIRING_RANGE);
		final double INTERCEPT = MAX_BULLET_POWER - SLOPE * MIN_FIRING_RANGE;

		final double distance = e.getDistance();

		if (distance <= MAX_FIRING_RANGE && getGunHeat() == 0 && getEnergy() > 1.1) {
			final double power = SLOPE * distance + INTERCEPT;

			fire(Math.min(power, getEnergy() - 0.1));
		}
	}

	@Override
	public void onWin(WinEvent e) {
		ahead(0);
		for (int i = 0; i < 100; ++i) {
			setColors(gun, radar, body);
			turnRight(2 * Rules.MAX_TURN_RATE);
			setColors(body, gun, radar);
			turnLeft(2 * Rules.MAX_TURN_RATE);
		}
	}

	private void gunTurn() {
		if (getGunTurnRemaining() < 10) {
			setTurnGunRight(360);
		}
	}

	private void move() {
		if (getDistanceRemaining() == 0) {
			turnRight(180);
			moveForward();
		}
	}

	private void moveForward() {
		final double wallDistance = getSentryBorderSize() - 1.0;

		double heading = getHeading();
		if (heading < 90) {
			double y = getY();
			double target = getBattleFieldHeight() - wallDistance;
			setAhead(target - y);
		}
		else if (heading < 180) {
			double x = getX();
			double target = getBattleFieldWidth() - wallDistance;
			setAhead(target - x);
		}
		else if (heading < 270) {
			double y = getY();
			double target = wallDistance;
			setAhead(y - target);
		}
		else {
			double x = getX();
			double target = wallDistance;
			setAhead(x - target);
		}
	}
}
