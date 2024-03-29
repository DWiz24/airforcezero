package airforcezero;
import battlecode.common.*;

import java.util.Random;
import java.lang.Math;

public class Lumberjack {
    //global stuff

    static final boolean DEBUG1 = false, DEBUG2 = false;  //set to false to make them shut up

    //general
    private static RobotController rc;
    private static Random rng;

    //variables related to my robot's behavior
    private static MapLocation[] excludeLocations,  excludeTrees;
    private static int excludeLocationsSize, excludeTreesSize;
    static boolean traveling, exploring, locationsNear, printedThisTurn, agressive;
    private static int travelingChannel;
    static MapLocation prevGardenerPos;
    private static float prevHealth, health;
    static float limit;

    //channels
    private static int next = -1; //previous and current turn next tree channel to write to

    //info about my surroundings, updates every turn
    private static BulletInfo[] bullets;
    private static RobotInfo[] allRobots, friendlyRobots, enemyRobots;
    static RobotInfo[] friendlyGardeners;
    private static TreeInfo[] allTrees, friendlyTrees, neutralTrees, enemyTrees;
    private static float[] allRobotDists, friendlyRobotDists, enemyRobotDists, allTreeDists, friendlyTreeDists, neutralTreeDists, enemyTreeDists;
    private static int friendlyRobotCount, enemyRobotCount, friendlyTreeCount, neutralTreeCount, enemyTreeCount;
    static int friendlyGardenerCount;
    private static float bestPriority, bestPriorityStatic2;
    private static int bestPriorityStatic;
    private static TreeInfo bestTree, bestTreeStatic;

    //random stuff
    private static boolean dead;

    /*
    -If nothing better to do, lumberjacks wander off to search for trees
    -Make own decisions
    -Report locations where support is needed to array
        
    -TODO:
    -(Important) running away
    -bullet dodging
    -calling combat
    */
    public static void run(RobotController rcArg) throws GameActionException{
        //general
        rc = rcArg;
        rng = new Random(rc.getID());

        //variables related to my robot's behavior
        excludeLocations = new MapLocation[25];
        excludeTrees = new MapLocation[25];
        excludeLocationsSize = 0;
        excludeTreesSize = 0;
        printedThisTurn = false;
        if(DEBUG1){
            System.out.print("\nI am a lumberjack!");
            printedThisTurn = true;
        }
        traveling = false;
        travelingChannel = -1;
        locationsNear = false;
        exploring = false;
        prevGardenerPos = rc.getInitialArchonLocations(rc.getTeam())[0];    //in case gardener runs away
        limit = 0f;
        prevHealth = health = rc.getHealth();

        //channels
        next = rc.readBroadcast(15);
        if (next == 0) { //in case this is the first lumberjack, set the index to default
            rc.broadcast(15, 16);
            next = 16;
        }

        dead = false;
        //code below repeats every turn
        while (true) {
            //updating info about robots and trees around me
            updateInfo();
            //health and census
            prevHealth = health;
            health = rc.getHealth();
            if(!dead && PublicMethods.isAboutToDie(rc, prevHealth)) {
                dead = true;
                rc.broadcast(2, rc.readBroadcast(2) - 1); //decrement census
            }
            int lumbers = rc.readBroadcast(2);
            int soldiers = rc.readBroadcast(3);
            agressive = false;
            if(lumbers > 30 || (lumbers > 15 && lumbers > soldiers))
                agressive = true;
            //others
            next = rc.readBroadcast(15);
            //System.out.print(next);
            if (friendlyGardenerCount > 0) {
                prevGardenerPos = friendlyGardeners[0].location;
            }
            if(DEBUG2) {
                rc.setIndicatorDot(prevGardenerPos, 0, 0, 0);
            }

            //check for better locations
            if(!traveling){
                chooseBestLocation(excludeLocations);
            }
            else {
                //make a call to get whether locations are around, since chooseBestLocation was never called
                if(bestTreeStatic != null)
                    areLocationsNear(rc, bestTreeStatic.location);
            }
            if(!locationsNear && bestTreeStatic != null && (agressive || bestPriorityStatic * 66.66666667f + dynamicPriorityFromBase(bestTreeStatic) > shrinkingPriority(rc))) {
                //if not locations in range and found locations worth reporting
                lumberjackNeeded(rc, bestTreeStatic.location, bestPriorityStatic, numberNeeded(rc, bestTreeStatic), bestTreeStatic.radius);
            }

            //striking
            boolean hasStruck = false;
            if(bodiesWithinDistance(enemyRobots, enemyRobotDists, 2, true) > bodiesWithinDistance(friendlyRobots, friendlyRobotDists, 2, false)) {
                rc.strike();
                hasStruck = true;
            }

            //move, shake, and chop
            MapLocation move = rc.getLocation();
            if(enemyRobotCount > 0 || bullets.length != 0)
                move = newMicro(rc, allTrees, friendlyRobots, friendlyRobotCount-1, enemyRobots, enemyRobotCount-1, bullets);
            if (move.equals(rc.getLocation()))
                move = Nav.lumberjackNav(rc, allTrees, allRobots, allTreeDists, allRobotDists, hasStruck);
            if(move != null)
                rc.move(move);
            shakeATree();
            
            if(printedThisTurn){
               System.out.println();
               printedThisTurn = false;
            }

            PublicMethods.donateBullets(rc);
            
            //end of while loop - yield to end
            Clock.yield();
        }
    }

