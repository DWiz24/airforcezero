package airforcezero;
import battlecode.common.*;

public class Gardener {
    public static void run(RobotController rc) throws GameActionException {
        while(true){
        	System.out.println("works");
        	RobotType[] canBuild = {RobotType.SOLDIER, RobotType.LUMBERJACK, RobotType.TANK, RobotType.SCOUT};
        	int whichRobot = 0;
        	boolean buildTree = true;
    		Direction[] dirs={Direction.getNorth(),Direction.getSouth(),Direction.getEast(),Direction.getWest()};
    		for(Direction place:dirs){
    			if(rc.canPlantTree(place) && rc.isBuildReady() && buildTree) {
    				rc.plantTree(place);
    				buildTree = false;
    			}
    			if(rc.canBuildRobot(canBuild[whichRobot], place) && rc.isBuildReady()) {
    				System.out.println("about to build");
    				rc.buildRobot(canBuild[whichRobot], place);
    				buildTree = true;
    			}
    		}
  
            Clock.yield();
        }
    }
    public static void pickDest() {
    	
    }
}