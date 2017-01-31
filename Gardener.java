package airforcezero;
import battlecode.common.*;

public class Gardener {
    public static void run(RobotController rc) throws GameActionException {
    	
    	Direction[] dirs={new Direction(0f), new Direction((float)(Math.PI/3.0)), new Direction((float)(2.0*Math.PI/3.0)), new Direction((float)(3.0*Math.PI/3.0)), new Direction((float)(4.0*Math.PI/3.0)), new Direction((float)(5.0*Math.PI/3.0))};
    	final int roundSpawned = rc.getRoundNum();
    	int soldiers = 0, lumbers = 0, planted = 0, lastRoundPlanted = rc.getRoundNum();
    	int spotsINeed = 1;
    	int channel = -1;
    	int censusChannel = 1;
    	int myLumbers = 0, mySoldiers = 0;
    	float theta = -1.0f;
    	float lastTurnHealth = rc.getHealth();
    	boolean onSpawn = true, dead = false;
    	Team myTeam = rc.getTeam();
        Team enemyTeam = myTeam.opponent();
    	//System.out.println("I AM THE " + id);
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
    		
    		lumbers = rc.readBroadcast(2);
    		soldiers = rc.readBroadcast(3);
    		//System.out.println(lumbers + ", " + soldiers);
    		
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
    		int x = (int)myLocation.x;
    		int y = (int)myLocation.y;
    		int planting = 0b0000_0001;
    		if(dead || (((directionsICanPlant < 2 || rc.getRoundNum() - lastRoundPlanted > 200) && ((lumbers + soldiers > 0) || directionsICanPlant == 0)) && !archonInWay))
    			planting = 0b0000_0000;
    		
    		
    		int message = (((x << 12) + y) << 12) + planting;
    		rc.broadcast(channel, message);
    		
    		if(rc.getHealth() < 5) //If I'm about to die, clear my spot
    			rc.broadcast(channel, 0);
    		
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
    		
   			//Check scouts and soldiers close code
   			boolean gettingShotAt = false;
   			RobotInfo[] nearbyRobots = rc.senseNearbyRobots(3f, enemyTeam);
   			for(RobotInfo badguy : nearbyRobots) {
   				if(badguy.type == RobotType.SCOUT) {
   					if(lumbers == 0)
   						rc.broadcast(200, rc.readBroadcast(200) + 1);
   					Lumberjack.lumberjackNeeded(rc, badguy.location, 15, 1, 0);
   				}
   				
   				if(badguy.type == RobotType.SOLDIER) {
   					gettingShotAt = true;
   				}
   			}
   			
    		//What do I build code
    		boolean buildtree = false;
    		
    		boolean safe = true;
    		if((distance < 20f && rc.getRoundNum() < 50)) {
    			//System.out.println("too close");
    			safe = false;
    		}
    		
    		lastTurnHealth = rc.getHealth(); 
    		RobotInfo[] allRobots = rc.senseNearbyRobots(); 
    		for(RobotInfo thisRobot : allRobots) {
    			if(thisRobot.team == enemyTeam && thisRobot.type!=RobotType.SCOUT) {
    				safe = false;
					MapLocation loc = thisRobot.location;
					gotoHacks:
					{
						for (int i = 31; i <= 45; i++) {
							int m = rc.readBroadcast(i);
							if (m != 0) {
								MapLocation map = Soldier.getLocation(m);
								if (map.distanceTo(loc) < 8) {
									break gotoHacks;
								}
							}
						}
						//System.out.println("I signaled");
						//rc.setIndicatorDot(loc,0,0,255);
						Soldier.reportCombatLocation(loc, 0);
					}
                    break;
                }
    		}

	   		if(soldiers+lumbers+planted == 0)
	   			safe = false;
    		
	   		boolean startEconomy = false;
	   		if(planted == 0 && mySoldiers > 0)
	   			startEconomy = true;

	   		if(directionsICanPlant > spotsINeed && ((safe && soldiers > 0)) || startEconomy) {
	   			buildtree = true;
	   		} else {
	   			buildtree = false;
	   		}
	   		
	   		int threshold = 10;
	   		int secondThreshold = 20;
	   		
	   		TreeInfo[] nearbyTrees = rc.senseNearbyTrees(10f, Team.NEUTRAL);
	   		int lumbersNeeded = -1;
	   		if((nearbyTrees.length == 0 && distance > 40f) || (rc.getRoundNum() < 100 && nearbyTrees.length < threshold)) {
	   			lumbersNeeded = 0;
	   		} else if (nearbyTrees.length < threshold || rc.readBroadcast(200) > 1) {
	   			lumbersNeeded = 1;
	   		} else if (nearbyTrees.length < secondThreshold){
	   			lumbersNeeded = 2;
	   		} else {
	   			lumbersNeeded = 3;
	   		}

	   		for (Direction place : dirs) {
				if (rc.canPlantTree(place) && buildtree) { 
					rc.plantTree(place);
					lastRoundPlanted = rc.getRoundNum();
					planted++;
				}
					//if(((rc.senseNearbyTrees(3f, Team.NEUTRAL).length > 1 && directionsICantPlant > 1) && (directionsICantPlant >= 4 || planted < 3)) && ((float)lumbers < (float)soldiers/(2f + rc.getRoundNum()/300f))) {
					if((((rc.senseNearbyTrees(10f, Team.NEUTRAL).length > 0) && myLumbers < lumbersNeeded) && lumbers < 10 && !gettingShotAt)) {
						if (rc.canBuildRobot(RobotType.LUMBERJACK, place) && rc.isBuildReady()) {
							rc.buildRobot(RobotType.LUMBERJACK, place);
							myLumbers++;
						}
					} else if(safe && soldiers > 1 && rc.readBroadcast(5) < 1 && rc.getRoundNum() < 500) {
						if(rc.canBuildRobot(RobotType.SCOUT, place)) {
							rc.buildRobot(RobotType.SCOUT, place);
						}
					} else {
						if (rc.canBuildRobot(RobotType.SOLDIER, place) && rc.isBuildReady()) {
							rc.buildRobot(RobotType.SOLDIER, place);
							mySoldiers++;
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
