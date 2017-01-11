package airforcezero;
import battlecode.common.*;
import java.lang.Math;

public class Archon {
    public static void run(RobotController rc) throws GameActionException {
        if (rc.canHireGardener(Direction.getEast()))
        rc.hireGardener(Direction.getEast());
        else rc.hireGardener(Direction.getWest());
        while(true){
        	int round = rc.getRoundNum();
        	System.out.println("This is round" + round);
        	int addLoc = 0;
        	Direction build = null;
        	RobotInfo[] nearRobot = rc.senseNearbyRobots();
        	MapLocation[] archons =rc.getInitialArchonLocations(rc.getTeam());
        	//Direction[] direc = {new Direction(0), new Direction((float)Math.PI/2), new Direction((float)Math.PI), new Direction((float)Math.PI*1.5F)};
        	if( round < 2 || (round > 70 && round < 500) )
        	{
        		while(true)
        		{
        			build = new Direction((float)Math.random() * 2 * (float)Math.PI);
        			if( rc.canHireGardener(build) )
        			{
        				rc.hireGardener(build);
        				break;
        			}
        		}
        	}
        	Direction ran = new Direction((float)Math.random() * 2 * (float)Math.PI);
        	if( rc.canMove(ran) )
        	{
        		rc.move(ran);
        	}
        	//else if( build < )
            Clock.yield();
        }
    }
}