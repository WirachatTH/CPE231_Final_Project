package th.ac.kmutt.cpe.algorithm.project.strategy.geneticStrategy;

import java.util.*;

public class GeneticStrategy {
    private final int[][] map;
    private final int rows, cols;
    private final int startRow, startCol;
    private final int goalRow, goalCol;

    private int pathLimit;
    private Agent finalAgent;
    
    // ทิศทาง: 0=Down, 1=Right, 2=Up, 3=Left
    private final int[] dRow = {1, 0, -1, 0};
    private final int[] dCol = {0, 1, 0, -1};

    public GeneticStrategy(int[][] map){
        this.map = map;
        this.rows = map.length;
        this.cols = map[0].length;
        this.startRow = 1;
        this.startCol = 1;
        this.goalRow = rows - 2;
        this.goalCol = cols - 2;
        this.pathLimit = (rows * cols); 
        
        Agent[] population = createInitial();
        this.solve(population);
    }
    
    public void solve(Agent[] population){
        int maxGenerations = 25000; 
        Agent[] tempPopulation = new Agent[population.length];
        
        int totalStagnation = 0;
        int explosionTimer = 0;
        
        double lastBestFitness = -1;
        boolean solutionFound = false;

        for (int gen = 0; gen < maxGenerations; gen++) {
            
            // 1. คำนวณ Fitness
            for(Agent agent : population){
                findFitness(agent);
            }

            // 2. เรียงลำดับ (Fitness มาก -> น้อย)
            Arrays.sort(population, Comparator.comparing((Agent a) -> a.getFitness()).reversed());

            double currentBestFitness = population[0].getFitness();
            
            // เจอ Goal ครั้งแรก
            if (isGoalReached(population[0])) {
                if (!solutionFound) {
                    System.out.println(">>> First Path Found at Gen: " + gen + " | Time: " + findTime(population[0]) + " <<<");
                    solutionFound = true;
                }
            }

            // เช็คว่าคะแนนดีขึ้นไหม
            if (Math.abs(currentBestFitness - lastBestFitness) < 0.001) {
                totalStagnation++;
                explosionTimer++;
            } else {
                if (solutionFound) {
                    System.out.println(">>> New Best Path Optimized! Gen: " + gen + " | Time: " + findTime(population[0]) + " <<<");
                }
                totalStagnation = 0;
                explosionTimer = 0;
                lastBestFitness = currentBestFitness;
            }

            // 3. เงื่อนไขหยุด
            if (solutionFound && totalStagnation > 2500) {
                 break;
            }

            // --- ระเบิดประชากร ---
            boolean triggerExplosion = (explosionTimer > 300);
            if (triggerExplosion) {
                explosionTimer = 0; 
            }

            // 4. Elitism
            int elitismCount = (int)(population.length * 0.1); 
            if (elitismCount < 2) elitismCount = 2;
            System.arraycopy(population, 0, tempPopulation, 0, elitismCount);

            double currentMutationRate = (explosionTimer > 100) ? 0.9 : 0.4; 
            
            // 5. สร้างประชากรใหม่
            for(int i = elitismCount; i < population.length; i++){
                double survivors = (rows < 30 && cols < 30) ? 0.45 : 0.6; 
                if (triggerExplosion && i > (population.length * survivors)) {
                    tempPopulation[i] = new Agent(pathLimit);
                    randomPathNew(tempPopulation[i]);
                } else {
                    tempPopulation[i] = startCorssOver(population);
                    mutate(tempPopulation[i], currentMutationRate);
                    updatePathMapFromDirect(tempPopulation[i]);
                }
            }
            
            population = tempPopulation.clone();
            
            if (gen % 500 == 0) {
                System.out.println("Gen " + gen + " | Best Fitness: " + population[0].getFitness() + " | Time: " + findTime(population[0]));
                // PathFindingResult result = new PathFindingResult();
                // result.visualizePath(map, population[population.length/2].pathMap);
            }
        }

        finalAgent = population[0];
        cleanCoordinateAgent(finalAgent);
        
        System.out.println("Time taken by Best Agent (Optimized): " + findFinalTime());
    }

    // --- Initialization ---
    public Agent[] createInitial(){
        int popSize = 300;
        Agent[] population = new Agent[popSize]; 

        for(int i = 0; i < popSize; i++){
            population[i] = new Agent(pathLimit);
            randomPathNew(population[i]);
        }
        return population;
    }

