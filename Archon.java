package airforcezero;
import battlecode.common.*;

public class Archon {
    public static void run(RobotController rc) throws GameActionException {
        if (rc.canHireGardener(Direction.getEast()))
        rc.hireGardener(Direction.getEast());
        else rc.hireGardener(Direction.getWest());
        while(true){
            Clock.yield();
        }
    }
}