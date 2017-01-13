package airforcezero;
import battlecode.common.*;
import java.lang.Math;

public class Archon {
   static int targetCreation = -1;
   static int gardener = 0;
   static int arCount = 0;
   static int lastCreatedRound = 0;
   static Direction[] dirs={Direction.getNorth(),Direction.getSouth(),Direction.getEast(),Direction.getWest(),
      	Direction.getNorth().rotateRightDegrees(45F),Direction.getSouth().rotateRightDegrees(45F),
      	Direction.getEast().rotateRightDegrees(45F),Direction.getWest().rotateRightDegrees(45F)};
   public static void run(RobotController rc) throws GameActionException {
      while(true)
      {
         MapLocation myLoc = rc.getLocation();
         int round = rc.getRoundNum();
         Direction build = null;
         RobotInfo[] nearRobot = rc.senseNearbyRobots();
         MapLocation[] archons =rc.getInitialArchonLocations(rc.getTeam());
         arCount = archons.length;
         if( round < 2 || (round >= 40 && gardener < 14/arCount) && targetCreation == -1 )
         {
            for( int x = 0; x < dirs.length; x++ )
            {
               build = dirs[x];
               if( rc.canHireGardener(build) )
               {
                  rc.hireGardener(build);
                  lastCreatedRound = round;
                  gardener += 1;
                  targetCreation = rc.senseRobotAtLocation(myLoc.add(build, 3.01F)).getID();
               	//System.out.println(""+targetCreation);
                  break;
               }
            }
         }
        	//try to move to target
         if( targetCreation != -1)
         {
            if( rc.canSenseRobot(targetCreation) )
            {
               float dest = myLoc.distanceTo(rc.senseRobot(targetCreation).location);
            	//target in correct range
               if( dest <= 4F)
               {
               	//System.out.println("Can see gardener");
               	//System.out.println("" + rc.senseRobot(targetCreation).location.distanceTo(rc.getLocation()));
                  Clock.yield();
               }
               else
               {
               	//System.out.println("CAN'T");
               	//change to try to move in range
                  Direction attempt = myLoc.directionTo(rc.senseRobot(targetCreation).location);
                  if( rc.canMove(attempt) )
                  {
                     rc.move(attempt);
                  }
                  else
                  {
                     Direction ran = new Direction((float)Math.random() * 2 * (float)Math.PI);
                     if( rc.canMove(ran) )
                        rc.move(ran);
                  }
               	//else if( build < )
               }
            }
            if( round - lastCreatedRound > 15 && round > 40 )
            {
               if( moveToEmptyArea(rc, nearRobot) )
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
            if( moveToEmptyArea(rc, nearRobot) )
               Clock.yield();
            Direction ran = new Direction((float)Math.random() * 2 * (float)Math.PI);
            if( rc.canMove(ran) )
               rc.move(ran);
         	//else if( build < )
         }
         Clock.yield();
      }
   }
   public static boolean moveToEmptyArea(RobotController myR, RobotInfo[] sRobots) throws GameActionException
   {
    	//determine if current is empty
      RobotInfo[] circle = myR.senseNearbyRobots(3.01F);
      if( circle.length <= 3 )
         return true;
      for( int x = 0; x < dirs.length; x++ )
      {
         Direction m = dirs[x];
         if( myR.canMove(m) )
         {
            myR.move(m);
            return true;
         }
      }
      return false;
   }
}