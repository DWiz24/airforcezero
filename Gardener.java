package airforcezero;
import battlecode.common.*;

public class Gardener {
    public static void run(RobotController rc) throws GameActionException {
    	final int roundSpawned = rc.getRoundNum();
    	int soldiers = 0, lumbers = 0, planted = 0;    	
    	int channel = -1;
    	float theta = -1.0f;
        while(true){
        	
        	//RobotType[] canBuild = {RobotType.SOLDIER, RobotType.LUMBERJACK, RobotType.TANK, RobotType.SCOUT};
        	
    		Direction[] dirs={new Direction(0f), new Direction((float)(Math.PI/3.0)), new Direction((float)(2.0*Math.PI/3.0)), new Direction((float)(3.0*Math.PI/3.0)), new Direction((float)(4.0*Math.PI/3.0)), new Direction((float)(5.0*Math.PI/3.0))}; 
    		
    		MapLocation sad = null; //Find lowest health tree, water it
    		float minhealth = 50f; 
    		Team myTeam = rc.getTeam();
    		TreeInfo[] trees = rc.senseNearbyTrees(3.0f, myTeam);
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
    		
    		//Communicate information to team array...currently takes first available spot
    		if(channel == -1) {
    			int tempchannel = 100;
    			while(rc.readBroadcast(tempchannel) != 0)
    				tempchannel++;
    			channel = tempchannel;
    		}
    		
    		MapLocation myLocation = rc.getLocation();
    		int x = (int)myLocation.x;
    		int y = (int)myLocation.y;
    		int planting = 0b0000_0001;
    		if(trees.length > 4)
    			planting = 0b0000_0000;
    		int message = (((x << 12) + y) << 12) + planting;
    		//message = (message << 12) + planting;
    		rc.broadcast(channel, message);
    		
    		if(rc.getHealth() < 5) //If I'm about to die, clear my spot
    			rc.broadcast(channel, 0);
    		//System.out.println("msg " + message);
    		
    		//Before I've determined the best theta, I should figure out if it's even worth moving
   			int directionsICantPlant = 0;
   			
   			for(int i = dirs.length-1; i >= 0; i--)
   				if(rc.isLocationOccupied(myLocation.add(dirs[i], 2f))) {
   					directionsICantPlant++;
   				}
   			
   			if(directionsICantPlant == 0)
   				theta = 0f;
   			else
   				theta = -1.0f; //I should then find a better spot
   			
    		//Moves to a good space for planting, based on more successful teams, changes turn to turn, stops trying after a certain number of turns
			
   			if(theta < -2.0f) { //For now, unable to move at all
   				
   				float deltaTheta = (float)(Math.PI/6.0);
   				
   				RobotInfo[] allRobots = rc.senseNearbyRobots();
   				TreeInfo[] allTrees = rc.senseNearbyTrees(7f, Team.NEUTRAL);
   				
   				float bestTheta = -1.0f;
   				int fewestThings = 1000;
   				
   				float currentTheta = 0.0f;
   				
   				while(currentTheta < (float)(2.0*Math.PI)) { //so i only test the unit circle
   					Direction thisDir = new Direction(currentTheta);
   					
   					if(rc.isLocationOccupied(myLocation.add(thisDir, 2f))) {
    					currentTheta += deltaTheta;
    					continue;
    				}
   					
   					int thingsInMyWay = 0;
   					
    				for(int i = allRobots.length-1; i >= 0; i--) {
    					if(thisDir.equals(myLocation.directionTo(allRobots[i].getLocation()), deltaTheta))
    						thingsInMyWay++;
    				}
    				
    				for(int i = allTrees.length-1; i >= 0; i--) {
    					if(thisDir.equals(myLocation.directionTo(allTrees[i].getLocation()), deltaTheta))
    						thingsInMyWay++;
    				}
    				
    				if(thingsInMyWay == 0) {
    					bestTheta = currentTheta;
    					break;
    				}
    				
    				//System.out.println(thingsInMyWay + " in this direction " + currentTheta);
    				if(thingsInMyWay < fewestThings) {
    					bestTheta = currentTheta;
    					fewestThings = thingsInMyWay;
    				}
    				
    				currentTheta += deltaTheta;
    				//System.out.println(theta);
    			}
   				if(bestTheta > -1.0)
   					theta = bestTheta;
    		}
   			
   			//System.out.println("theta: " + theta);
   			
   			if(theta>=0.0 && directionsICantPlant > 1) {
   				Direction toMove = new Direction(theta);
   				if(rc.canMove(toMove)) {
   					//System.out.println("gunna move");
   					rc.move(toMove);
   				}
   			}
    		//End trying to move code
    		
    		//What do I build code
    		boolean buildtree = false;
    		
	   		if(planted == 4 || (planted < 5 && planted < soldiers*2 && soldiers > 1))
	   			buildtree = true;
	   		else
	   			buildtree = false;
	    		
			for (Direction place : dirs) {
				//if(sad!=null && rc.isLocationOccupied(sad.add(place)))
				if (rc.canPlantTree(place) && buildtree) { 
					rc.plantTree(place);
					planted++;
				}
					if((rc.senseNearbyTrees(3f, Team.NEUTRAL).length > 2) && soldiers < 2) {
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
			PublicMethods.donateBullets(rc);

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