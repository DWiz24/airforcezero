package airforcezero;
import battlecode.common.*;

import java.util.Random;
import java.lang.Math;

public class Lumberjack {
    //global stuff

    static final boolean DEBUG1 = true, DEBUG2 = true;  //set to false to make them shut up

    //general
    private static RobotController rc;
    private static Random rng;

    //variables related to my robot's behavior
    private static boolean traveling, locationsNear, printedThisTurn;
    private static int travelingChannel;
    private static MapLocation prevGardenerPos;

    //channels
    private static int prevNext, next = -1; //previous and current turn next tree channel to write to

    //info about my surroundings, updates every turn
    private static RobotInfo[] allRobots, friendlyRobots, enemyRobots, friendlyGardeners;
    private static TreeInfo[] allTrees, friendlyTrees, neutralTrees, enemyTrees;
    private static float[] allRobotDists, friendlyRobotDists, enemyRobotDists, allTreeDists, friendlyTreeDists, neutralTreeDists, enemyTreeDists;
    private static int friendlyRobotCount, enemyRobotCount, friendlyTreeCount, neutralTreeCount, enemyTreeCount, friendlyGardenerCount;
    private static float bestPriority;
    private static int bestPriorityStatic;
    private static TreeInfo bestTree, bestTreeStatic;
    
