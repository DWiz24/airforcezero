package airforcezero;
import battlecode.common.*;

public class Tank {
    public static void run(RobotController rc) throws GameActionException {
        while(true){
            PublicMethods.donateBullets(rc);
            Clock.yield();
        }
    }
    public static void pickDest() {
    	
    }
}