    private void randomPathNew(Agent agent){
        int[] direct = agent.getDirect();
        int sizePath = direct.length;
        
        agent.pathMap.clear();
        agent.pathMap.add(convertToCoordinate(startRow, startCol));
        
        int currentRow = startRow;
        int currentCol = startCol;

        for(int i = 0; i < sizePath; i++){
            int selectedMove = selectMoveWithBias(currentRow, currentCol, agent, (i > 0 ? direct[i-1] : -1));

            if (selectedMove == -1) break;
            
            direct[i] = selectedMove;
            currentRow += dRow[selectedMove];
            currentCol += dCol[selectedMove];
            agent.setPath(i+1, convertToCoordinate(currentRow, currentCol));
            
            if (currentRow == goalRow && currentCol == goalCol) break;
        }
    }

    private int selectMoveWithBias(int r, int c, Agent agent, int prevMove) {
        List<Integer> validMoves = new ArrayList<>();
        
        for(int j = 0; j < 4; j++){
            int nextR = r + dRow[j];
            int nextC = c + dCol[j];
            
            if(nextR >= 0 && nextR < rows && nextC >= 0 && nextC < cols) {
                int nextID = convertToCoordinate(nextR, nextC);
                if( map[nextR][nextC] != -1 && !agent.pathMap.contains(nextID) ){
                    validMoves.add(j);
                }
            }
        }
        
        // ห้าม U-Turn
        if(prevMove != -1 && validMoves.size() > 1){
            int opposite = (prevMove + 2) % 4;
            validMoves.remove((Integer)opposite);
        }

        // Dead End Check
        List<Integer> smartMoves = new ArrayList<>();
        for (int move : validMoves) {
            int nextR = r + dRow[move];
            int nextC = c + dCol[move];
            if (!isDeadEnd(nextR, nextC, agent)) {
                smartMoves.add(move);
            }
        }

        List<Integer> candidates = !smartMoves.isEmpty() ? smartMoves : validMoves;
        if (candidates.isEmpty()) return -1;

        candidates.sort((m1, m2) -> {
            int r1 = r + dRow[m1];
            int c1 = c + dCol[m1];
            int dist1 = Math.abs(goalRow - r1) + Math.abs(goalCol - c1);
            int cost1 = map[r1][c1]; 

            int r2 = r + dRow[m2];
            int c2 = c + dCol[m2];
            int dist2 = Math.abs(goalRow - r2) + Math.abs(goalCol - c2);
            int cost2 = map[r2][c2];

            double weight = 5.0 + (new Random().nextDouble() * 10.0);
            double score1 = dist1 + (cost1 * weight);
            double score2 = dist2 + (cost2 * weight);

            return Double.compare(score1, score2);
        });

        Random rand = new Random();
        double rate = (rows < 30 && cols < 30)? 0.75 : 0.05;
        if (rand.nextDouble() < rate) {
            return candidates.get(0);
        } else {
            return candidates.get(rand.nextInt(candidates.size()));
        }
    }

    private boolean isDeadEnd(int r, int c, Agent agent) {
        if (r == goalRow && c == goalCol) return false;
        int blockedCount = 0;
        for (int i = 0; i < 4; i++) {
            int nr = r + dRow[i];
            int nc = c + dCol[i];
            if (nr < 0 || nr >= rows || nc < 0 || nc >= cols || map[nr][nc] == -1 || agent.pathMap.contains(convertToCoordinate(nr, nc))) {
                blockedCount++;
            }
        }
        return blockedCount >= 3;
    }

    // --- Genetic Operations ---
    public Agent startCorssOver(Agent[] population){
        Random rand = new Random();
        int limit = (int)(population.length * 1); 
        if (limit < 2) limit = population.length;
        
        int indexP1 = rand.nextInt(limit);
        int indexP2 = rand.nextInt(limit);
        while(indexP1 == indexP2) indexP2 = rand.nextInt(limit);
        
        return this.crossover(population[indexP1], population[indexP2]);
    }

