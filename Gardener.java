package airforcezero;
import battlecode.common.*;
import java.util.Random;
public class Gardener {
    public static void run(RobotController rc) throws GameActionException {
        while(true){
        	
        	RobotType[] canBuild = {RobotType.SOLDIER, RobotType.LUMBERJACK, RobotType.TANK, RobotType.SCOUT};
        	int whichRobot = 0;
        	Random random = new Random();
    		Direction[] dirs={new Direction((float)(Math.PI/4.0)), Direction.getEast(), new Direction((float)(5.0*Math.PI/4.0)), Direction.getNorth(), new Direction((float)(3.0*Math.PI/4.0)),Direction.getSouth(),Direction.getWest()};
    		MapLocation sad = null;
    		float minhealth = 50f;
    		TreeInfo[] trees = rc.senseNearbyTrees(2.0f);
    		System.out.println(trees.length);
    		for(TreeInfo tree:trees) {
    			System.out.println("going through trees");
    			if(tree.health < minhealth){
    				minhealth = tree.health;
    				sad = tree.location;
    				System.out.println("watered a tree");
    			}
    		}
    		
    		if(sad!=null && rc.canWater(sad))
    			rc.water(sad);
    		if (rc.isBuildReady()) {
				for (Direction place : dirs) {
					if(sad!=null && rc.isLocationOccupied(sad.add(place)))
						continue;
					if (rc.canPlantTree(place) && rc.isBuildReady()) {
						System.out.println("about to build tree");
						rc.plantTree(place);
					}
					if (rc.canBuildRobot(RobotType.SOLDIER, place) && rc.isBuildReady()) {
						System.out.println("about to build solider");
						rc.buildRobot(canBuild[whichRobot], place);
					}
				}
			}
    		if(rc.getTeamBullets() >= 10000){
    			rc.donate(10000f);
    		}
            Clock.yield();
        }
    }
    public static void pickDest() {
    	
    }
}