package airforcezero;
import battlecode.common.*;
import java.util.Random;
public class Gardener {
    public static void run(RobotController rc) throws GameActionException {
        while(true){
        	
        	RobotType[] canBuild = {RobotType.SOLDIER, RobotType.LUMBERJACK, RobotType.TANK, RobotType.SCOUT};
        	int whichRobot = 1;
        	Random random = new Random();
        	boolean buildTree = random.nextBoolean();
        	MapLocation here = rc.getLocation();
        	float x = here.x;
        	float y = here.y;
    		Direction[] dirs={Direction.getNorth(),Direction.getSouth(),Direction.getEast(),Direction.getWest(), new Direction(here, new MapLocation(x+1, y+1))};
    		
    		if (rc.isBuildReady()) {
				for (Direction place : dirs) {
					if (rc.canPlantTree(place) && rc.isBuildReady() && buildTree) {
						System.out.println("about to build tree");
						rc.plantTree(place);
					}
					if (rc.canBuildRobot(RobotType.SOLDIER, place) && rc.isBuildReady()) {
						System.out.println("about to build solider");
						rc.buildRobot(canBuild[whichRobot], place);
					}
					buildTree = !buildTree;
				}
			}
            Clock.yield();
        }
    }
    public static void pickDest() {
    	
    }
}