package airforcezero;
import battlecode.common.*;
import java.lang.Math;

public class Archon {
	static int targetCreation = -1;
	static int garCount = 0;
	static int arCount = 0;
	static int lastCreatedRound = 0;
	static Direction[] dirs={Direction.getNorth(),Direction.getSouth(),Direction.getEast(),Direction.getWest(),
			Direction.getNorth().rotateRightDegrees(45F),Direction.getSouth().rotateRightDegrees(45F),
			Direction.getEast().rotateRightDegrees(45F),Direction.getWest().rotateRightDegrees(45F)};
	static int dLen = dirs.length;
	static MapLocation destination = null;
	static boolean isBlockingGardener = false;
    public static void run(RobotController rc) throws GameActionException {

		MapLocation[] archons =rc.getInitialArchonLocations(rc.getTeam());
		MapLocation[] enemyArchons=rc.getInitialArchonLocations(rc.getTeam().opponent());
		arCount = archons.length;
		rc.broadcast(30,30+arCount); //for soldier comm channels
		Soldier.rc=rc;
		for (int i=enemyArchons.length-1; i>=0; i--) {
			Soldier.reportCombatLocation(enemyArchons[i],0);
		}
        while(true)
        {
        	MapLocation myLoc = rc.getLocation();
        	int round = rc.getRoundNum();
        	Direction build = null;
        	RobotInfo[] nearRobot = rc.senseNearbyRobots();
        	TreeInfo[] trees = rc.senseNearbyTrees();
        	BulletInfo[] bullets = rc.senseNearbyBullets(8);

        	//shake trees
        	shakeATree(rc);
        	//hire gardener
        	if( round < 2 || (round >= 40 && garCount < 14/arCount) && targetCreation == -1 )
        	{
        		for( int x = 0; x < dLen; x++ )
        		{
        			build = dirs[x];
        			if( rc.canHireGardener(build) )
        			{
        				rc.hireGardener(build);
        				lastCreatedRound = round;
        				garCount += 1;
        				targetCreation = rc.senseRobotAtLocation(myLoc.add(build, 3.01F)).getID();
        				//System.out.println(""+targetCreation);
        				break;
        			}
        		}
        	}
        	//System.out.println("Hi");
        	//try not to get killed
        	//wtf logic FIX IT
        	//if isBlockingGardener = true needToDodgeAndMove = false else returns false but moves archon and false
        	if( needToDodgeAndMove(rc, bullets) || isBlockingGardener )	//sets isBlockingGardener true if don't move
        	{
        		//System.out.print("Dog");
        		isBlockingGardener = false;
        		Clock.yield();
        	}
        	//try to move to gardener
        	if( targetCreation != -1)
        	{
        		if( rc.canSenseRobot(targetCreation) )
        		{
        			float dest = myLoc.distanceTo(rc.senseRobot(targetCreation).location);
        			//target not in correct range
        			if( dest > 5F )
            		{
        				destination = rc.senseRobot(targetCreation).location;
        				rc.move(Nav.archonNav(rc, trees, nearRobot));
        				Clock.yield();
            		}
        		}
        		if( round - lastCreatedRound > 15 && round > 40 )
        		{
        			if( moveToEmptyArea(rc) )
        			{
        				//System.out.println("Found empty area");
        				targetCreation = -1; //signal that can build in this area next round
        				Clock.yield();
        			}
        		}
        		/*else
        			System.out.println("Can't see");*/
        	}
        	//can't sense give up create gardener
        	else
        	{
        		if( moveToEmptyArea(rc) )
        			Clock.yield();
        		Direction ran = new Direction((float)Math.random() * 2 * (float)Math.PI);
        		if( rc.canMove(ran) )
        			rc.move(ran);
        		//else if( build < )
        	}
			float b=rc.getTeamBullets();
			if (rc.getRoundLimit()-rc.getRoundNum()<400) {
				rc.donate(b-(b%10));
			}
            Clock.yield();
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
    //Look in 45 degree semicircles if len of full robots <= 1 radius of 3.01 == good for gardener
    //Look in 45 degree semicircles if len of full robots <= 1 radius of 4 == good for archon
    public static boolean isEmptyArea(RobotController myR, MapLocation center) throws GameActionException
    {
    	int count = 0;
    	for( int x = 0; x < dLen/2; x++ )
    	{
    		MapLocation newLoc = center.add(dirs[x], 1F);
    		if( myR.onTheMap(newLoc) )
    		{
    			if( !myR.isLocationOccupied(center.add(dirs[x], 1F)) )
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
    	RobotInfo gardener = null;
    	MapLocation gLoc = null;
    	if( targetCreation != -1 )
    	{
    		if( myR.canSenseRobot(targetCreation) )
    		{
    			gardener = myR.senseRobot(targetCreation);
    			gLoc = myR.getLocation();
    		}
    	}
    	boolean runAway = false;
    	for( BulletInfo bull: b )
    	{
    		Direction head = bull.getDir();
    		MapLocation loc = bull.getLocation();
    		float speed = bull.getSpeed();
    		MapLocation end = loc.add(head, speed*2);
    		boolean hitMe = bulletWillHit(loc, end, myR.getLocation());
    		if( hitMe )
    			runAway = true;
    		if( gardener != null )
    		{
    			if( speed*2 > loc.distanceTo(gLoc) )
    				break;
    			if( hitMe && bulletWillHit(loc, end, gLoc) )
    			{
    				isBlockingGardener = true;
    				return false;
    			}
    		}
    	}
    	isBlockingGardener = false;
    	if( runAway )
    	{
    		//make better run away actually use bullets
    		if( moveToEmptyArea(myR) )
    			return true;
    		//rip
    		Direction ran = new Direction((float)Math.random() * 2 * (float)Math.PI);
    		if( myR.canMove(ran) )
    			myR.move(ran);
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
    	  //          -o->             --|-->  |            |  --|->
    	  //    (t1 hit,t2 hit),    (t1 hit,t2>1),      (t1<0, t2 hit), 
    	  //       ->  o               o ->           | -> |
    	  //     (t1>1,t2>1),      (t1<0,t2<0),     (t1<0, t2>1)

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