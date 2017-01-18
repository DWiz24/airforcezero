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
    		//System.out.println(trees.length);
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
    		MapLocation me = rc.getLocation();
    		boolean buildtree = false;
    		boolean looking = true;
    		
    		int whichDir = 0;
    		int numTrees = rc.getTreeCount();
    		int numRobots = rc.getRobotCount();
    		int numNearbys = 0;
    		
    		if(rc.getRoundNum() - roundSpawned > 50)
    			looking = false;
    		if(looking) {
	    		RobotInfo nearbys[] = rc.senseNearbyRobots(3f);
	    		Direction badDirections[] = new Direction[nearbys.length];
	    		for(int i = nearbys.length - 1; i >= 0; i--) {
	    			RobotType thisType = nearbys[i].getType();
	    			System.out.println("before");
	    			if(thisType == RobotType.ARCHON || thisType == RobotType.GARDENER)
	    				numNearbys++;
	    			badDirections[i] = new Direction(me, nearbys[i].getLocation()); 
	    		}

	    		System.out.println(nearbys + " around this location: " + rc.getLocation().toString());
	    		if(numNearbys>0) {
	    			while(whichDir < dirs.length-1 && !rc.canMove(dirs[whichDir]))
	    				whichDir++;
	    			if(whichDir < dirs.length && rc.canMove(dirs[whichDir]))
	    				rc.move(dirs[whichDir]);
	    		}
    		}
    		//End trying to move code
    		
    		//What do I build code
    		
	   		if(numTrees < numRobots && (int)rc.senseNearbyTrees(3.0f).length <= 4 && !looking && numRobots > 2)
	   			buildtree = true;
	   		else
	   			buildtree = false;
	    		
			for (Direction place : dirs) {
				//if(sad!=null && rc.isLocationOccupied(sad.add(place)))
				//	continue; 
				if (rc.canPlantTree(place) && rc.isBuildReady() && buildtree) { 
					rc.plantTree(place);
				}
					
						if (rc.canBuildRobot(RobotType.SOLDIER, place) && rc.isBuildReady()) {
							rc.buildRobot(RobotType.SOLDIER, place);
							soldiers++;
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
    
    public static boolean isBad(RobotController rc, Direction place, Direction[] in) {
    	for(int i = in.length - 1; i >= 0; i--) {
    		if(place.equals(in[i], (float)(Math.PI/8.0)))
    			return true;
    	}
    	return false; 
    }
}