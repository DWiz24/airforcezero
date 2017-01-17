package airforcezero;
import battlecode.common.*;
import java.util.Random;
public class Gardener {
    public static void run(RobotController rc) throws GameActionException {
        while(true){
        	
        	RobotType[] canBuild = {RobotType.SOLDIER, RobotType.LUMBERJACK, RobotType.TANK, RobotType.SCOUT};
        	int whichRobot = 1;
        	Random random = new Random();
<<<<<<< HEAD
        	boolean buildTree = random.nextBoolean();
        	MapLocation here = rc.getLocation();
        	float x = here.x;
        	float y = here.y;
    		Direction[] dirs={Direction.getNorth(),Direction.getSouth(),Direction.getEast(),Direction.getWest(), new Direction(here, new MapLocation(x+1, y+1))};
=======
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
>>>>>>> 2d614cc904dcb0cafcc1022a14e1b16dcfecbbf8
    		
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