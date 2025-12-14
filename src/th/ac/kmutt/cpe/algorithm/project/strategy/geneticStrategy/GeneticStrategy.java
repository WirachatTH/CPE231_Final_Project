package th.ac.kmutt.cpe.algorithm.project.strategy.geneticStrategy;

import java.util.*;

public class GeneticStrategy {
    private final int[][] map;
    private final int rows, cols;
    private final int startRow, startCol;
    private final int goalRow, goalCol;

    private int pathLimit;
    private Agent finalAgent;
    private int mapSize;

    private final Random globalRand = new Random();

    // ทิศทาง: 0=Down, 1=Right, 2=Up, 3=Left
    private final int[] dRow = {1, 0, -1, 0};
    private final int[] dCol = {0, 1, 0, -1};

    public GeneticStrategy(int[][] map){
        this.map = map;
        this.rows = map.length;
        this.cols = map[0].length;
        this.mapSize = rows * cols;
        this.startRow = 1;
        this.startCol = 1;
        this.goalRow = rows - 2;
        this.goalCol = cols - 2;
        this.pathLimit = ((rows * cols)/12 < 400)? (int)((rows * cols)/1.25) : (rows * cols)/12 ; 
        
        Agent[] population = createInitial();
        this.solve(population);
    }
    
    public void solve(Agent[] population){
        Agent[] tempPopulation = new Agent[population.length];
        int maxGeneration = 50000;
        int totalStagnation = 0;
        int explosionTimer = 0;
        double lastBestFitness = -1;
        boolean solutionFound = false;
        int gen = 0;

        int stagnationLimit = 200 * cols;
        int explosionLimit = rows * 20;

        while (true) {
            
            // 1. Evaluate
            Arrays.stream(population).parallel().forEach(this::evaluateAgent);

            // 2. เรียงลำดับ Fitness
            Arrays.parallelSort(population, Comparator.comparingInt(Agent::getFitness).reversed());

            double currentBestFitness = population[0].getFitness();
            
            // เจอ Goal ครั้งแรก
            if (isGoalReached(population[0])) {
                if (!solutionFound) {
                    // [Performance] ดึงเวลาจากที่คำนวณไว้แล้ว ไม่ต้องหาใหม่
                    System.out.println(">>> First Path Found at Gen: " + gen + " | Time: " + population[0].getCalculatedTime() + " <<<");
                    solutionFound = true;
                }
            }

            // เช็ค Stagnation
            if (Math.abs(currentBestFitness - lastBestFitness) < 0.001) {
                totalStagnation++;
                explosionTimer++;
            } else {
                if (solutionFound) {
                    System.out.println(">>> New Best Path Optimized! Gen: " + gen + " | Time: " + population[0].getCalculatedTime() + " <<<");
                }
                totalStagnation = 0;
                explosionTimer = 0;
                lastBestFitness = currentBestFitness;
            }

            // 3. เงื่อนไขหยุด
            if (solutionFound && totalStagnation > stagnationLimit || gen == maxGeneration) {
                 break;
            }

            boolean triggerExplosion = (explosionTimer > explosionLimit);
            if (triggerExplosion) explosionTimer = 0;

            // 4. Elitism
            int elitismCount = (int)(population.length * 0.1); 
            if (elitismCount < 2) elitismCount = 2;
            System.arraycopy(population, 0, tempPopulation, 0, elitismCount);

            // Rescue Bad Winners logic
            int rescueCount = 0;
            int maxRescue = 30;
            
            for (int i = elitismCount; i < population.length; i += 15) {
                if (rescueCount >= maxRescue) break;
                if (isGoalReached(population[i])) {
                    tempPopulation[elitismCount + rescueCount] = population[i];
                    rescueCount++;
                } else {
                    break; 
                }
            }
            
            int totalProtected = elitismCount + rescueCount;
            double currentMutationRate = (explosionTimer > 100) ? 0.9 : 0.4; 
            
            // 5. สร้างประชากรใหม่
            for(int i = totalProtected; i < population.length; i++){
                double survivors = (rows < 30) ? 0.45 : 0.6; 
                
                if (triggerExplosion && i > (population.length * survivors)) {
                    tempPopulation[i] = new Agent(pathLimit, mapSize);
                    randomPathNew(tempPopulation[i]);
                } else {
                    tempPopulation[i] = startCrossOver(population);
                    mutate(tempPopulation[i], currentMutationRate);
                }
            }
            
            population = tempPopulation.clone();
            
            if (gen % 1000 == 0) {
                 System.out.println("Gen " + gen + " | Best Fitness: " + population[0].getFitness() + " | Time: " + population[0].getCalculatedTime());
            }
            gen++;
        }

        finalAgent = population[0];
        // Clean Path รอบสุดท้ายเพื่อความสวยงามก่อนจบ
        cleanCoordinateAgent(finalAgent);
        
        System.out.println("Time taken by Best Agent (Optimized): " + findFinalTime());
    }