    /*
    -If nothing better to do, lumberjacks wander off to search for trees
    -Make own decisions
    -Report locations where support is needed to array
        
    -TODO:
    -strategic determination of which trees to chop
    -Choosing a good location to stand while cutting trees
    -(Important) running away
    -bullet dodging
    -combat (if necessary) + calling combat
    */
    public static void run(RobotController rcArg) throws GameActionException{
        //general
        rc = rcArg;
        rng = new Random(rc.getID());

        //variables related to my robot's behavior
        printedThisTurn = false;
        if(DEBUG1){
            System.out.print("\nI am a lumberjack!");
            printedThisTurn = true;
        }
        traveling = false;
        travelingChannel = -1;
        locationsNear = false;
        prevGardenerPos = rc.getInitialArchonLocations(rc.getTeam())[0];    //in case gardener runs away

        //channels
        next = rc.readBroadcast(15);
        if (next == 0) { //in case this is the first lumberjack, set the index to default
            rc.broadcast(15, 16);
            next = 16;
        }

        //code below repeats every turn
        while (true) {
            //updating info about robots and trees around me
            updateInfo();
            prevNext = next;
            next = rc.readBroadcast(15);
            if (friendlyGardenerCount > 0) {
                prevGardenerPos = friendlyGardeners[0].location;
            }

            //check for better locations
            if(!traveling){
                chooseBestLocation();
            }
            else
                //make a call to get whether locations are around, since chooseBestLocation was never called
                areLocationsNear();
            if(!locationsNear && bestTreeStatic != null && rc.getLocation().distanceTo(bestTreeStatic.location) < 7 && bestPriorityStatic * 66.66666667f + dynamicPriorityFromBase(bestTreeStatic) > shrinkingPriority())
                //if not locations in range and found locations worth reporting
                lumberajckNeeded(bestTreeStatic.location, bestPriorityStatic, 1);

            //striking
            boolean hasStruck = false;
            if(bodiesWithinDistance(enemyRobots, enemyRobotDists, 2) > 0 && bodiesWithinDistance(friendlyRobots, friendlyRobotDists, 2) == 0) {
                rc.strike();
                hasStruck = true;
            }

            //move, shake, and chop
            MapLocation move = Nav.lumberjackNav(rc, allTrees, allRobots, allTreeDists, allRobotDists, hasStruck);
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
                        if(info.type == RobotType.LUMBERJACK){
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
                int staticPriority = staticPriorityOfTree(info);
                float priority = staticPriority * 66.66666667f + dynamicPriorityOfTree(info);
                if(staticPriority > -1 && priority > bestPriority){
                    bestPriority = priority;
                    bestTree = info;
                }
                if(staticPriority > bestPriorityStatic){
                    bestPriorityStatic = staticPriority;
                    bestTreeStatic = info;
                }
                treeCount++;
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
                        if(info.type == RobotType.LUMBERJACK){
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
                int staticPriority = staticPriorityOfTree(info);
                float priority = staticPriority * 66.66666667f + dynamicPriorityOfTree(info);
                if(staticPriority > -1 && priority > bestPriority){
                    bestPriority = priority;
                    bestTree = info;
                }
                if(staticPriority > bestPriorityStatic){
                    bestPriorityStatic = staticPriority;
                    bestTreeStatic = info;
                }
                treeCount++;
            }
        }
    }
    private static int bodiesWithinDistance(BodyInfo[] bodies, float[] dists, float dmax){
        //gives the amount of bodies from given sorted array (represented by dists) that are closer than distance
        int count = 0;
        for(int i = 0; i < dists.length; i++){
            //0.0 is default for float arrays
            if(dists[i] == 0.0f)
                break;
            else if(dists[i] - bodies[i].getRadius() <= dmax)
                count++;
            else if(dists[i] - 2 > dmax)
                break;
        }
        return count;
    }
    private static float shrinkingPriority(){
        //950 - 800
        return 950f - (rc.getRoundNum())/20f;
    }
    private static void areLocationsNear() throws GameActionException{
        locationsNear = false;

        for(int i = 16; i < 30; i++){
            int value = rc.readBroadcast(i);
            MapLocation valueLoc = intToLocation(value);
            if(value == 0)
                continue;

            if(!locationsNear && valueLoc.distanceTo(rc.getLocation()) <= 7f) {
                locationsNear = true;
                return;
            }
        }
    }

    //movement stuff
    private static MapLocation pickNearTreeLoc(TreeInfo tree) throws GameActionException{
        //Does exactly what it says.
        //Takes over approaching trees when close (dist <= 1 + maxTreeRadius).
        //TODO: make gardeners leave room for others.
        MapLocation nearTreeLoc = tree.location.add(tree.location.directionTo(rc.getLocation()), tree.radius + RobotType.LUMBERJACK.bodyRadius + 0.001f);
        Nav.setDest(nearTreeLoc);
        return nearTreeLoc;
    }

    //tree handling stuff
    public static void lumberajckNeeded(MapLocation location, int priority, int number) throws GameActionException{
        //PLEASE USE
        //priority can be in range [0, 15] (use something greater than 10)
        //number can be in range [0, 7], and this number of lumberjacks will arrive

        if(Lumberjack.DEBUG1){
            System.out.print("\nRequested " + number + " lumberjack(s) at " + location.x + ", " + location.y + " with priority " + priority + ".");
            printedThisTurn = true;
            if(rc.getTeam() == Team.A)
                rc.setIndicatorDot(location, 200, 255, 0);
            else
                rc.setIndicatorDot(location, 0, 255, 200);
        }

        if(rc.getType() != RobotType.LUMBERJACK)
            next = rc.readBroadcast(15);

        rc.broadcast(next, priorityToInt(priority) | neededToInt(number) | locationToInt(location));
        next++;

        if(next == 30)
            next = 16;

        rc.broadcast(15, next);
    }
    private static void chooseBestLocation() throws GameActionException{
        chooseBestLocation(null);
    }
    private static void chooseBestLocation(MapLocation exclude) throws GameActionException{
        float bestP = bestPriority + 100; //additional 100 for preferring to stay where I am
        int bestValue = -1;
        int bestChannel = -1;
        MapLocation bestLocation = null;

        locationsNear = false;

        for(int i = 16; i < 30; i++){
            int value = rc.readBroadcast(i);
            MapLocation valueLoc = intToLocation(value);
            if(value == 0)
                continue;

            if(!locationsNear && valueLoc.distanceTo(rc.getLocation()) <= 7f)
                locationsNear = true;

            //check for exclusion
            if(exclude != null) {
                if(exclude.equals(valueLoc))
                    continue;
            }

            //check for 0 needed
            if(intToNeeded(value) == 0)
                continue;

            float priority = intToPriority(value) * 66.66666667f + dynamicPriorityOfTree(valueLoc);

            if(priority > bestP){
                bestP = priority;
                bestLocation = valueLoc;
                bestValue = value;
                bestChannel = i;
            }
        }

        if(bestLocation != null){
            rc.broadcast(bestChannel, isolatePriority(bestValue) | neededToInt((bestValue)-1) | isolateLocation(bestValue));
            traveling = true;
            travelingChannel = bestChannel;
            Nav.setDest(bestLocation);
            if(DEBUG2){
                System.out.print("Priority " + bestP + " exceeds current priority " + bestPriority + ".");
                printedThisTurn = true;
            }
            if(DEBUG1){
                System.out.print("\nTraveling to " + bestLocation.x + ", " + bestLocation.y + ".");
                printedThisTurn = true;
            }
        }
    }
    private static int staticPriorityOfTree(TreeInfo tree){
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
    private static int dynamicPriorityOfTree(TreeInfo treeInfo){
        //expanding circle goes from ~7 to ~28 distance
        int priority = 0;

        priority += 0.25f * dynamicPriorityFromBase(treeInfo);
        priority += 0.75f * dynamicPriorityFromMe(treeInfo);

        return priority;
    }
    private static int dynamicPriorityOfTree(MapLocation treeLoc){
        //expanding circle goes from ~7 to ~28 distance
        int priority = 0;

        priority += 0.25f * dynamicPriorityFromBase(treeLoc);
        priority += 0.75f * dynamicPriorityFromMe(treeLoc);

        return priority;
    }
    private static float dynamicPriorityFromBase(TreeInfo treeInfo){
        return 7.071067812f * (141.4213562f - prevGardenerPos.distanceTo(treeInfo.location) + treeInfo.radius);
    }
    private static float dynamicPriorityFromMe(TreeInfo treeInfo){
        return 7.071067812f * (141.4213562f - rc.getLocation().distanceTo(treeInfo.location) + treeInfo.radius);
    }
    private static float dynamicPriorityFromBase(MapLocation treeLoc){
        return 7.071067812f * (141.4213562f - prevGardenerPos.distanceTo(treeLoc) + 0.5f);
    }
    private static float dynamicPriorityFromMe(MapLocation treeLoc){
        return 7.071067812f * (141.4213562f - rc.getLocation().distanceTo(treeLoc) + 0.5f);
    }
    //conversions
    //priority is integer [0, 15], 4-bit
    //needed is integer [0, 7], 3-bit
    //locations are float [0, 600] turned 10-bit
    private static int isolatePriority(int i){
        return i & 0b111100000000000000000000000;
    }
    private static int isolateLocation(int i){
        return i & 0b11111111111111111111;
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
    private static int intToPriority(int i){
        return i >>> 23;
    }
    private static int intToNeeded(int i){
        return (i >>> 20) & 0b111;
    }
    private static MapLocation intToLocation(int i){
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

        if(traveling && reached){
            traveling = false;
            rc.broadcast(travelingChannel, 0);
            travelingChannel = -1;
        }
        if(bestTree == null) {
            Nav.setDest(rc.getLocation().add(new Direction(rng.nextFloat() * 2 * (float) Math.PI), 20));   //explorer code
        }
        else{
            Nav.setDest(bestTree.location);
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