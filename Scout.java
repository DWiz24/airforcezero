package airforcezero;
import battlecode.common.*;

public class Scout {
	static boolean harass = false;
	static int deadArchons = 0;
	static int index = 0;
	static int lastBroadcast = 0;
	static MapLocation[] archons = null;
	static boolean noSoldier = true;
	static boolean reflectTree = false;
	static MapLocation startTree = null;
	static boolean moved = false;
    public static void run(RobotController rc) throws GameActionException 
    {
    	archons = rc.getInitialArchonLocations(rc.getTeam().opponent());
        while(true)
        {
        	if( rc.canShake() )
        		Soldier.shakeATree(rc);
        	
        	MapLocation me = rc.getLocation();
        	RobotInfo[] robots = rc.senseNearbyRobots();
        	MapLocation target = null;
        	BulletInfo[] bullets = rc.senseNearbyBullets();
        	//check to see if can harass
        	//if so target = null
        	
        	//find a tree to use to protect for micro all archons dead
      
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
       		//micro
       		RobotInfo[] friend = new RobotInfo[robots.length];
            int friends = -1;
            RobotInfo[] enemy = new RobotInfo[robots.length];
            int enemies = -1;
       		for( int i = 0; i < robots.length; i++ )
    		{
       			if( robots[i].getTeam().equals(rc.getTeam().opponent()) )
                    enemy[++enemies] = robots[i];
       			else
       				friend[++friends] = robots[i];
    		}
       		if( enemies > 0 && bullets.length != 0 )
       			target = newMicro(rc, trees, friend, friends, enemy, enemies, bullets);
        	//find archon or gardener in range
        	boolean foundTarget = false;
        	int solCount = 0;
        	if( target == null )
        	{
        		for( int i = 0; i < robots.length; i++ )
        		{
        			if( robots[i].getTeam().equals(rc.getTeam().opponent()) && robots[i].getType().equals(RobotType.GARDENER) )
        			{
        				//System.out.println("Trying gardener and archon");
        				MapLocation temp = robots[i].getLocation();
        				float tempDist = temp.distanceTo(me);
        				//System.out.println("" + tempDist);
        				if( tempDist < (robots[i].getRadius()+1.5F) && noSoldier && rc.canFireSingleShot() )
        				{
        					//System.out.println("Shooting");
        					rc.fireSingleShot(me.directionTo(temp));
        					continue;
        				}
        				if( !foundTarget )
        				{
        					target = temp;
        					foundTarget = true;
        				}
        				
        			}
        			else if( robots[i].getTeam().equals(rc.getTeam()) && robots[i].getType().equals(RobotType.SOLDIER) && robots[i].getLocation().distanceTo(me) < 7F)
        				solCount++;
        		}
        		if( solCount == 0 )
            		noSoldier = true;
            	else
            		noSoldier = false;
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
        	//Systemm.out.println("" + target.x + "y: " + target.y);
        	if( !ScoutNav.goToTarget(rc, target) )
        	{
        			Direction rand = new Direction((float)Math.random() * 2 * (float)Math.PI);
        			if( rc.canMove(rand) )
        				rc.move(rand);
        	}
			PublicMethods.donateBullets(rc);
            Clock.yield();
        }
    }

    static MapLocation newMicro(RobotController rc, TreeInfo[] trees, RobotInfo[] friend, int friends, RobotInfo[] enemy, int enemies, BulletInfo[] allBullets) throws GameActionException {
        //int prebyte=Clock.getBytecodeNum();
        int nbullets = 0;
        float[] dists = new float[allBullets.length];
        MapLocation loc = rc.getLocation();
        BulletInfo[] bullets = new BulletInfo[allBullets.length];
        for (int i = allBullets.length - 1; i >= 0; i--) {
            BulletInfo b = allBullets[i];
            MapLocation oloc = b.location;
            float dist = loc.distanceTo(oloc);
            float radsBetween = Math.abs(oloc.directionTo(loc).radiansBetween(b.dir));
            if (dist <= 1.8 || Math.asin(1.8 / dist) >= radsBetween) {
                float minDist = b.speed * 2;
                for (int k = trees.length - 1; k >= 0; k--) {
                    MapLocation tree = trees[k].location;
                    float theta = Math.abs(b.location.directionTo(tree).radiansBetween(b.dir));
                    float distT = tree.distanceTo(b.location);
                    float r = trees[k].radius;
                    if (Math.asin(r / distT) > theta) {
                        double sintheta = Math.sin(theta);
                        float y = (float) Math.asin(distT * sintheta / r) - theta;
                        float impact = (float) (r * Math.sin(y) / sintheta);
                        minDist = Math.min(minDist, impact);
                    }
                }
                for (int k = friends; k >= 0; k--) {
                    if (friend[k].type == RobotType.TANK || friend[k].type == RobotType.ARCHON) {
                        MapLocation tree = friend[k].location;
                        float theta = Math.abs(b.location.directionTo(tree).radiansBetween(b.dir));
                        float distT = tree.distanceTo(b.location);
                        float r = friend[k].getRadius();
                        if (Math.asin(r / distT) > theta) {
                            double sintheta = Math.sin(theta);
                            float y = (float) Math.asin(distT * sintheta / r) - theta;
                            float impact = (float) (r * Math.sin(y) / sintheta);
                            minDist = Math.min(minDist, impact);
                        }
                    }
                }
                double sintheta = Math.sin(radsBetween);
                //boolean a=;
                //double distB=; //1.570796+
                //System.out.println(a+" "+ distB);
                if (dist < 1.8 || 1.8 * Math.sin(Math.asin(dist * sintheta / 1.8) - radsBetween) / sintheta < minDist) {
                    if (!(0.2 * Math.sin(Math.asin(dist * sintheta / 0.2) - radsBetween) / sintheta < b.speed)) {
                        dists[nbullets] = minDist;
                        //rc.setIndicatorLine(b.location, b.location.add(b.dir, minDist), 100, 100, 100);
                        //rc.setIndicatorDot(b.location, 100, 100, 100);
                        bullets[nbullets++] = b;
                    }
                }
            }
        }
        nbullets--;
        //if (nbullets!=-1) underFire=rc.getRoundNum();
        MapLocation[] jack = new MapLocation[enemies + 1];
        int jacks = -1;
        //int enemySoldiersNTanks = 0;
        for (int i = enemies; i >= 0; i--) {
            if (enemy[i].location.distanceTo(loc) < 4.75) jack[++jacks] = enemy[i].location;
        }
        //int newByte=Clock.getBytecodeNum();
        //System.out.println("Precomputation: "+(newByte-prebyte));
        int minDamage = 0;
        float minDist = 99999;
        for (int k = nbullets; k >= 0; k--) {
            BulletInfo b = bullets[k];
            float dist = loc.distanceTo(b.location);
            float radsBetween = Math.abs(b.location.directionTo(loc).radiansBetween(b.dir));
            if (dist <= 1 || Math.asin(1 / dist) > radsBetween) {
                double sintheta = Math.sin(radsBetween);
                if (dist<=1 || Math.sin(Math.asin(dist * sintheta) - radsBetween) / sintheta < Math.min(dists[k], b.speed) || 0.2 * Math.sin(Math.asin(dist * sintheta / 0.2) - radsBetween) / sintheta < dists[k])
                    minDamage += b.damage;
            }
        }
        for (int k = jacks; k >= 0; k--) {
            if (loc.distanceTo(jack[k]) <= 3.75) minDamage += 2;
        }
        //System.out.println(minDamage);
        //boolean attackOrRun = (enemySoldiersNTanks >= friends + 2) || (enemySoldiersNTanks > 0 && friends <= 1 && rc.getHealth() < 20);
        // true means run
        //if (attackOrRun) rc.setIndicatorDot(rc.getLocation(), 0, 0, 0);
        for (int i = enemies; i >= 0; i--) {
            switch (enemy[i].type) {
                case SOLDIER:
                case TANK:
                    minDist = Math.max(loc.distanceTo(enemy[i].location), minDist);
                    break;
                case SCOUT:
                case GARDENER:
                case LUMBERJACK:
                    minDist = Math.max(minDist, -loc.distanceTo(enemy[i].location));
                    break;
                case ARCHON:
                    minDist = Math.max(minDist, -99 * loc.distanceTo(enemy[i].location));
            }
        }
        //if (enemies==-1) minDist=-(pastTarget!=null && rc.getRoundNum()-pastTargetSet<20?loc.distanceTo(pastTarget.location):loc.distanceTo(Nav.dest));
        MapLocation best = rc.getLocation();
        Direction dir = Direction.getEast();

        for (int i = 8; i > 0; i--) {
            //MapLocation move = rc.getLocation().add(dir, 1.9f);
            //if (!rc.isCircleOccupied(move,1))
            MapLocation move =loc.add(dir, .8f);
            if (rc.canMove(move)) {
                int damage = 0;
                for (int k = nbullets; k >= 0; k--) {
                    BulletInfo b = bullets[k];
                    float dist = b.location.distanceTo(move);
                    float radsBetween = Math.abs(b.location.directionTo(move).radiansBetween(b.dir));
                    if (dist <= 1 || Math.asin(1 / dist) > radsBetween) {
                        double sintheta = Math.sin(radsBetween);
                        if (dist<=1 || Math.sin(Math.asin(dist * sintheta) - radsBetween) / sintheta < Math.min(dists[k], b.speed) || 0.2 * Math.sin(Math.asin(dist * sintheta / 0.2) - radsBetween) / sintheta < dists[k])
                            damage += b.damage;
                            rc.setIndicatorDot(b.location,255,255,255);
                    }
                }

                for (int k = jacks; k >= 0; k--) {
                    if (move.distanceTo(jack[k]) <= 3.75) damage += 2;
                }
                if (damage <= minDamage) {
                    float theDist = -9999999;

                    for (int x = enemies; x >= 0; x--) {
                        switch (enemy[x].type) {
                            case SOLDIER:
                            case TANK:
                                theDist = Math.max(move.distanceTo(enemy[x].location), theDist);
                                break;
                            case SCOUT:
                            case GARDENER:
                            case LUMBERJACK:
                                theDist = Math.max(theDist, -move.distanceTo(enemy[x].location));
                                break;
                            case ARCHON:
                                theDist = Math.max(theDist, -99 * move.distanceTo(enemy[x].location));
                        }
                    }

                    /*if (enemies == -1) theDist = -(pastTarget!=null && rc.getRoundNum()-pastTargetSet<20?move.distanceTo(pastTarget.location):move.distanceTo(Nav.dest));
                    if (damage < minDamage || damage == minDamage && theDist > minDist) {
                        minDamage = damage;
                        best = move;
                        minDist = theDist;
                    }*/

                }
            }
            dir = dir.rotateLeftDegrees(45);
        }
        //System.out.println("Other: "+(Clock.getBytecodeNum()-newByte));
        return best;
    }
}