    // --- Core Logic ใหม่ รวมร่าง Fitness + Simulation ---
    private void evaluateAgent(Agent agent) {
        if (agent.isEvaluated()) return;
        // 1. Reset Agent
        agent.clearPath();
        
        int curR = startRow;
        int curC = startCol;
        int startCoord = convertToCoordinate(curR, curC);
        
        agent.addPath(startCoord); // ใส่จุดเริ่ม + mark visited
        
        int[] direct = agent.getDirect();
        double score = 10000;
        int calculatedTime = 0;
        boolean reachedGoal = false;

        // 2. Simulate Walk
        for(int i = 0; i < direct.length; i++) {
            if (curR == goalRow && curC == goalCol) {
                reachedGoal = true;
                break;
            }

            int move = direct[i];
            int nextR = curR + dRow[move];
            int nextC = curC + dCol[move];
            
            // Check Bounds & Walls
            if(nextR < 0 || nextR >= rows || nextC < 0 || nextC >= cols || map[nextR][nextC] == -1) {
                score -= 5000;
                break; 
            }
            
            // Valid Move
            calculatedTime += map[nextR][nextC];
            curR = nextR;
            curC = nextC;
            
            agent.addPath(convertToCoordinate(curR, curC)); 
            
            score += 1;
        }

        // 3. Calculate Fitness
        int distance = Math.abs(goalRow - curR) + Math.abs(goalCol - curC);

        if (reachedGoal) {
            score += 100000; 
            score -= (calculatedTime * 7);
        } else {
            score -= (distance * 20); 
        }

        if (score < 1) score = 1;
        
        agent.setFitness((int)score);
        agent.setCalculatedTime(calculatedTime);
        agent.setEvaluated(true);
    }

    public Agent[] createInitial(){
        int popSize = 300;
        Agent[] population = new Agent[popSize]; 
        for(int i = 0; i < popSize; i++){
            population[i] = new Agent(pathLimit, mapSize);
            randomPathNew(population[i]);
        }
        return population;
    }

    private void randomPathNew(Agent agent){
        int[] direct = agent.getDirect();
        int sizePath = direct.length;
        
        agent.clearPath();
        int startCoord = convertToCoordinate(startRow, startCol);
        agent.addPath(startCoord);
        
        int currentRow = startRow;
        int currentCol = startCol;

        for(int i = 0; i < sizePath; i++){
            int selectedMove = selectMoveWithBias(currentRow, currentCol, agent, (i > 0 ? direct[i-1] : -1));

            if (selectedMove == -1) break;
            
            direct[i] = selectedMove;
            currentRow += dRow[selectedMove];
            currentCol += dCol[selectedMove];
            
            int newCoord = convertToCoordinate(currentRow, currentCol);
            agent.addPath(newCoord);
            
            if (currentRow == goalRow && currentCol == goalCol) break;
        }
    }

