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
    		Team myTeam = rc.getTeam();
    		TreeInfo[] trees = rc.senseNearbyTrees(3.0f, myTeam);
    		//System.out.println(trees.length);
    		for(TreeInfo tree:trees) {
    			if(tree.health < minhealth && rc.canWater(tree.ID)){
    				minhealth = tree.health;
    				sad = tree.location;
    			}
    		}
    		if(sad!=null && rc.canWater(sad))
    			rc.water(sad);
    		if(rc.canShake())
    			shakeATree(rc);
    		
    		//Finds number of nearby robots. If too many, find a good place to plant trees and soldiers. If it's been too long, stop moving start planting
    		int baddirs=0;
    		for(int i = dirs.length-1; i >= 0; i--) {
    			if(rc.canMove(dirs[i]))
    				baddirs++;
    		}
    		
    		boolean buildtree = false;
    		boolean looking = true;
    		
    		int whichDir = 0;
    		int numNearbys = 0;
    		
    		if(rc.getRoundNum() - roundSpawned > 20)
    			looking = false;
    		
    		if(looking) {
	    		RobotInfo nearbys[] = rc.senseNearbyRobots(3f);
	    		for(int i = nearbys.length - 1; i >= 0; i--) {
	    			RobotType thisType = nearbys[i].getType();
	    			
	    			if(thisType == RobotType.ARCHON || thisType == RobotType.GARDENER || thisType == RobotType.LUMBERJACK)
	    				numNearbys++;
	    		}

	    		if(numNearbys>0) {
	    			while(whichDir < dirs.length-1 && !rc.canMove(dirs[whichDir]))
	    				whichDir++;
	    			if(rc.canMove(dirs[whichDir]))
	    				rc.move(dirs[whichDir]);
	    		}
    		}
    		//End trying to move code
    		
    		//What do I build code
    		int numTrees = rc.getTreeCount();
    		int numRobots = rc.getRobotCount();
    		
	   		if(numTrees < numRobots && (int)rc.senseNearbyTrees(3.0f, myTeam).length <= 4 && !looking && numRobots > 1)
	   			buildtree = true;
	   		else
	   			buildtree = false;
	    		
			for (Direction place : dirs) {
				//if(sad!=null && rc.isLocationOccupied(sad.add(place)))
				//	continue; 
				if (rc.canPlantTree(place) && buildtree) { 
					rc.plantTree(place);
				}
					if(rc.senseNearbyTrees(3f, Team.NEUTRAL).length > 3 || lumbers < soldiers/4 || baddirs > 2) {
						if (rc.canBuildRobot(RobotType.LUMBERJACK, place) && rc.isBuildReady()) {
							rc.buildRobot(RobotType.LUMBERJACK, place);
							lumbers++;
						}
					}
				
					else {
						if (rc.canBuildRobot(RobotType.SOLDIER, place) && rc.isBuildReady()) {
							rc.buildRobot(RobotType.SOLDIER, place);
							soldiers++;
						}
					}
					
					
			}
    		
    		
    		if(rc.getTeamBullets() >= 10000){
    			rc.donate(10000f);
    		}
			float b=rc.getTeamBullets();
			if (rc.getRoundLimit()-rc.getRoundNum()<400) {
				rc.donate(b-(b%10));
			}
            Clock.yield();
        }
    }
    public static void pickDest() {
    	
    }
    static void shakeATree(RobotController rc) throws GameActionException {
        TreeInfo[] trees = rc.senseNearbyTrees(2);
        if (trees.length == 0) return;
        int maxBullets = trees[0].containedBullets;
        int bestTree = 0;
        for (int i = trees.length - 1; i != 0; i--) {
            if (trees[i].containedBullets > maxBullets) {
                maxBullets = trees[i].containedBullets;
                bestTree = i;
            }
        }
        rc.shake(trees[bestTree].ID);
    }
    public static boolean isBad(RobotController rc, Direction place, Direction[] in) {
    	for(int i = in.length - 1; i >= 0; i--) {
    		if(place.equals(in[i], (float)(Math.PI/8.0)))
    			return true;
    	}
    	return false; 
    }
}