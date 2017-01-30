package airforcezero;
import battlecode.common.*;

public class Scout {
	static boolean harass = false;
	static int deadArchons = 0;
	static int index = 0;
	static int lastBroadcast = 0;
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
        		boolean robotTree = false;
        		MapLocation robotTreeLoc = null;
			    TreeInfo robotTreeInfo = null;
        		float robotRadius = 0;
        		for( int i = 0; i < trees.length; i++ )
            	{
        			if( trees[i].containedBullets > 0 || trees[i].containedRobot != null )
        			{
        				MapLocation temp = trees[i].getLocation();
        				float tempDist = temp.distanceTo(me);
        				if( tempDist <= trees[i].radius )
        					continue;
        				if( tempDist <= trees[i].radius + 2F )
        				{
        					if( trees[i].containedRobot != null )
        					{
        						robotTree = true;
        						robotTreeInfo = trees[i];
        						robotTreeLoc = temp;
        						robotRadius = trees[i].radius;
        					}
        				}
        				if( trees[i].containedBullets > 0 )
        					target = temp;
        				break;
        			}
            	}
        		if( ((lastBroadcast == 0 || rc.getRoundNum()-lastBroadcast > 15) && trees.length>11) || robotTree )
        		{
        			rc.broadcast(200, rc.readBroadcast(200)+1);
        			lastBroadcast = rc.getRoundNum();
        			if( robotTree )
        			{
        				//System.out.println("Found robot");
						Lumberjack.areLocationsNear(rc, robotTreeLoc);
						if(!Lumberjack.locationsNear)
        					Lumberjack.lumberjackNeeded(rc, robotTreeLoc, Lumberjack.staticPriorityOfTree(rc, robotTreeInfo), Lumberjack.numberNeeded(rc, robotTreeInfo), robotRadius);
        			}
        		}
        	}
        	//find archon or gardener in range
        	if( target == null )
        	{
        		for( int i = 0; i < robots.length; i++ )
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
        		for( int x = 0; x < archons.length; x++ )
        		{
        			float tempDist = archons[x].distanceTo(me);
        			if( tempDist < 1F )
    					continue;
    				target = archons[x];
    				break;
        		}
        	}
        	//System.out.println("" + target.x + "y: " + target.y);
        	if( !ScoutNav.goToTarget(rc, target) )
        	{
        		//if( target == null )
        		//{
        			//if( )
        			Direction rand = new Direction((float)Math.random() * 2 * (float)Math.PI);
        			if( rc.canMove(rand) )
        				rc.move(rand);
        		//}
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