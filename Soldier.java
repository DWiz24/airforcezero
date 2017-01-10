//package battlecode2017;
import battlecode.common.*;

public class Soldier {
    public static void run(RobotController rc) throws GameActionException {
        MapLocation[] initialEnemyLocs=rc.getInitialArchonLocations(rc.getTeam().opponent());
        MapLocation[] initialFriendLocs=rc.getInitialArchonLocations(rc.getTeam());
        int testDest=1;
        MapLocation dest=initialEnemyLocs[0];
        float bugMinDist=99999;
        Direction prevmove=null;
        boolean bugging=false;
        while(true){
            if (rc.getLocation().distanceTo(dest)<3) {
                if (testDest>=initialEnemyLocs.length) {
                    dest=initialFriendLocs[testDest-initialEnemyLocs.length];
                    testDest++;
                } else {
                    dest=initialEnemyLocs[testDest];
                    testDest++;
                }
            }
            Direction toDest=rc.getLocation().directionTo(dest);

            Clock.yield();
        }
    }
}