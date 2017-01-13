package airforcezero;

import battlecode.common.*;

public class Soldier {

    static MapLocation[] initialEnemyLocs;
    static MapLocation[] initialFriendLocs;
    static int testDest=0;
    public static void run(RobotController rc) throws GameActionException {
        initialEnemyLocs = rc.getInitialArchonLocations(rc.getTeam().opponent());
        initialFriendLocs = rc.getInitialArchonLocations(rc.getTeam());
        while (true) {
            //System.out.println(bugging);
            TreeInfo[] trees = rc.senseNearbyTrees();
            RobotInfo[] robots=rc.senseNearbyRobots();

            MapLocation toMove= Nav.soldierNav(rc,trees,robots);
            rc.move(toMove);
            Clock.yield();
            }
        }
    static void pickDest() {
        if (testDest-initialEnemyLocs.length==initialFriendLocs.length) testDest=0;
        if (testDest >= initialEnemyLocs.length) {

            Nav.setDest(initialFriendLocs[testDest - initialEnemyLocs.length]);
            testDest++;
        } else {
            Nav.setDest(initialEnemyLocs[testDest]);
            testDest++;
        }
    }
    }