    //helper methods
    private static void updateInfo(){
        bullets = rc.senseNearbyBullets();

        allRobots = rc.senseNearbyRobots();
        friendlyRobots = new RobotInfo[allRobots.length];
        enemyRobots = new RobotInfo[allRobots.length];
        friendlyGardeners = new RobotInfo[allRobots.length];

        allRobotDists = new float[allRobots.length];
        friendlyRobotDists = new float[allRobots.length];
        enemyRobotDists = new float[allRobots.length];

        allTrees = rc.senseNearbyTrees();
        friendlyTrees = new TreeInfo[allTrees.length];
        neutralTrees = new TreeInfo[allTrees.length];
        enemyTrees = new TreeInfo[allTrees.length];

        allTreeDists = new float[allTrees.length];
        friendlyTreeDists = new float[allTrees.length];
        neutralTreeDists = new float[allTrees.length];
        enemyTreeDists = new float[allTrees.length];

        friendlyRobotCount = 0;
        enemyRobotCount = 0;
        friendlyTreeCount = 0;
        neutralTreeCount = 0;
        enemyTreeCount = 0;
        friendlyGardenerCount = 0;

        bestPriority = -1f;
        bestPriorityStatic = -1;
        bestTree = null;
        bestTreeStatic = null;
        if(rc.getTeam() == Team.A) {
            int robotCount = 0;
            for(RobotInfo info : allRobots) {
                float dist = rc.getLocation().distanceTo(info.location);
                allRobotDists[robotCount] = dist;
                switch(info.team) {
                    case A:
                        friendlyRobots[friendlyRobotCount] = info;
                        friendlyRobotDists[friendlyRobotCount] = dist;
                        friendlyRobotCount++;
                        if(info.type == RobotType.GARDENER){
                            friendlyGardeners[friendlyGardenerCount] = info;
                            friendlyGardenerCount++;
                        }
                        break;
                    case B:
                        enemyRobots[enemyRobotCount] = info;
                        enemyRobotDists[enemyRobotCount] = dist;
                        enemyRobotCount++;
                }
                robotCount++;
            }
            int treeCount = 0;
            treeLoop:
            for(TreeInfo info : allTrees){
                float dist = rc.getLocation().distanceTo(info.location);
                allTreeDists[treeCount] = dist;
                switch(info.team) {
                    case A:
                        friendlyTrees[friendlyTreeCount] = info;
                        friendlyTreeDists[friendlyTreeCount] = dist;
                        friendlyTreeCount++;
                        break;
                    case B:
                        enemyTrees[enemyTreeCount] = info;
                        enemyTreeDists[enemyTreeCount] = dist;
                        enemyTreeCount++;
                        break;
                    case NEUTRAL:
                        neutralTrees[neutralTreeCount] = info;
                        neutralTreeDists[neutralTreeCount] = dist;
                        neutralTreeCount++;
                }
                treeCount++;
                //check for exclusion
                for(int i = 0; i < excludeTreesSize; i++) {
                    if(excludeTrees[i].equals(info.location))
                        continue treeLoop;
                }
                int staticPriority = staticPriorityOfTree(rc, info);
                float staticPriority2 = staticPriority * 66.66666667f + dynamicPriorityFromBase(info);
                float priority;
                if(agressive)
                    priority = staticPriority * 66.66666667f + dynamicPriorityFromMe(info);
                else
                    priority = staticPriority * 66.66666667f + dynamicPriorityOfTree(info);
                if(rc.getRoundNum() < 500 && staticPriority == 0)  //clearing base
                    priority += 1000;
                if(staticPriority > -1 && priority > bestPriority){
                    bestPriority = priority;
                    bestTree = info;
                }
                if(staticPriority > -1 && staticPriority2 > bestPriorityStatic2){
                    bestPriorityStatic = staticPriority;
                    bestPriorityStatic2 = staticPriority2;
                    bestTreeStatic = info;
                }
            }
        }
        else{
            int robotCount = 0;
            for(RobotInfo info : allRobots) {
                float dist = rc.getLocation().distanceTo(info.location);
                allRobotDists[robotCount] = dist;
                switch(info.team) {
                    case B:
                        friendlyRobots[friendlyRobotCount] = info;
                        friendlyRobotDists[friendlyRobotCount] = dist;
                        friendlyRobotCount++;
                        if(info.type == RobotType.GARDENER){
                            friendlyGardeners[friendlyGardenerCount] = info;
                            friendlyGardenerCount++;
                        }
                        break;
                    case A:
                        enemyRobots[enemyRobotCount] = info;
                        enemyRobotDists[enemyRobotCount] = dist;
                        enemyRobotCount++;
                }
                robotCount++;
            }
            int treeCount = 0;
            treeLoop:
            for(TreeInfo info : allTrees){
                float dist = rc.getLocation().distanceTo(info.location);
                allTreeDists[treeCount] = dist;
                switch(info.team) {
                    case B:
                        friendlyTrees[friendlyTreeCount] = info;
                        friendlyTreeDists[friendlyTreeCount] = dist;
                        friendlyTreeCount++;
                        break;
                    case A:
                        enemyTrees[enemyTreeCount] = info;
                        enemyTreeDists[enemyTreeCount] = dist;
                        enemyTreeCount++;
                        break;
                    case NEUTRAL:
                        neutralTrees[neutralTreeCount] = info;
                        neutralTreeDists[neutralTreeCount] = dist;
                        neutralTreeCount++;
                }
                treeCount++;
                //check for exclusion
                for(int i = 0; i < excludeTreesSize; i++) {
                    if(excludeTrees[i].equals(info.location))
                        continue treeLoop;
                }
                int staticPriority = staticPriorityOfTree(rc, info);
                float staticPriority2 = staticPriority * 66.66666667f + dynamicPriorityFromBase(info);
                float priority;
                if(agressive)
                    priority = staticPriority * 66.66666667f + dynamicPriorityFromMe(info);
                else
                    priority = staticPriority * 66.66666667f + dynamicPriorityOfTree(info);
                if(rc.getRoundNum() < 500 && staticPriority == 0)  //clearing base
                    priority += 1000;
                if(staticPriority > -1 && priority > bestPriority){
                    bestPriority = priority;
                    bestTree = info;
                }
                if(staticPriority > -1 && staticPriority2 > bestPriorityStatic2){
                    bestPriorityStatic = staticPriority;
                    bestPriorityStatic2 = staticPriority2;
                    bestTreeStatic = info;
                }
            }
        }
    }
    private static int bodiesWithinDistance(BodyInfo[] bodies, float[] dists, float dmax, boolean countScouts){
        //gives the amount of bodies from given sorted array (represented by dists) that are closer than distance
        int count = 0;
        for(int i = 0; i < dists.length; i++){
            //0.0 is default for float arrays
            if(dists[i] == 0.0f)
                break;
            else if(dists[i] - bodies[i].getRadius() <= dmax){
                count++;
                if(countScouts && bodies[i].isRobot() && ((RobotInfo)bodies[i]).type == RobotType.SCOUT)
                    count++;
            } 
            else if(dists[i] - 2 > dmax)
                break;
        }
        return count;
    }
    static float shrinkingPriority(RobotController rc){
        //950 - 900
        //~7.07 to ~14.14
        return 950f - (rc.getRoundNum())/60f;
    }
    static void areLocationsNear(RobotController rc, MapLocation treeLocation) throws GameActionException{
        locationsNear = false;

        for(int i = 16; i < 30; i++){
            int value = rc.readBroadcast(i);
            MapLocation valueLoc = intToLocation(value);
            if(value == 0)
                continue;

            if(DEBUG2){
                if(rc.getTeam() == Team.A)
                    rc.setIndicatorDot(valueLoc, 200, 255, 0);
                else
                    rc.setIndicatorDot(valueLoc, 0, 255, 200);
            }

            if(!locationsNear && valueLoc.distanceTo(treeLocation) <= 7f) {
                locationsNear = true;
                if(!DEBUG2)
                    return;
            }
        }
    }
    private static void setLimit(float l){
        if(l == 0f)
            limit = 0f;
        else
            limit = l + .7f;
    }

