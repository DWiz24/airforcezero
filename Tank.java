package airforcezero;
import battlecode.common.*;

public class Tank {
    public static void run(RobotController rc) throws GameActionException {
        /*while(false){
            PublicMethods.donateBullets(rc);
            Clock.yield();
        } */
        System.out.println("Bid farewell, cruel world!");
        rc.disintegrate();
    }
    public static void pickDest() {
    	
    }
}