package yrgo;

import static robocode.util.Utils.*;

import java.awt.*;
import java.util.*;
import robocode.*;

/**
 * Robot that keeps track of who has hit it and tries to shoot the robot that hits it the most
 * first.
 *
 * Tries to combine a few robots, without much success...
 * 
 */
@SuppressWarnings("java:S110")
public class ExtraRobot6 extends AdvancedRobot {
	private static final Color body = new Color(46, 56, 46);
	private static final Color gun = new Color(80, 201, 206);
	private static final Color radar = new Color(114, 161, 229);
	private static final Color bullet = Color.orange;
	private static final Color scan = new Color(152, 131, 229);

	private double priorityValue;
	private String priorityName;
	private double gunTurnAmount;
	private int count;

	// robot name -> total power hit by
	private Map<String, Double> hits = new HashMap<>();

	@Override
	public void run() {
		setColors(body, gun, radar, bullet, scan);
		setAdjustGunForRobotTurn(true);

		priorityName = null;
		priorityValue = 0;
		count = 0;
		gunTurnAmount = Rules.GUN_TURN_RATE;
		hits.clear();

		while (!Thread.currentThread().isInterrupted()) {
			setAhead(20);
			setTurnGunRight(gunTurnAmount);
			execute();

			count++;

			if (count > 5) {
				// If we still haven't seen our target for 5 turns, look right
				gunTurnAmount = Rules.GUN_TURN_RATE;
			}
			else if (count > 2) {
				// If we've haven't seen our target for 2 turns, look left
				gunTurnAmount = -Rules.GUN_TURN_RATE;
			}

			// If we *still* haven't seen our target, find another target
			if (count > 10) {
				track(null, 0);
				System.err.println("timeout finding target");

				setTurnGunRight(360);
				moveTowardsCenter();
			}
		}
	}

	@Override
	public void onScannedRobot(ScannedRobotEvent e) {
		// Don't ever target sentry robots if possible
		if (e.isSentryRobot()) {
			if (e.getName().equals(priorityName)) {
				hits.remove(priorityName);
				track(null, 0.0);
			}

			return;
		}

		// If we have a target, and this isn't it, return immediately
		// so we can get more ScannedRobotEvents.
		if (priorityName != null && !e.getName().equals(priorityName)) {
			return;
		}

		// If we don't have a target, well, now we do!
		if (priorityName == null) {
			track(e.getName(), hits.getOrDefault(e.getName(), 0.0));
		}

		count = 0;

		// If our target is too far away, turn and move toward it.
		if (e.getDistance() > 100) {
			moveCloserToTarget(e);
			return;
		}

		aimFireAndReposition(e);

		scan();
	}

	@Override
	public void onHitByBullet(HitByBulletEvent e) {
		// Track how much each robot has hit us for
		final String name = e.getName();
		double value = hits.merge(name, e.getPower(), Double::sum);

		if (priorityName == null || (value > priorityValue && !priorityName.equals(name))) {
			track(name, value);
		}
	}

	@Override
	public void onHitRobot(HitRobotEvent e) {
		double bearing = e.getBearing();

		// If in front of us, try to fire at it, just because :)
		if (Math.abs(bearing) < 10) {
			gunTurnAmount = normalRelativeAngleDegrees(bearing + (getHeading() - getGunHeading()));
			setTurnGunRight(gunTurnAmount);

			setFireIfAble(Rules.MAX_BULLET_POWER);
			setBack(30);

			execute();
		}
	}