    private Agent crossover(Agent parentA, Agent parentB){
        int[] directA = parentA.getDirect();
        int[] directB = parentB.getDirect();
        int len = directA.length;
        Random rand = new Random();

        int cutIndex = rand.nextInt(len - 2) + 1; 

        Agent newAgent = new Agent(len);
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
            while(filledUntil < len) newDirect[filledUntil++] = rand.nextInt(4);
        } else {
            int filledUntil = cutIndex;
            while(filledUntil < len) newDirect[filledUntil++] = rand.nextInt(4);
        }
        return newAgent;
    }

    private void mutate(Agent agent, double mutationRate) {
        Random rand = new Random();
        int[] direct = agent.getDirect();
        
        if (rand.nextDouble() < mutationRate) {
            int len = direct.length;
            int mutateIndex = rand.nextInt(len - 1) + 1;

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
        }
    }

    private void repairPathTail(Agent agent, int startR, int startC, int startIndex) {
        int[] direct = agent.getDirect();
        int currentRow = startR;
        int currentCol = startC;
        List<Integer> path = agent.pathMap;

        if (startIndex < path.size()) {
            path.subList(startIndex, path.size()).clear();
            path.add(convertToCoordinate(currentRow, currentCol));
        } else {
             path.clear(); 
             path.add(convertToCoordinate(currentRow, currentCol));
        }

        for(int i = startIndex; i < direct.length; i++){
            int selectedMove = selectMoveWithBias(currentRow, currentCol, agent, (i > 0 ? direct[i-1] : -1));
            if (selectedMove == -1) {
                direct[i] = new Random().nextInt(4); 
            } else {
                direct[i] = selectedMove;
                currentRow += dRow[selectedMove];
                currentCol += dCol[selectedMove];
                path.add(convertToCoordinate(currentRow, currentCol)); 
                if (currentRow == goalRow && currentCol == goalCol) break;
            }
        }
    }

    private void updatePathMapFromDirect(Agent agent) {
        agent.pathMap.clear();
        int curR = startRow;
        int curC = startCol;
        agent.pathMap.add(convertToCoordinate(curR, curC));
        int[] direct = agent.getDirect();
        for(int i=0; i<direct.length; i++) {
            int move = direct[i];
            int nextR = curR + dRow[move];
            int nextC = curC + dCol[move];
            if(nextR < 0 || nextR >= rows || nextC < 0 || nextC >= cols || map[nextR][nextC] == -1) break; 
            curR = nextR;
            curC = nextC;
            agent.pathMap.add(convertToCoordinate(curR, curC));
        }
    }

    public void findFitness(Agent agent){
        int[] direct = agent.getDirect();
        int curR = startRow;
        int curC = startCol;
        
        double score = 10000; 
        
        boolean reachedGoal = false;
        
        for(int i = 0; i < direct.length; i++){
            int move = direct[i];
            int nextR = curR + dRow[move];
            int nextC = curC + dCol[move];
            
            // ชนกำแพง
            if(nextR < 0 || nextR >= rows || nextC < 0 || nextC >= cols || map[nextR][nextC] == -1){
                score -= 5000;
                break; 
            }
            
            curR = nextR;
            curC = nextC;
            
            // Exploration Bonus:
            score += 1; 

            if(curR == goalRow && curC == goalCol){
                reachedGoal = true;
                break;
            }
        }

        // Distance Calculation
        int distance = Math.abs(goalRow - curR) + Math.abs(goalCol - curC);

        if (reachedGoal) {
            score += 100000; // รางวัลใหญ่
            score -= (findTime(agent) * 7);
        } else {
            score -= (distance * 20); 
        }

        if (score < 1) score = 1;
        agent.setFitness((int)score);
    }

    private boolean isGoalReached(Agent agent) {
        return agent.getFitness() > 50000; 
    }

    private void cleanCoordinateAgent(Agent agent){
        List<Integer> rawPath = agent.pathMap;
        List<Integer> cleanPath = new ArrayList<>();
        for (Integer coord : rawPath) {
            int existingIndex = cleanPath.indexOf(coord);
            if (existingIndex != -1) {
                cleanPath.subList(existingIndex + 1, cleanPath.size()).clear();
            } else {
                cleanPath.add(coord);
            }
            int r = coord / cols; 
            int c = coord % cols; 
            if (r == goalRow && c == goalCol) break; 
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
    private int findTime(Agent agent){
        int useTime = 0;
        List<Integer> path = agent.pathMap;
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