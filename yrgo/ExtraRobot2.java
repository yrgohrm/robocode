package yrgo;

import static robocode.util.Utils.*;

import robocode.*;
import java.awt.Color;

/**
 * A very simple robot that just tries to shoot the first robot it can find.
 */
@SuppressWarnings("java:S110")
public class ExtraRobot2 extends AdvancedRobot {
    private static final Color bodyColor = new Color(40, 17, 43);
    private static final Color gunColor = new Color(94, 93, 92);
    private static final Color radarColor = new Color(141, 170, 145);

    private int radarDirection = 1;

    @Override
    public void run() {
        setColors(bodyColor, gunColor, radarColor);

        setAdjustRadarForGunTurn(true);
        setAdjustGunForRobotTurn(true);

        while (!Thread.currentThread().isInterrupted()) {
            turnRadarRight(360);
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
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
    public void onWin(WinEvent e) {
        for (int i = 0; i < 20; ++i) {
            setTurnRight(360);
            setTurnGunLeft(360);
            setTurnRadarRight(360);
            execute();
        }
    }
}