    //movement stuff
    private static MapLocation newMicro(RobotController rc, TreeInfo[] trees, RobotInfo[] friend, int friends, RobotInfo[] enemy, int enemies, BulletInfo[] allBullets) throws GameActionException {
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
            if (dist <= 1.75 || Math.asin(1.75 / dist) >= radsBetween) {
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
                if (dist < 1.75 || 1.75 * Math.sin(Math.asin(dist * sintheta / 1.75) - radsBetween) / sintheta < minDist) {
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
            if(agressive)
                minDist = Math.max(minDist, -loc.distanceTo(enemy[i].location));
            else
                minDist = Math.max(minDist, loc.distanceTo(enemy[i].location));
        }
        MapLocation best = rc.getLocation();
        Direction dir = Direction.getEast();

        for (int i = 8; i > 0; i--) {
            //MapLocation move = rc.getLocation().add(dir, 1.9f);
            //if (!rc.isCircleOccupied(move,1))
            MapLocation move =loc.add(dir, 0.75f);
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
                            //rc.setIndicatorDot(b.location,255,255,255);
                    }
                }

                for (int k = jacks; k >= 0; k--) {
                    if (move.distanceTo(jack[k]) <= 3.75) damage += 2;
                }
                if (damage <= minDamage) {
                    float theDist = -9999999;

                    for (int x = enemies; x >= 0; x--) {
                        if(agressive)
                            theDist = Math.max(theDist, -move.distanceTo(enemy[x].location));
                        else
                            theDist = Math.max(theDist, move.distanceTo(enemy[x].location));
                    }
                    if (damage < minDamage || damage == minDamage && theDist > minDist) {
                        minDamage = damage;
                        best = move;
                        minDist = theDist;
                    }

                }
            }
            dir = dir.rotateLeftDegrees(45);
        }
        //System.out.println("Other: "+(Clock.getBytecodeNum()-newByte));
        return best;
    }
    private static MapLocation pickNearTreeLoc(TreeInfo tree) throws GameActionException{
        //Does exactly what it says.
        //Takes over approaching trees when close (dist <= 1 + maxTreeRadius).
        //TODO: make gardeners leave room for others.
        MapLocation nearTreeLoc = tree.location.add(tree.location.directionTo(rc.getLocation()), tree.radius + RobotType.LUMBERJACK.bodyRadius + 0.001f);
        Nav.setDest(nearTreeLoc);
        exploring = false;
        limit = 0f;
        return nearTreeLoc;
    }

    //tree handling stuff
    static void lumberjackNeeded(RobotController rc, MapLocation location, int priority, int number, float radius) throws GameActionException{
        //PLEASE USE
        //priority can be in range [0, 15] (use something greater than 10)
        //number can be in range [0, 7], and this number of lumberjacks will arrive
        //ALSO, if requesting location is the center of a tree, specify its radius, otherwise pass 0

        if(DEBUG1){
            System.out.print("\nRequested " + number + " lumberjack(s) at " + location.x + ", " + location.y + " with priority " + priority + ".");
            if(DEBUG2)
                System.out.print("\nBroadcasted to channel " + next);
            printedThisTurn = true;
            if(rc.getTeam() == Team.A)
                rc.setIndicatorDot(location, 255, 255, 0);
            else
                rc.setIndicatorDot(location, 0, 255, 255);
        }

        if(rc.getType() != RobotType.LUMBERJACK) {
            next = rc.readBroadcast(15);
            if(next == 0){
                next = 16;
                rc.broadcast(15, 16);
            }
        }

        rc.broadcast(next, radiusToInt(radius) | priorityToInt(priority) | neededToInt(number) | locationToInt(location));
        next++;

        if(next == 30)
            next = 16;

        rc.broadcast(15, next);
    }
    private static void chooseBestLocation(MapLocation[] exclude) throws GameActionException{
        float bestP = bestPriority; //modify to make prioritize current loc less
        int bestValue = -1;
        int bestChannel = -1;
        MapLocation bestLocation = null;

        locationsNear = false;

        locationLoop:
        for(int i = 16; i < 30; i++){
            int value = rc.readBroadcast(i);
            MapLocation valueLoc = intToLocation(value);
            if(value == 0)
                continue;

            if(DEBUG2){
                if(rc.getTeam() == Team.A)
                    rc.setIndicatorDot(valueLoc, 200, 255, 0);
                else
                    rc.setIndicatorDot(valueLoc, 0, 255, 200);
            }

            if(!locationsNear && bestTreeStatic != null && valueLoc.distanceTo(bestTreeStatic.location) <= 7f)
                locationsNear = true;

            //check for exclusion
            for(int i2 = 0; i2 < excludeLocationsSize; i2++) {
                if(exclude[i2].equals(valueLoc))
                    continue locationLoop;
            }

            //check for 0 needed
            if(intToNeeded(value) == 0)
                continue;

            float priority;
            if(agressive)
                priority = intToPriority(value) * 66.66666667f + dynamicPriorityFromMe(valueLoc);
            else
                priority = intToPriority(value) * 66.66666667f + dynamicPriorityOfTree(valueLoc);

            if(priority > bestP){
                bestP = priority;
                bestLocation = valueLoc;
                bestValue = value;
                bestChannel = i;
            }
        }

        if(bestLocation != null){
            if(!agressive)
                rc.broadcast(bestChannel, isolateRadius(bestValue) | isolatePriority(bestValue) | neededToInt(intToNeeded(bestValue)-1) | isolateLocation(bestValue));
            traveling = true;
            travelingChannel = bestChannel;
            Nav.setDest(bestLocation);
            setLimit(intToRadius(bestValue));
            exploring = false;
            if(DEBUG2){
                System.out.print("\nPriority " + bestP + " exceeds current priority " + bestPriority + ".");
                System.out.print("\n" + (intToNeeded(bestValue)-1) + " remaining.");
                printedThisTurn = true;
            }
            if(DEBUG1){
                System.out.print("\nTraveling to " + bestLocation.x + ", " + bestLocation.y + ".");
                printedThisTurn = true;
            }
        }
        else{
            if(DEBUG2){
                System.out.print("\nPriority " + bestP + " too large to travel.");
                printedThisTurn = true;
            }
        }
    }
    static int staticPriorityOfTree(RobotController rc, TreeInfo tree){
        //-1 for our trees (later ignored)
        //0 for neutral
        //5 for enemy
        //5 - 15 for robots

        if(rc.getTeam() == Team.A){
            switch(tree.team){
                case A:
                    return -1;
                case B:
                    return 5;
                case NEUTRAL:
                    if(tree.containedRobot == null)
                        return 0;
                    switch(tree.containedRobot){
                        case ARCHON:
                            return 15;
                        case TANK:
                            return 11;
                        case SOLDIER:
                            return 14;
                        case SCOUT:
                            return 11;
                        case LUMBERJACK:
                            return 12;
                        case GARDENER:
                            return 13;
                    }
            }
        }
        else{
            switch(tree.team){
                case B:
                    return -1;
                case A:
                    return 5;
                case NEUTRAL:
                    if(tree.containedRobot == null)
                        return 0;
                    switch(tree.containedRobot){
                        case ARCHON:
                            return 15;
                        case TANK:
                            return 5;
                        case SOLDIER:
                            return 14;
                        case SCOUT:
                            return 11;
                        case LUMBERJACK:
                            return 12;
                        case GARDENER:
                            return 13;
                    }
            }
        }
        return -1;  //never reached
    }
    static int dynamicPriorityOfTree(TreeInfo treeInfo){
        //expanding circle goes from ~7 to ~28 distance
        int priority = 0;

        priority += 0.6f * dynamicPriorityFromBase(treeInfo);
        priority += 0.4f * dynamicPriorityFromMe(treeInfo);

        return priority;
    }
    static int dynamicPriorityOfTree(MapLocation treeLoc){
        //expanding circle goes from ~7 to ~28 distance
        int priority = 0;

        priority += 0.6f * dynamicPriorityFromBase(treeLoc);
        priority += 0.4f * dynamicPriorityFromMe(treeLoc);

        return priority;
    }
    static float dynamicPriorityFromBase(TreeInfo treeInfo){
        return 7.071067812f * (141.4213562f - (prevGardenerPos.distanceTo(treeInfo.location) - treeInfo.radius));
    }
    private static float dynamicPriorityFromMe(TreeInfo treeInfo){
        return 7.071067812f * (141.4213562f - rc.getLocation().distanceTo(treeInfo.location) + treeInfo.radius);
    }
    static float dynamicPriorityFromBase(MapLocation treeLoc){
        return 7.071067812f * (141.4213562f - (prevGardenerPos.distanceTo(treeLoc) - 0.5f));
    }
    private static float dynamicPriorityFromMe(MapLocation treeLoc){
        return 7.071067812f * (141.4213562f - rc.getLocation().distanceTo(treeLoc) + 0.5f);
    }
    static int numberNeeded(RobotController rc, TreeInfo tree){
        //determines how many lumberjacks needed on tree
        //for trees with robots: 1 at size 1.0, 7 at size 10.0
        if(rc.getTeam() == Team.A){
            switch(tree.team){
                case A:
                    return 0;
                case B:
                    return 2;
                case NEUTRAL:
                    if(tree.containedRobot == null)
                        return 1;
                    else
                        return round((tree.radius-1f)/1.5f) + 1;
            }
        }
        else{
            switch(tree.team){
                case B:
                    return 0;
                case A:
                    return 2;
                case NEUTRAL:
                    if(tree.containedRobot == null)
                        return 1;
                    else
                        return round((tree.radius-1f)/1.5f) + 1;
            }
        }
        return -1;
    }
    //conversions
    //radius is 3-bit
    //priority is integer [0, 15], 4-bit
    //needed is integer [0, 7], 3-bit
    //locations are float [0, 600] turned 10-bit
    private static int isolateRadius(int i){
        return i & 0b111000000000000000000000000000;
    }
    private static int isolatePriority(int i){
        return i & 0b111100000000000000000000000;
    }
    private static int isolateLocation(int i){
        return i & 0b11111111111111111111;
    }
    private static int radiusToInt(float r){
        int value = -1;
        if(r >= 10f)
            value = 7;
        else if(r >= 8f)
            value = 6;
        else if(r >= 6f)
            value = 5;
        else if(r >= 4f)
            value = 4;
        else if(r >= 2f)
            value = 3;
        else if(r >= 1f)
            value = 2;
        else if(r >= .5f)
            value = 1;
        else if(r >= 0f)
            value = 0;

        return value << 27;
    }
    private static int priorityToInt(int p){
        return p << 23;
    }
    private static int neededToInt(int n){
        return n << 20;
    }
    private static int locationToInt(MapLocation loc){
        //1.705 = 1023/600
        return (round(loc.x * 1.705f) << 10) | round(loc.y * 1.705f);
    }
    private static float intToRadius(int i){
        int value = i >>> 27;
        switch(value){
            case 0:
                return 0f;
            case 1:
                return .5f;
            case 2:
                return 1f;
            case 3:
                return 2f;
            case 4:
                return 4f;
            case 5:
                return 6f;
            case 6:
                return 8f;
            case 7:
                return 10f;
        }
        return -1f;
    }
    private static int intToPriority(int i){
        return (i >>> 23) & 0b1111;
    }
    private static int intToNeeded(int i){
        return (i >>> 20) & 0b111;
    }
    static MapLocation intToLocation(int i){
        //0.5865102639 = 600/1023
        return new MapLocation(0.5865102639f * ((i >>> 10) & 0b1111111111), 0.5865102639f * (i & 0b1111111111));
    }

    //integration methods
    public static void pickDest(boolean reached) throws GameActionException{
        if(DEBUG2) {
            if (reached)
                System.out.print("\nReached my destination!");
            else
                System.out.print("\nGot stuck!");
            printedThisTurn = true;
        }

        if(reached){
            //reset
            excludeLocations = new MapLocation[25];
            excludeTrees = new MapLocation[25];
            excludeLocationsSize = 0;
            excludeTreesSize = 0;
            if(traveling){
                rc.broadcast(travelingChannel, 0);
                travelingChannel = -1;
            }
        }
        else if (!exploring){
            //add to exclusions
            if(traveling){
                if(excludeLocationsSize != 25) {
                    excludeLocations[excludeLocationsSize] = Nav.dest;
                    excludeLocationsSize++;
                }
                else if(DEBUG1){
                    System.out.print("\n Reached location array limit!");
                }
            }
            else{
                if(excludeTreesSize != 25) {
                    excludeTrees[excludeTreesSize] = Nav.dest;
                    excludeTreesSize++;
                }
                else if(DEBUG1){
                    System.out.print("\n Reached tree array limit!");
                }
            }
        }
        if(traveling)
            traveling = false;

        if(bestTree == null) {
            Nav.setDest(rc.getLocation().add(new Direction(rng.nextFloat() * 2 * (float) Math.PI), 20));   //explorer code
            limit = 0f;
            exploring = true;
        }
        else{
            Nav.setDest(bestTree.location);
            limit = bestTree.radius + 1f;
            exploring = false;
            if(DEBUG2) {
                System.out.print("\nGoing after tree at " + bestTree.location.x + ", " + bestTree.location.y + ".");
                printedThisTurn = true;
            }
        }
    }
    private static void shakeATree() throws GameActionException {   //shakes a tree within range
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

    //methods to preserve bytecodes
    private static int round(float a){   //should take less bytecodes than Math.round
        return (int) (a+0.5f);
    }
}