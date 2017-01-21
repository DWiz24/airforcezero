package airforcezero;
import battlecode.common.*;

public class ScoutNav {
	public static boolean goToTarget(RobotController rc, MapLocation dest) throws GameActionException 
	{
		MapLocation me = rc.getLocation();
		if( dest == null )
			return false;
		//should never happen
		if (me.equals(dest)) 
			return false;
		//direct path - best case
        Direction direct = me.directionTo(dest);
		if (rc.canMove(direct)) 
		{
			rc.move(direct);
			return true;
		}	
		//see if it's shorter to go left or right and try with priority 
		//edit more to prioritize stopping in trees?
		Direction[] dirs;
		if (compareLR(me, dest)) 
		{
			dirs = new Direction[] { direct, direct.rotateLeftDegrees(45F), direct.rotateRightDegrees(45F),
					direct.rotateLeftDegrees(90F), direct.rotateRightDegrees(90F) };			
		} 
		else 
		{
			dirs = new Direction[] { direct, direct.rotateRightDegrees(45F), direct.rotateLeftDegrees(45F),
					direct.rotateRightDegrees(90F), direct.rotateLeftDegrees(90F) };
		}
	    float currentDist = me.distanceTo(dest);
	    for (Direction dir : dirs) 
	    {
	    	MapLocation newLoc = me.add(dir);
	    	if (newLoc.distanceTo(dest) > currentDist) 
	    		continue;
	    	if (rc.canMove(dir)) 
	    	{
	    		rc.move(dir);
	    		return true;
	    	} 
	    }
	    return false;
	}
	
	private static boolean compareLR(MapLocation curr, MapLocation dest) 
	{
		Direction toDest = curr.directionTo(dest);
		MapLocation leftLoc = curr.add(toDest.rotateLeftDegrees(45F));
		MapLocation rightLoc = curr.add(toDest.rotateRightDegrees(45F));
		return (dest.distanceTo(leftLoc) < dest.distanceTo(rightLoc));
	}
}