	@Override
	public void onRobotDeath(RobotDeathEvent e) {
		System.err.println("death");

		// Stop tracking the dead robot
		final String name = e.getName();
		hits.remove(name);

		// If the robot we were targeting died, find a new target
		if (priorityName.equals(name)) {
			var optEntry = hits.entrySet().stream()
					.sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue())).findFirst();
			if (optEntry.isPresent()) {
				track(optEntry.get().getKey(), optEntry.get().getValue());
			}
			else {
				track(null, 0);
			}
		}
	}

	@Override
	public void onHitWall(HitWallEvent e) {
		// If we hit a wall, just try to move closer to the batttlefield center
		moveTowardsCenter();
	}

	@Override
	public void onWin(WinEvent e) {
		for (int i = 0; i < 100; ++i) {
			setColors(gun, radar, body);
			turnRight(2 * Rules.MAX_TURN_RATE);
			setColors(radar, body, gun);
			turnRight(2 * Rules.MAX_TURN_RATE);
			setColors(body, gun, radar);
			turnRight(2 * Rules.MAX_TURN_RATE);
		}
	}

	/**
	 * Fire if the gun isn't overheated and make sure we have some left over power after firing.
	 * 
	 * @param power the max amount of energy given to the bullet
	 * 
	 */
	private void fireIfAble(double power) {
		if (getGunHeat() == 0 && getEnergy() > 0.2) {
			fire(Math.min(power, getEnergy() - 0.1));
		}
	}

	/**
	 * Sets the gun to fire a bullet when the next execution takes place if the gun isn't overheated
	 * and make sure we have some left over power after firing.
	 * 
	 * @param power the max amount of energy given to the bullet
	 * 
	 */
	private void setFireIfAble(double power) {
		if (getGunHeat() == 0 && getEnergy() > 0.2) {
			setFire(Math.min(power, getEnergy() - 0.1));
		}
	}


	/**
	 * Move closer to the robot being scanned and fire a (small) shot if we think we have a good
	 * enough chance of hitting the robot.
	 * 
	 * @param e the scanned robot event
	 * 
	 */
	private void moveCloserToTarget(ScannedRobotEvent e) {
		final double distance = e.getDistance();
		final double bearing = e.getBearing();
		gunTurnAmount = normalRelativeAngleDegrees(bearing + (getHeading() - getGunHeading()));

		setTurnGunRight(gunTurnAmount);
		setTurnRight(bearing);
		setAhead(distance - 90);

		execute();

		// even if the robot is further away, fire at it if it is in front
		// of us and fire with faster bullets depending on distance
		final double MAX_FIRING_RANGE = 150.0;
		final double MIN_FIRING_RANGE = 100.0;
		final double MAX_BEARING_TO_FIRE = 10.0;
		if (distance <= MAX_FIRING_RANGE && Math.abs(bearing) <= MAX_BEARING_TO_FIRE) {
			final double SLOPE = (1.0 - 0.1) / (MIN_FIRING_RANGE - MAX_FIRING_RANGE);
			final double INTERCEPT = 1.0 - SLOPE * MIN_FIRING_RANGE;
			final double fireAmount = SLOPE * distance + INTERCEPT;

			fireIfAble(fireAmount);
		}
	}

	/**
	 * Aim at the scanned robot and then fire. Afterwords, reposition as to not present an immovable
	 * target.
	 * 
	 * @param e the scanned robot event
	 * 
	 */
	private void aimFireAndReposition(ScannedRobotEvent e) {
		double bearing = e.getBearing();
		gunTurnAmount = normalRelativeAngleDegrees(bearing + (getHeading() - getGunHeading()));
		setTurnGunRight(gunTurnAmount);
		execute();

		fireIfAble(Rules.MAX_BULLET_POWER);

		// Our target is too close! Back up.
		if (e.getDistance() < 100 && Math.abs(bearing) <= 90) {
			double amount = -40 + 80 * Math.random();
			setTurnGunRight(amount);
			setTurnLeft(amount);
			setBack(80);
		}
		else {
			// Otherwise we must always be moving
			setAhead(20);
		}

		execute();
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

	/**
	 * Select wich target we are currently tracking and the priority value it should have.
	 * 
	 * @param name the name of the robot
	 * @param value the priority this robot has
	 * 
	 */
	private void track(String name, double value) {
		System.err.println("switch target to " + name);
		priorityName = name;
		priorityValue = value;
	}
}
