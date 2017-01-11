package airforcezero;
import battlecode.common.*;

public class Gardener {
    public static void run(RobotController rc) throws GameActionException {
        while(true){
        	RobotType[] canBuild = {RobotType.SOLDIER, RobotType.LUMBERJACK, RobotType.TANK, RobotType.SCOUT};
        	int whichRobot = 0;
    		Direction[] dirs={Direction.getNorth(),Direction.getSouth(),Direction.getEast(),Direction.getWest()};
    		for(Direction place:dirs){
    			if(rc.canBuildRobot(canBuild[whichRobot], place) && rc.isBuildReady()) {
    				rc.buildRobot(canBuild[whichRobot], place);
    			}
    		}
            Clock.yield();
        }
    }
}