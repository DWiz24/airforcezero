package airforcezero;
import battlecode.common.*;
public class Gardener {
    public static void run(RobotController rc) throws GameActionException {
    	final int roundSpawned = rc.getRoundNum();
    	int soldiers = 0;
    	int lumbers = 0;
        while(true){
        	
        	//RobotType[] canBuild = {RobotType.SOLDIER, RobotType.LUMBERJACK, RobotType.TANK, RobotType.SCOUT};
        	//whichRobot = 0;
    		Direction[] dirs={new Direction((float)(Math.PI/3.0)), new Direction((float)(2.0*Math.PI/3.0)), new Direction((float)(3.0*Math.PI/3.0)), new Direction((float)(4.0*Math.PI/3.0)), new Direction((float)(5.0*Math.PI/3.0)), new Direction((float)(6.0*Math.PI/3.0))}; 
    		MapLocation sad = null; //Find lowest health tree, water it
    		float minhealth = 50f; 
    		TreeInfo[] trees = rc.senseNearbyTrees(3.0f);
    		System.out.println(trees.length);
    		for(TreeInfo tree:trees) {
    			if(tree.health < minhealth){
    				minhealth = tree.health;
    				sad = tree.location;
    			}
    		}
    		if(sad!=null && rc.canWater(sad))
    			rc.water(sad);
    		//End watering
    		
    		
    		//Finds number of nearby robots. If too many, find a good place to plant trees and soldiers. If it's been too long, stop moving start planting
    		boolean buildtree = false;
    		boolean looking = true;
    		
    		int whichDir = 0;
    		int numTrees = rc.getTreeCount();
    		int numRobots = rc.getRobotCount();
    		int numNearbys = 0;
    		if(rc.getRoundNum() - roundSpawned > 100)
    			looking = false;
    		if(looking) {
	    		RobotInfo nearbys[] = rc.senseNearbyRobots(5.49f);
	    		for(int i = nearbys.length - 1; i >= 0; i--) {
	    			RobotType thisType = nearbys[i].getType();
	    			if(thisType == RobotType.ARCHON || thisType == RobotType.GARDENER)
	    				numNearbys++;
	    		}
	    		
	    		System.out.println(nearbys + " " + rc.getLocation().toString());
	    		if(numNearbys>0) {
	    			while(!rc.canMove(dirs[whichDir]) && whichDir < dirs.length)
	    				whichDir++;
	    			if(rc.canMove(dirs[whichDir]))
	    				rc.move(dirs[whichDir]);
	    		}
    		}
    		//End trying to move code
    		
    		//What do I build code
    		else if (!looking || numNearbys == 0) {
	    		if(numTrees < numRobots && (int)rc.senseNearbyTrees(3.0f).length <= 4)
	    			buildtree = true;
	    		else
	    			buildtree = false;
	    		
				for (Direction place : dirs) {
					if(sad!=null && rc.isLocationOccupied(sad.add(place)))
						continue; 
					if (rc.canPlantTree(place) && rc.isBuildReady() && buildtree) { //Make sure this line actually works at some point
						rc.plantTree(place);
					}
					
						if (rc.canBuildRobot(RobotType.SOLDIER, place) && rc.isBuildReady()) {
							rc.buildRobot(RobotType.SOLDIER, place);
							soldiers++;
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