package airforcezero;
import battlecode.common.*;

public class Scout {
	static boolean harass = false;
	static int deadArchons = 0;
	static boolean overlapTree = false;
    public static void run(RobotController rc) throws GameActionException 
    {
        while(true)
        {
        	Soldier.shakeATree(rc);
        	
        	MapLocation me = rc.getLocation();
        	RobotInfo[] robots = rc.senseNearbyRobots();
        	float minTargetDist = 999F;
        	MapLocation target = null;
        	//check to see if can harass
        	//if so target = null
        	
        	//find archon or gardener in range
        	for( int i = robots.length-1; i >= 0; i-- )
        	{
        		if( robots[i].getTeam().equals(rc.getTeam().opponent()) && (robots[i].getType().equals(RobotType.ARCHON) 
        				|| robots[i].getType().equals(RobotType.GARDENER)) )
        		{
        			MapLocation temp = robots[i].getLocation();
        			float tempDist = temp.distanceTo(me);
        			if( minTargetDist > tempDist )
        			{
        				minTargetDist = tempDist;
        				target = temp;
        				overlapTree = false;
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
        			if( minTargetDist > tempDist )
        			{
        				minTargetDist = tempDist;
        				target = archons[x];
        				overlapTree = false;
        			}
        		}
        	}
        	//find a tree to use to protect for micro all archons dead
        	if( target == null )
        	{
        		TreeInfo[] trees = rc.senseNearbyTrees();
        		for( int i = trees.length-1; i >= 0; i-- )
            	{
            		MapLocation temp = trees[i].getLocation();
            		float tempDist = temp.distanceTo(me);
            		if( tempDist < 1F )
            		{
            			overlapTree = true;
            			break;
            		}
            		if( minTargetDist > tempDist )
            		{
            			minTargetDist = tempDist;
            			target = temp;
            		}
            	}
        	}
        	if( !ScoutNav.goToTarget(rc, target) )
        	{
        		//could not move - how to deal with this?
        	    //move in a random direction between certain degrees depending on LR
        		ScoutNav.compareLR(me, target);
        		//no target
        		if( target == null )
        		{
        			//if( )
        			Direction rand = new Direction((float)Math.random() * 2 * (float)Math.PI);
        			if( rc.canMove(rand) )
        				rc.move(rand);
        		}
        	}
			PublicMethods.donateBullets(rc);
            Clock.yield();
        }
    }
}