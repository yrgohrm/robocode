package yrgo;

import java.awt.*;
import robocode.*;

/**
 * A robot that drives around the edge at a few different distances from the wall. It will stay at
 * one distance from the wall as long as it finds robots suitable to shoot at often enough.
 * 
 */
@SuppressWarnings("java:S110")
public class ExtraRobot3 extends AdvancedRobot {

	private static final double[] WALL_DISTANCE = {108, 54, 18};

	private static final Color body = new Color(140, 30, 255);
	private static final Color gun = new Color(255, 41, 117);
	private static final Color radar = new Color(242, 34, 255);
	private static final Color bullet = new Color(255, 144, 31);
	private static final Color scan = new Color(255, 211, 25);

	private boolean shouldGunTurnLeft = false;
	private long lastFireTime = 0;
	private int wallDistanceIndex = 0;

	@Override
	public void run() {
		shouldGunTurnLeft = false;
		lastFireTime = 0;
		wallDistanceIndex = 0;

		setColors(body, gun, radar, bullet, scan);

		turnLeft(getHeading() % 90);

		while (!Thread.currentThread().isInterrupted()) {
			gunTurn();
			move();
			execute();

			maybeSwitchDistanceFromWall();
		}
	}

	private void maybeSwitchDistanceFromWall() {
		long time = getTime();
		if (time - lastFireTime > 150) {
			lastFireTime = time;
			wallDistanceIndex = (wallDistanceIndex + 1) % WALL_DISTANCE.length;
		}
	}

	@Override
	public void onScannedRobot(ScannedRobotEvent e) {
		final double MAX_FIRING_RANGE = 300.0;
		final double MIN_FIRING_RANGE = 0.0;
		final double MAX_BULLET_POWER = Rules.MAX_BULLET_POWER;
		final double MIN_BULLET_POWER = 0.1;
		final double SLOPE =
				(MAX_BULLET_POWER - MIN_BULLET_POWER) / (MIN_FIRING_RANGE - MAX_FIRING_RANGE);
		final double INTERCEPT = MAX_BULLET_POWER - SLOPE * MIN_FIRING_RANGE;

		final double distance = e.getDistance();

		if (distance <= MAX_FIRING_RANGE && getGunHeat() == 0 && getEnergy() > 0.2) {
			final double power = SLOPE * distance + INTERCEPT;

			lastFireTime = getTime();
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
		if (getGunTurnRemaining() == 0) {
			setTurnGunRight(shouldGunTurnLeft ? -180 : 180);
			shouldGunTurnLeft = !shouldGunTurnLeft;
		}
	}

	private void move() {
		if (getDistanceRemaining() == 0) {
			turnRight(90);
			moveForward();
		}
	}

	private void moveForward() {
		final double wallDistance = WALL_DISTANCE[wallDistanceIndex];

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
