package airforcezero;
import battlecode.common.*;
import java.lang.Math;

public class Archon {
	static int arCount = 0;
	static Direction[] dirs={Direction.getNorth(),Direction.getSouth(),Direction.getEast(),Direction.getWest(),
			Direction.getNorth().rotateRightDegrees(45F),Direction.getSouth().rotateRightDegrees(45F),
			Direction.getEast().rotateRightDegrees(45F),Direction.getWest().rotateRightDegrees(45F)};
	static int dLen = dirs.length;
	static MapLocation destination = null;
	static boolean isBlockingGardener = false;
	static int myIndex = -1;
	static int[] myAr = new int[3];
	static Direction lastHired = null;
    public static void run(RobotController rc) throws GameActionException {

		MapLocation[] archons =rc.getInitialArchonLocations(rc.getTeam());
		MapLocation[] enemyArchons=rc.getInitialArchonLocations(rc.getTeam().opponent());
		arCount = archons.length;
		for( int x = arCount-1; x>=0; x-- )
		{
			if( archons[x].equals(rc.getLocation()) )
			{
				myIndex = 80+x;
				//System.out.println("Index" + myIndex);
				break;
			}
		}
		if (rc.getRoundNum()<=2) {
			//rc.broadcast(30, 30 + arCount); //for soldier comm channels
			Soldier.rc = rc;
			for (int i = arCount - 1; i >= 0; i--) {
				Soldier.reportCombatLocation(enemyArchons[i], 0b10000000);
			}
		}
        while(true)
        {
        	
        	boolean moved = false;
        	MapLocation myLoc = rc.getLocation();
        	int round = rc.getRoundNum();
        	Direction build = null;
        	RobotInfo[] nearRobotEnemies = rc.senseNearbyRobots(5F, rc.getTeam().opponent());
        	TreeInfo[] trees = rc.senseNearbyTrees();
        	RobotInfo[] robots = rc.senseNearbyRobots();
        	//BulletInfo[] bullets = rc.senseNearbyBullets(8);
        	
        	//shake trees
        	if( rc.canShake() )
        		shakeATree(rc);
        	//hire gardener

        	boolean roomForGardeners = false;
        	//boolean isGardener = false;
        	boolean makeG = true;
        	MapLocation[] gardeners = new MapLocation[15];
        	int tc = 0;
        	for (int i=100; i<=114; i++)
        	{
        		int mes = rc.readBroadcast(i);
        		if( mes == 0 )
        		{
        			roomForGardeners = true;
        			continue;
        		}
        		//isGardener = true;
        		float garX = (mes>>>20)/4.0F;
        		float garY = ((mes<<12)>>8)/4.0F;
  				int priority = mes&0b11111111;	//if all 0 and roomforgardeners then make gardener
  				//System.out.println("priority"+priority);
  				if( priority != 0 )
  				{
  					makeG = false;
  					//System.out.println("FALSE");
  				}
  				gardeners[tc] = new MapLocation(garX, garY);
        	}
        	if( needToDodgeAndMove(rc, rc.senseNearbyBullets()) )
        		moved = true;
        	int s = rc.senseNearbyTrees(6.5F).length+robots.length;
            s = (s<<8)+0b00000001; //same as 0 but 0 = dead or non existant
            reportBuildStatus(rc, s);
            //System.out.println("makeG"+makeG);
           // System.out.println("roomForG"+roomForGardeners);
            //System.out.println("Index"+(myIndex-80+2));
            boolean hired = false;
        	if( round >= (myIndex-80+3) && makeG && roomForGardeners && (nearRobotEnemies.length == 0 || rc.getTeamBullets() >= 184) )
        	{
        		//System.out.println("Index"+(myIndex-80+2));
        		//System.out.println("MAKING");
        		if( pickArchon(rc, robots.length) )
        		{	
        			//System.out.println("Is Archon");
        			//choose gardener direction
        			Direction[] Gardirs = dirs;
        			if( lastHired != null )
        			{
        				Direction[] temp = {lastHired, lastHired.rotateRightDegrees(60F), lastHired.rotateLeftDegrees(60F),
        						lastHired.rotateRightDegrees(90F), lastHired.rotateLeftDegrees(90F), 
        						lastHired.rotateRightDegrees(120F), lastHired.rotateLeftDegrees(120F), 
        						lastHired.rotateRightDegrees(150F), lastHired.rotateLeftDegrees(150F)};
        				Gardirs = temp;
        			}
        			for( int x = 0; x < Gardirs.length; x++ )
        			{
        				build = Gardirs[x];
        				if( rc.canHireGardener(build) )
        				{
        					lastHired = build;
        					rc.hireGardener(build);
        					hired = true;
        					Direction[] test = {build.opposite(), build.rotateLeftDegrees(90F), build.rotateLeftDegrees(90F), build.rotateRightDegrees(90F)};
        					for(Direction t:test)
        					{
        						int count = 0;
        						for( float i = 5F; i > 0; i-- )
        						{
        							if( rc.canMove(myLoc.add(t, i)) )
        								count++;
        						}
        						if( count < 3 )
        							continue;
        						if( rc.onTheMap(myLoc.add(t, 8F)) && !rc.isLocationOccupiedByTree(myLoc.add(t, 6F)) )
        						{
        							destination = myLoc.add(t, 6F);	//runaway
        							break;
        						}
        					}
        				   //System.out.println("created");
        				}
        			}
        			//System.out.println("FS");
        			if( !hired )
        			{
        				//int t = rc.senseNearbyTrees(6.5F).length;
        				s = ((s>>8)<<8)+0b00000100; //same as 0 but 0 = dead or non existant
        				reportBuildStatus(rc, s);
        			}
        		}
        	}
        	//System.out.println("here");
        	//try to move away from gardener
        	if( !moved && destination != null && myLoc.distanceTo(destination) > 4F )
        	{
        			//System.out.println("Still gard??");
        			//System.out.println("" + destination.x + " "+destination.y);
        			
        			rc.move(Nav.archonNav(rc, trees, robots));
        			moved = true;
        	}
        	//away from soldier or no destination
        	else if(!moved)
        	{
        		//System.out.println("soldier sit");
        		boolean reset = false;
        		RobotInfo[] nearAll = rc.senseNearbyRobots(6F, rc.getTeam());
        		for( RobotInfo r: nearAll )
        		{
        			if( r.type == RobotType.SOLDIER )
        			{
        				Direction goA = myLoc.directionTo(r.location);
        				Direction[] test = {goA.opposite(), goA.rotateRightDegrees(45F), goA.rotateLeftDegrees(90F), goA.rotateRightDegrees(90F)};
        				for(Direction t:test)
        				{
        					if( rc.onTheMap(myLoc.add(t, 8F)) && !rc.isLocationOccupiedByTree(myLoc.add(t, 6F)))
        					{
        						//System.out.println("Runaway");
        						destination = myLoc.add(t, 6F);	//runaway
        						reset = true;
        						rc.move(Nav.archonNav(rc, trees, robots));
        						moved = true;
        						break;
        					}
        				}
        			}
        			if( reset )
        				break;
        		}
        		if( !hired && !moved )
        		{
        			//System.out.println("Try to runtree");
        			if( trees.length > 0 )
        			{
        				Direction runTree = myLoc.directionTo(trees[(int)(Math.random()*(trees.length-1))].location);
        				Direction[] treeTry = {runTree.opposite(), runTree.rotateLeftDegrees(90F), runTree.rotateRightDegrees(90F)};
        				for( int x = 0; x < 3; x++ )
        				{
        					if( rc.canMove(treeTry[x]) )
        					{
        						rc.move(treeTry[x]);
        						moved = true;
        						break;
        					}
        				}
        			}
        			if( !moved )
        			{
        				Direction ran = new Direction((float)Math.random() * 2 * (float)Math.PI);
        				if( rc.canMove(ran) )
        					rc.move(ran);
        			}
        		}
        	}
			PublicMethods.donateBullets(rc);
            Clock.yield();
        }
    }
    public static boolean pickArchon(RobotController r, int aroundRobs) throws GameActionException
    {
    	int fourCount = 0;
    	int stat = ((r.senseNearbyTrees(6.5F).length+aroundRobs)<<8) + 0b00000001;
    	if( r.getHealth() < 6 )
    		stat = 0;
    	reportBuildStatus(r, stat);
    	for (int i=80; i<=82; i++)
    	{
    		int mes = r.readBroadcast(i);
    		//System.out.println("mes" + (mes>>8) + " " + (mes&0b11111111));
    		//System.out.println("stat" + (stat>>8) + " " + (stat&0b11111111));
    		if( mes == 0 )	//ded
    			continue;
    		if( (mes&0b11111111) == 4 )
    		{
    			fourCount++;
    			if( fourCount == arCount-1 )
    			{
    				reportBuildStatus(r, ((stat>>8)<<8)+0b00000011);
    				return true;
    			}
    		}
    		if( (mes>>>8) < (stat>>>8) )
    		{
    			//System.out.println("False stat");
    			return false;
    		}
    		if( (mes&0b11111111) == 3 )
    		{
    			//System.out.println("False prio");
    			return false;
    		}
    	}
    	reportBuildStatus(r, ((stat>>8)<<8)+0b00000011);
    	return true;
    }
    public static void reportBuildStatus(RobotController r, int myStat) throws GameActionException
    {
    	if( myIndex != -1 )
    	{
    		r.broadcast(myIndex, myStat);
    		return;
    	}
    }
    public static boolean moveToEmptyArea(RobotController myR) throws GameActionException
    {
    	//determine if current is empty
    	if( isEmptyArea(myR, myR.getLocation()) ) 
    	{
    		//System.out.println("is empty");
    		return true;
    	}
    	for( int x = 0; x < dLen; x++ )
		{
			Direction m = dirs[x];
			if( myR.canMove(m) )
			{
				//System.out.println("RunAway");
				myR.move(m);
				return true;
				/*if( isEmptyArea(myR, myR.getLocation().add(m)) )
				{
					myR.move(m);
					return true;
				}*/
			}
		}
    	return false;
    }
    //Empty = can build gardener and still move out of area
    public static boolean isEmptyArea(RobotController myR, MapLocation center) throws GameActionException
    {
    	int count = 0;
    	for( int x = 0; x < dLen/2; x++ )
    	{
    		MapLocation newLoc = center.add(dirs[x], 3F);
    		if( myR.onTheMap(newLoc) )
    		{
    			if( !myR.isLocationOccupied(center.add(dirs[x], 3F)) )
    				count++;
    		}
    	}
    	if( count >= 2 )
    		return true;
    	//if( myR.senseNearbyRobots(center, 3.01F, null).length <= 3 )
    		//return true;
    	return false;
    }
    public static boolean needToDodgeAndMove(RobotController myR, BulletInfo[] b) throws GameActionException
    {
    	Direction run = null;
    	MapLocation runLoc = null;
    	MapLocation runLocP2 = null;
    	boolean runAway = false;
    	for( int x = 0; x < b.length; x++ )
    	{
    		BulletInfo bull = b[x];
    		Direction head = bull.getDir();
    		MapLocation loc = bull.getLocation();
    		float speed = bull.getSpeed();
    		MapLocation end = loc.add(head, speed*2);
    		boolean hitMe = bulletWillHit(loc, end, myR.getLocation());
    		if( hitMe )
    		{
    			runAway = true;
    			run = head;
    			runLoc = loc;
    			if( b.length == 1 )
    				runLocP2 = loc;
    			else if( x == b.length-1 )
    				runLocP2 = b[x-1].location;
    			else
    				runLocP2 = b[x+1].location;
    		}
    	}
    	if( runAway )
    	{
    		float shotDir = runLoc.x-runLocP2.x;
    		Direction[] trying = {run.rotateLeftDegrees(45F), run.rotateLeftDegrees(90F), run.rotateRightDegrees(45F), run.rotateRightDegrees(90F)};
    		if( shotDir != 0 )
    		{
    			if( shotDir > 0 )
    			{
    				Direction[] temp = {run.rotateRightDegrees(30F), run.rotateRightDegrees(60F), run.rotateRightDegrees(90F), run.rotateRightDegrees(120F), run.rotateRightDegrees(150F)};
    				trying = temp;
    			}
    			else
    			{
    				Direction[] temp = {run.rotateLeftDegrees(30F), run.rotateLeftDegrees(60F), run.rotateLeftDegrees(90F), run.rotateLeftDegrees(120F), run.rotateLeftDegrees(150F)};
    				trying = temp;
    			}
    		}
    		for( Direction d : trying )
    		{
    			if(myR.canMove(d))
    			{
    				myR.move(d);
    				return true;
    			}
    		}
    	}
    	return false;
    }
    public static float dotProduct(float[] v1, float[] v2)
    {
    	return v1[0]*v2[0]+v1[1]*v2[1];
    }
    public static boolean bulletWillHit(MapLocation bulletStart, MapLocation bulletEnd, MapLocation robotCenter)
    {
        float[] d = {bulletEnd.x-bulletStart.x, bulletEnd.y-bulletStart.y};
        float[] f = {bulletStart.x-robotCenter.x, bulletStart.y-robotCenter.y};
    	float a = dotProduct(d, d);
    	float b = 2*dotProduct(f, d);
    	float c = dotProduct(f, f) - 2*2 ;

    	float discrim = b*b-4*a*c;
    	if( discrim < 0 )	//no 
    	  return false;
    	else
    	{
    		discrim = (float)Math.sqrt((double) discrim);
    	  // either solution may be on or off the ray so need to test both
    	  float t1 = (-b - discrim)/(2*a);
    	  float t2 = (-b + discrim)/(2*a);

    	  if( t1 >= 0 && t1 <= 1 )
    	    return true;
    	  // t1 didn't intersect so either started
    	  // inside the circle or completely past it
    	  if( t2 >= 0 && t2 <= 1 )
    	    return true ;
    	  return false ;
    	}
    }
    //Daniel's shakeTree 
    static void shakeATree(RobotController rc) throws GameActionException {
        TreeInfo[] trees = rc.senseNearbyTrees(3);	//move radius is changed that's all
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
    public static void pickDest()
    {
    	Nav.setDest(destination);
    }
}