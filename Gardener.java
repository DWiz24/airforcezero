package airforcezero;
import battlecode.common.*;
public class Gardener {
    public static void run(RobotController rc) throws GameActionException {
    	final int roundSpawned = rc.getRoundNum();
    	int soldiers = 0;
    	int lumbers = 0;
    	float theta = -1.0f;
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
    		
    		//Moves to a good space for planting, based on more successful teams, changes turn to turn, stops trying after a certain number of turns
			MapLocation myLocation = rc.getLocation();

    		// 
    		
   			if(theta < 0.0) {
   				float deltaTheta = (float)(Math.PI/12.0);
   				
   				RobotInfo[] allRobots = rc.senseNearbyRobots();
   				TreeInfo[] allTrees = rc.senseNearbyTrees(7f, Team.NEUTRAL);
   				
   				float bestTheta = -1.0f;
   				int fewestThings = 1000;
   				
   				theta = 0.0f;
   				
   				while(theta < (float)(2.0*Math.PI)) { //so i only test the unit circle
   					if(!rc.canMove(new Direction(theta))) {
    					theta += deltaTheta;
    					continue;
    				}
   					
   					int thingsInMyWay = 0;
    				Direction thisDir = new Direction(theta);
    				
    				for(int i = allRobots.length-1; i >= 0; i--) {
    					if(thisDir.equals(myLocation.directionTo(allRobots[i].getLocation()), deltaTheta))
    						thingsInMyWay++;
    				}
    				
    				for(int i = allTrees.length-1; i >= 0; i--) {
    					if(thisDir.equals(myLocation.directionTo(allTrees[i].getLocation()), deltaTheta))
    						thingsInMyWay++;
    				}
    				
    				if(thingsInMyWay == 0) {
    					bestTheta = theta;
    					break;
    				}
    				
    				//System.out.println(thingsInMyWay + " in this direction " + theta);
    				if(thingsInMyWay < fewestThings) {
    					bestTheta = theta;
    					fewestThings = thingsInMyWay;
    				}
    				
    				theta += deltaTheta;
    				//System.out.println(theta);
    			}
   				if(bestTheta > -1.0)
   					theta = bestTheta;
    		}
   			
   			//System.out.println(theta);
   			
   			//Now I've determined the best theta, I should figure out if it's even worth moving
   			int directionsICantPlant = 0;
   			
   			for(int i = dirs.length-1; i >= 0; i--)
   				if(rc.isLocationOccupied(myLocation.add(dirs[i], 2f)))
   					directionsICantPlant++;
   			
   			//System.out.println("Cannot plant in this many directions" + directionsICantPlant);
   			
   			if(theta>=0.0 && directionsICantPlant > 1) {
   				Direction toMove = new Direction(theta);
   				if(rc.canMove(toMove)) {
   					//System.out.println("gunna move");
   					rc.move(toMove);
   				}
   			}
    		//End trying to move code
    		
    		//What do I build code
    		int numTrees = rc.getTreeCount();
    		int numRobots = rc.getRobotCount();
    		boolean buildtree = false;
    		
	   		if(numTrees < numRobots && numRobots > 1)
	   			buildtree = true;
	   		else
	   			buildtree = false;
	    		
			for (Direction place : dirs) {
				//if(sad!=null && rc.isLocationOccupied(sad.add(place)))
				//	continue; 
				if (rc.canPlantTree(place) && buildtree) { 
					rc.plantTree(place);
				}
				/*	if((rc.senseNearbyTrees(3f, Team.NEUTRAL).length > 4 || lumbers < soldiers/6 || baddirs > 3) && soldiers < 10) {
						if (rc.canBuildRobot(RobotType.LUMBERJACK, place) && rc.isBuildReady()) {
							rc.buildRobot(RobotType.LUMBERJACK, place);
							lumbers++;
						}
					}*/
				
					//else {
						if (rc.canBuildRobot(RobotType.SOLDIER, place) && rc.isBuildReady()) {
							rc.buildRobot(RobotType.SOLDIER, place);
							soldiers++;
						}
					//}
					
					
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