package airforcezero;
import battlecode.common.*;

public class Scout {
	static boolean harass = false;
	static int deadArchons = 0;
	static int index = 0;
	static MapLocation[] archons = null;
    public static void run(RobotController rc) throws GameActionException 
    {
    	archons = rc.getInitialArchonLocations(rc.getTeam().opponent());
        while(true)
        {
        	Soldier.shakeATree(rc);
        	
        	MapLocation me = rc.getLocation();
        	RobotInfo[] robots = rc.senseNearbyRobots();
        	MapLocation target = null;
        	//check to see if can harass
        	//if so target = null
        	
        	//find a tree to use to protect for micro all archons dead
        	if( target == null )
        	{
        		TreeInfo[] trees = rc.senseNearbyTrees();
        		for( int i = trees.length-1; i >= 0; i-- )
            	{
        			if( trees[i].containedBullets > 0 )
        			{
        				MapLocation temp = trees[i].getLocation();
        				float tempDist = temp.distanceTo(me);
        				if( tempDist < 1F )
        					continue;
        				target = temp;
        				break;
        			}
            	}
        	}
        	//find archon or gardener in range
        	if( target == null )
        	{
        		for( int i = robots.length-1; i >= 0; i-- )
        		{
        			if( robots[i].getTeam().equals(rc.getTeam().opponent()) && (robots[i].getType().equals(RobotType.ARCHON) 
        					|| robots[i].getType().equals(RobotType.GARDENER)) )
        			{
        				MapLocation temp = robots[i].getLocation();
        				float tempDist = temp.distanceTo(me);
        				if( tempDist < 1F )
        					continue;
        				target = temp;
        				break;
        			}
        		}
        	}
        	//try initial archon locations
        	MapLocation[] archons = rc.getInitialArchonLocations(rc.getTeam().opponent());
        	if( target == null && deadArchons != archons.length )
        	{
        		for( int x = archons.length-1; x >= 0; x-- )
        		{
        			float tempDist = archons[x].distanceTo(me);
        			if( tempDist < 1F )
    					continue;
    				target = archons[x];
    				break;
        		}
        	}
        	if( !ScoutNav.goToTarget(rc, target) )
        	{
        		if( target == null )
        		{
        			//if( )
        			Direction rand = new Direction((float)Math.random() * 2 * (float)Math.PI);
        			if( rc.canMove(rand) )
        				rc.move(rand);
        		}
        		//could not move - how to deal with this?
        	    //move in a random direction between certain degrees depending on LR
        		ScoutNav.compareLR(me, target);
        		//no target
        	}
			PublicMethods.donateBullets(rc);
            Clock.yield();
        }
    }
}