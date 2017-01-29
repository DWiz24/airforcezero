package airforcezero;
import battlecode.common.*;

public class Gardener {
    public static void run(RobotController rc) throws GameActionException {
    	Direction[] dirs={new Direction(0f), new Direction((float)(Math.PI/3.0)), new Direction((float)(2.0*Math.PI/3.0)), new Direction((float)(3.0*Math.PI/3.0)), new Direction((float)(4.0*Math.PI/3.0)), new Direction((float)(5.0*Math.PI/3.0))};
    	final int roundSpawned = rc.getRoundNum();
    	int soldiers = 0, lumbers = 0, planted = 0, lastRoundPlanted = rc.getRoundNum(), lastRoundCreated = rc.getRoundNum();    	
    	int scounts = 2;
    	int channel = -1;
    	int censusChannel = 1;
    	float theta = -1.0f;
    	float lastTurnHealth = rc.getHealth();
    	boolean onSpawn = true, dead = false;
    	Team myTeam = rc.getTeam();
        Team enemyTeam = myTeam.opponent();
    	
    	//Finds the shortest distance between our archons vs their archons
    	MapLocation[] theirArchons = rc.getInitialArchonLocations(enemyTeam);
		MapLocation[] myArchons = rc.getInitialArchonLocations(myTeam);
		float distance = 99999f;
		if(myArchons.length == 1) {
			distance = myArchons[0].distanceTo(theirArchons[0]);
		} else {
			for(int i = theirArchons.length-1; i >= 0; i--) {
				for(int n = myArchons.length-1; i>=0; i--) {
					float thisDistance = myArchons[n].distanceTo(theirArchons[i]); 
					if(thisDistance < distance) {
						distance = thisDistance;
					}
				}
			}
		}
		
    	while(true){
    		
    		MapLocation sad = null; //Find lowest health tree, water it
    		float minhealth = 50f; 
    		
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
    		//End water and shaking
    		
    		
    		//Communicate information to team array - first to census [0,5], then to archon for other stuff
    		if(onSpawn) {
    			rc.broadcast(censusChannel, rc.readBroadcast(censusChannel) + 1);
    			onSpawn = false;
    		}
    		if(PublicMethods.isAboutToDie(rc, lastTurnHealth) && !dead) {
    			rc.broadcast(censusChannel, rc.readBroadcast(censusChannel) - 1);
    			dead = true;
    		}
    		
    		MapLocation myLocation = rc.getLocation();
    		int directionsICantPlant = 0, directionsICanPlant = 0;
   			boolean archonInWay = false;
   			for(int i = dirs.length-1; i >= 0; i--) {
   				if(!rc.onTheMap(myLocation.add(dirs[i], 2.01f), 1f) || rc.isCircleOccupiedExceptByThisRobot(myLocation.add(dirs[i], 2.01f), 1f)) {
   					directionsICantPlant++;
   					RobotInfo inWay = rc.senseRobotAtLocation(myLocation.add(dirs[i], 2.01f));
   					if(inWay != null && inWay.type == RobotType.ARCHON) {
   						archonInWay = true;
   					}
   				} else {
   					directionsICanPlant++;
   				}
   			} 
   			
   			if(directionsICanPlant==0) {
   				boolean foundGoodDirection = false;
   				float direction = 0f;
   				while(!rc.canBuildRobot(RobotType.LUMBERJACK, new Direction((float) (direction))) && direction < (float)(2.0*Math.PI)) {
   					direction += (float)(Math.PI/48.0);
   					if(rc.canBuildRobot(RobotType.LUMBERJACK, new Direction((float) (direction)))) {
   						foundGoodDirection = true;
   						break;
   					}
   					//System.out.println(direction);
   				}
   				
   				if(foundGoodDirection) {
   	   				for(int i = 0; i < dirs.length; i++)
   	   					dirs[i] = new Direction(direction + (float)(((double)i*Math.PI/3.0)%(2.0*Math.PI)));
   	   			}
   			}
   			
   			//System.out.println("I can plant in this many directions: " + directionsICanPlant);
    		if(channel == -1) {
    			int tempchannel = 100;
    			while(rc.readBroadcast(tempchannel) != 0)
    				tempchannel++;
    			channel = tempchannel;
    		}
    		//System.out.println("My channel is " + channel);
    		int x = (int)myLocation.x;
    		int y = (int)myLocation.y;
    		int planting = 0b0000_0001;
    		if((directionsICanPlant < 2 || rc.getRoundNum() - lastRoundPlanted > 200) && ((lumbers + soldiers > 0) || directionsICanPlant == 0))
    			planting = 0b0000_0000;
    		if(archonInWay)
    			planting = 0b0000_0001;
    		int message = (((x << 12) + y) << 12) + planting;
    		rc.broadcast(channel, message);
    		
    		if(rc.getHealth() < 5) //If I'm about to die, clear my spot
    			rc.broadcast(channel, 0);
    		//System.out.println("msg " + message);
    		
    		//Before I've determined the best theta, I should figure out if it's even worth moving

   			
   			if(directionsICantPlant == 0)
   				theta = 0f;
   			else
   				theta = -1.0f; //I should then find a better spot
   			
    		//Moves to a good space for planting, based on more successful teams, changes turn to turn, stops trying after a certain number of turns
			
   			if(theta <= -2.0f) {
   				
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
   					//System.out.println("going to move");
   					rc.move(toMove);
   				}
   			}
    		//End trying to move code
    		
   			
    		//What do I build code
    		boolean buildtree = false;
    		
    		//int countedSoliders = get from team shared array
    		//TreeInfo[] nearbyTrees = rc.senseNearbyTrees();
    		
    		boolean safe = true;
    		if(distance < 20f && rc.getRoundNum() < 100) {
    			safe = false;
    		}
    		RobotInfo[] allRobots = rc.senseNearbyRobots(5f);
    		for(RobotInfo thisRobot : allRobots) {
    			if(thisRobot.team == enemyTeam)
    				safe = false;
    		}
    		
	   		if(directionsICanPlant > 1 && safe && planted < soldiers*2 && soldiers >= 1) {
	   			buildtree = true;
	   		} else {
	   			buildtree = false;
	   		}
	   		if(soldiers+lumbers+planted == 0)
	   			safe = false;
			for (Direction place : dirs) {
				//if(sad!=null && rc.isLocationOccupied(sad.add(place)))
				if (rc.canPlantTree(place) && buildtree) { 
					rc.plantTree(place);
					lastRoundPlanted = rc.getRoundNum();
					lastRoundCreated = rc.getRoundNum();
					planted++;
				}
					//if(((rc.senseNearbyTrees(3f, Team.NEUTRAL).length > 1 && directionsICantPlant > 1) && (directionsICantPlant >= 4 || planted < 3)) && ((float)lumbers < (float)soldiers/(2f + rc.getRoundNum()/300f))) {
					if((rc.senseNearbyTrees(7f, Team.NEUTRAL).length > 0) && lumbers < 1 && safe) {
						if (rc.canBuildRobot(RobotType.LUMBERJACK, place) && rc.isBuildReady()) {
							lastRoundCreated = rc.getRoundNum();
							rc.buildRobot(RobotType.LUMBERJACK, place);
							lumbers++;
						}
					}
				
					else {
						if (rc.canBuildRobot(RobotType.SOLDIER, place) && rc.isBuildReady()) {
							rc.buildRobot(RobotType.SOLDIER, place);
							lastRoundCreated = rc.getRoundNum();
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
