package yrgo;

import static robocode.util.Utils.*;

import java.awt.*;
import java.util.*;
import robocode.*;

/**
 * Robot that keeps track of who has hit it and tries to shoot the robot that hits it the most
 * first.
 *
 */
@SuppressWarnings("java:S110")
public class ExtraRobot1 extends AdvancedRobot {
    private static final Color body = new Color(46, 56, 46);
    private static final Color gun = new Color(80, 201, 206);
    private static final Color radar = new Color(114, 161, 229);
    private static final Color bullet = Color.orange;
    private static final Color scan = new Color(152, 131, 229);

    private double priorityValue;
    private String priorityName;
    private int radarDirection;

    // robot name -> total power hit by
    private Map<String, Double> hits = new HashMap<>();

    @Override
    public void run() {
        priorityName = null;
        priorityValue = 0;
        radarDirection = 1;
        hits.clear();

        setColors(body, gun, radar, bullet, scan);
        setAdjustRadarForGunTurn(true);
        setAdjustGunForRobotTurn(true);

        while (!Thread.currentThread().isInterrupted()) {
            turnRadarRight(360);
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

        // Shoot when the enemy isn't too far away
        if (e.getDistance() < 300 && getGunHeat() == 0)
            setFire(Rules.MAX_BULLET_POWER);

        final double absoluteBearing = getHeading() + e.getBearing();
        final double bearingFromGun = normalRelativeAngleDegrees(absoluteBearing - getGunHeading());

        // Turn the robot towards the enemy
        setTurnRight(e.getBearing());
        setTurnGunRight(bearingFromGun);

        // And move forward
        setAhead(e.getDistance());

        // Inverts the gun direction each time we see a robot
        radarDirection = -radarDirection;

        // Turn 360 degrees (clockwise or anti clockwise,)
        setTurnRadarRight(360.0 * radarDirection);

        // Execute all the pending actions
        execute();
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