    private int selectMoveWithBias(int r, int c, Agent agent, int prevMove) {
        int[] candidates = new int[4];
        int count = 0;

        for(int j = 0; j < 4; j++){
            int nextR = r + dRow[j];
            int nextC = c + dCol[j];
            
            if(nextR >= 0 && nextR < rows && nextC >= 0 && nextC < cols) {
                int nextID = (nextR * cols) + nextC;

                // ห้าม U-Turn
                if( map[nextR][nextC] != -1 && !agent.isVisited(nextID) ){
                    if (prevMove != -1) {
                         int opposite = (prevMove + 2) % 4;
                         if (j == opposite) continue; 
                    }
                    candidates[count++] = j;
                }
            }
        }

        if (count == 0) return -1;
        if (count == 1) return candidates[0]; 

        double bestScore = Double.MAX_VALUE;
        int bestMove = candidates[0];
        
        for (int i = 0; i < count; i++) {
            int move = candidates[i];
            int r2 = r + dRow[move];
            int c2 = c + dCol[move];
            
            int dist = Math.abs(goalRow - r2) + Math.abs(goalCol - c2);
            int cost = map[r2][c2];
            
            double weight = 5.0 + (globalRand.nextDouble() * 10.0);
            double score = dist + (cost * weight);
            
            if (score < bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }

        double rate = (rows < 30 || rows > 60) ? 0.7 : 0.05;
        if (globalRand.nextDouble() < rate) {
            return bestMove; 
        } else {
            return candidates[globalRand.nextInt(count)]; 
        }
    }

    // --- Genetic Operations ---
    public Agent startCrossOver(Agent[] population){
        int limit = (int)(population.length * 1); 
        if (limit < 2) limit = population.length;
        
        int indexP1 = globalRand.nextInt(limit);
        int indexP2 = globalRand.nextInt(limit);
        while(indexP1 == indexP2) indexP2 = globalRand.nextInt(limit);

        Agent parentA = population[indexP1];
        Agent parentB = population[indexP2];
        
        if (globalRand.nextBoolean()) {
            return this.crossover(parentA, parentB);       
        } else {
            return this.crossoverMultiPointDynamic(parentA, parentB); 
        }
    }

    private Agent crossover(Agent parentA, Agent parentB){
        int[] directA = parentA.getDirect();
        int[] directB = parentB.getDirect();
        int len = directA.length;

        int cutIndex = globalRand.nextInt(len - 2) + 1; 

        Agent newAgent = new Agent(len, mapSize);
        int[] newDirect = newAgent.getDirect();
        System.arraycopy(directA, 0, newDirect, 0, cutIndex);

        Integer coordinateCut = -1;
        if (cutIndex < parentA.pathMap.size()) coordinateCut = parentA.pathMap.get(cutIndex);

        boolean hasPass = coordinateCut != -1 && parentB.pathMap.contains(coordinateCut);
        
        if(hasPass){
            int indexInB = parentB.pathMap.indexOf(coordinateCut);
            int startCopyB = indexInB + 1;
            int spaceLeft = len - cutIndex;
            int actualDataLeftInB = len - startCopyB; 
            int copyCount = Math.min(spaceLeft, actualDataLeftInB);
            if (copyCount > 0 && startCopyB < len) { 
                 System.arraycopy(directB, startCopyB, newDirect, cutIndex, copyCount);
            }
            int filledUntil = cutIndex + copyCount;
            while(filledUntil < len) newDirect[filledUntil++] = globalRand.nextInt(4);
        } else {
            int filledUntil = cutIndex;
            while(filledUntil < len) newDirect[filledUntil++] = globalRand.nextInt(4);
        }
        return newAgent;
    }

    private Agent crossoverMultiPointDynamic(Agent parentA, Agent parentB) {
        int[] directA = parentA.getDirect();
        int[] directB = parentB.getDirect();
        int len = directA.length;
        
        Agent newAgent = new Agent(len, mapSize);
        int[] newDirect = newAgent.getDirect();

        int mapScale = this.rows + this.cols;
        int numCuts = (mapScale / 40)+1; 
        if (numCuts > 3) numCuts = 3;

        Set<Integer> cutPoints = new TreeSet<>(); 
        while (cutPoints.size() < numCuts) {
            cutPoints.add(globalRand.nextInt(len - 2) + 1);
        }
        
        List<Integer> cuts = new ArrayList<>(cutPoints);
        cuts.add(len);

        int currentIndex = 0;
        boolean useParentA = true; 
        
        for (int cutIndex : cuts) {
            int lengthToCopy = cutIndex - currentIndex;
            if (useParentA) {
                System.arraycopy(directA, currentIndex, newDirect, currentIndex, lengthToCopy);
            } else {
                System.arraycopy(directB, currentIndex, newDirect, currentIndex, lengthToCopy);
            }
            currentIndex = cutIndex;
            useParentA = !useParentA;
        }
        return newAgent;
    }

    private void mutate(Agent agent, double mutationRate) {
        int[] direct = agent.getDirect();
        
        if (globalRand.nextDouble() < mutationRate) {
            int len = direct.length;
            int mutateIndex = globalRand.nextInt(len - 1) + 1;

            int currentR = startRow;
            int currentC = startCol;
            boolean possible = true;
            
            for (int i = 0; i < mutateIndex; i++) {
                currentR += dRow[direct[i]];
                currentC += dCol[direct[i]];
                if(currentR < 0 || currentR >= rows || currentC < 0 || currentC >= cols || map[currentR][currentC] == -1){
                    possible = false;
                    break;
                }
            }
            if(possible) repairPathTail(agent, currentR, currentC, mutateIndex);
            agent.markDirty();
        }
    }

    private void repairPathTail(Agent agent, int startR, int startC, int startIndex) {
        int[] direct = agent.getDirect();
        List<Integer> path = agent.pathMap;
        
        Arrays.fill(agent.visitedFlags, false);
        
        if (startIndex < path.size()) {
            path.subList(startIndex, path.size()).clear();
        } else {
            path.clear();
        }

        if(path.isEmpty()){
             int sc = convertToCoordinate(startRow, startCol);
             path.add(sc);
        }

        for(Integer coord : path){
            if(coord >= 0 && coord < mapSize) agent.visitedFlags[coord] = true;
        }

        int currentRow = startR; 
        int currentCol = startC;

        for(int i = startIndex; i < direct.length; i++){
            int selectedMove = selectMoveWithBias(currentRow, currentCol, agent, (i > 0 ? direct[i-1] : -1));
            
            if (selectedMove == -1) {
                direct[i] = globalRand.nextInt(4); 
            } else {
                if (currentRow == goalRow && currentCol == goalCol) break;
                direct[i] = selectedMove;
                currentRow += dRow[selectedMove];
                currentCol += dCol[selectedMove];
                
                int newCoord = convertToCoordinate(currentRow, currentCol);
                
                if (currentRow >= 0 && currentRow < rows && currentCol >= 0 && currentCol < cols) {
                    agent.addPath(newCoord); 
                }
            }
        }
    }

    private boolean isGoalReached(Agent agent) {
        return agent.getFitness() > 50000; 
    }

    private void cleanCoordinateAgent(Agent agent){
        List<Integer> rawPath = agent.pathMap;
        
        int goalCoord = convertToCoordinate(goalRow, goalCol);
        int goalIndex = rawPath.indexOf(goalCoord);
        
        if(goalIndex != -1 && goalIndex < rawPath.size() - 1){
            rawPath.subList(goalIndex + 1, rawPath.size()).clear();
        }
        
        List<Integer> cleanPath = new ArrayList<>();
        for (Integer coord : rawPath) {
            int existingIndex = cleanPath.indexOf(coord);
            if (existingIndex != -1) {
                cleanPath.subList(existingIndex + 1, cleanPath.size()).clear();
            } else {
                cleanPath.add(coord);
            }
        }
        agent.pathMap = cleanPath;
    }

    public int findFinalTime(){
        int useTime = 0;
        List<Integer> path = finalAgent.pathMap;
        for(int i = 1; i < path.size(); i++){
            int coord = path.get(i);
            int r = coord / cols; 
            int c = coord % cols;
            if(r >= 0 && r < rows && c >= 0 && c < cols) {
                useTime += map[r][c];
            }
        }
        return useTime;
    }

    private int convertToCoordinate(int r, int c){
        return (r * this.cols) + c; 
    }

    public List<Integer> getCoordinate(){
        return finalAgent.pathMap;
    }
}