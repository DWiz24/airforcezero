package airforcezero;
import battlecode.common.*;
import java.util.Random;
public class Gardener {
    public static void run(RobotController rc) throws GameActionException {
        while(true){
        	
        	//RobotType[] canBuild = {RobotType.SOLDIER, RobotType.LUMBERJACK, RobotType.TANK, RobotType.SCOUT};
        	//int whichRobot = 0;

    		Direction[] dirs={new Direction((float)(Math.PI/4.0)), Direction.getEast(), new Direction((float)(5.0*Math.PI/4.0)), Direction.getNorth(), new Direction((float)(3.0*Math.PI/4.0)),Direction.getSouth(),Direction.getWest()};
    		Random random = new Random();
    		MapLocation sad = null; //Find lowest health tree, water it
    		float minhealth = 50f;
    		TreeInfo[] trees = rc.senseNearbyTrees(2.0f);
    		System.out.println(trees.length);
    		for(TreeInfo tree:trees) {
    			if(tree.health < minhealth){
    				minhealth = tree.health;
    				sad = tree.location;
    			}
    		}
    		if(sad!=null && rc.canWater(sad))
    			rc.water(sad);
    		
    		//Build robots (just soldiers and lumberjacks atm) and plant trees 
    		boolean buildtree = false;
    		int numTrees = rc.getTreeCount();
    		int numRobots = rc.getRobotCount();
    		float nearbys = (rc.senseNearbyRobots(1.0f).length);
    		System.out.println(nearbys);
    		if(nearbys>3){
    			Direction rand = new Direction((float)Math.random() * 2 * (float)Math.PI);
    			if(rc.canMove(rand))
    				rc.move(rand);
    		}
    		if(numTrees < numRobots/2)
    			buildtree = true;
    		if (rc.isBuildReady()) {
				for (Direction place : dirs) {
					if(sad!=null && rc.isLocationOccupied(sad.add(place)))
						continue;
					if (rc.canPlantTree(place) && rc.isBuildReady() && buildtree) { //Make sure this line actually works at some point
						rc.plantTree(place);
					}
					if (rc.canBuildRobot(RobotType.SOLDIER, place) && rc.isBuildReady()) {
						rc.buildRobot(RobotType.SOLDIER, place);
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