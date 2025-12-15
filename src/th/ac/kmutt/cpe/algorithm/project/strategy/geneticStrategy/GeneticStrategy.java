package th.ac.kmutt.cpe.algorithm.project.strategy.geneticStrategy;

import java.util.*;

public class GeneticStrategy {
    private final int[][] map;
    private final int rows, cols;
    private final int startRow, startCol;
    private final int goalRow, goalCol;

    // GA Parameters
    private final int POPULATION_SIZE = 1500;
    private final int MAX_GENERATIONS = 100000;
    private int currentGenomeLength;

    // Stop Conditions
    private final int MAX_USELESS_RESETS = 5;
    private final long TIME_LIMIT_PER_ATTEMPT_MS = 59000; // 59s per attempt
    private long startTime;

    // Directions: 0=Down, 1=Right, 2=Up, 3=Left
    private final int[] dRow = {1, 0, -1, 0};
    private final int[] dCol = {0, 1, 0, -1};

    private final Random rand = new Random();

    private Agent bestAgentFound = null;
    private int bestCostFound = Integer.MAX_VALUE;


    public GeneticStrategy(int[][] map){
        this.map = map;
        this.rows = map.length;
        this.cols = map[0].length;

        // 1. HARDCODED ENDPOINTS
        this.startRow = 1;
        this.startCol = 1;
        this.goalRow = rows - 2;
        this.goalCol = cols - 2;

        System.out.println("[Config] Start: (" + startRow + "," + startCol + ") -> Goal: (" + goalRow + "," + goalCol + ")");

        // Reset State
        this.bestAgentFound = null;
        this.bestCostFound = Integer.MAX_VALUE;
        this.currentGenomeLength = rows * cols;
        this.startTime = System.currentTimeMillis();

        // Run the Solver (จะจบก็ต่อเมื่อ 59s หรือ status 5/5 หรือ gen limit)
        solve(createInitialPopulation());

        System.out.println();
    }

    private Agent[] createInitialPopulation() {
        Agent[] pop = new Agent[POPULATION_SIZE];
        for (int i = 0; i < POPULATION_SIZE; i++) {
            pop[i] = new Agent(currentGenomeLength, rows * cols);
            generateBiasedDNA(pop[i]);
        }
        return pop;
    }

    private void generateBiasedDNA(Agent agent) {
        int[] dna = agent.getDirect();
        int limit = Math.min(dna.length, currentGenomeLength);
        for (int j = 0; j < limit; j++) {
            dna[j] = (rand.nextDouble() < 0.7) ? getDirectionTowardsGoal() : rand.nextInt(4);
        }
        // กันกรณี Agent constructor ตั้ง evaluated เป็น true
        agent.setEvaluated(false);
    }

    private int getDirectionTowardsGoal() {
        if (rand.nextBoolean()) return (goalRow > startRow) ? 0 : 2;
        else return (goalCol > startCol) ? 1 : 3;
    }

    private boolean isTimeUp() {
        long elapsed = System.currentTimeMillis() - startTime;
        return elapsed >= TIME_LIMIT_PER_ATTEMPT_MS;
    }

    public void solve(Agent[] population) {
        int gen = 0;
        int noImprovement = 0;
        int consecutiveResetsWithoutGain = 0;
        int lastRecordedBest = Integer.MAX_VALUE;

        while (gen < MAX_GENERATIONS) {
            if (isTimeUp()) {
                break; // ออกเพราะครบ 59s
            }

            // 1. Evaluate
            Arrays.stream(population).parallel().forEach(this::evaluateAgent);
            Arrays.sort(population, Comparator.comparingDouble(Agent::getFitness).reversed());

            // 2. Track Best
            Agent currentBest = population[0];

            if (isGoalReached(currentBest)) {
                int realCost = currentBest.getCalculatedTime();

                if (realCost < bestCostFound) {
                    bestCostFound = realCost;
                    bestAgentFound = cloneAgent(currentBest);
                    cleanCoordinateAgent(bestAgentFound);

                    noImprovement = 0;
                    consecutiveResetsWithoutGain = 0;

                    // Optimization
                    int pathLen = bestAgentFound.pathMap.size();
                    if (pathLen > 0) {
                        int proposedLength = (int)(pathLen * 2.0);
                        int minSafeLength = Math.min(rows * cols, 500);
                        if (proposedLength < currentGenomeLength) {
                            currentGenomeLength = Math.max(proposedLength, minSafeLength);
                            System.out.println(">>> Optimizing Genome Size: " + currentGenomeLength);
                        }
                    }

                    long elapsed = System.currentTimeMillis() - startTime;
                    System.out.println(">>> NEW RECORD: " + bestCostFound + " (Gen " + gen + ", " + elapsed/1000.0 + "s) <<<");

                } else {
                    noImprovement++;
                }
            } else {
                if (bestCostFound != Integer.MAX_VALUE) noImprovement++;
            }

            // 3. Stagnation Breaker
            if (noImprovement > 400 && bestAgentFound != null) {
                System.out.println("--- STAGNATION (" + noImprovement + "). Mass Mutation... ---");

                if (bestCostFound < lastRecordedBest) {
                    lastRecordedBest = bestCostFound;
                    consecutiveResetsWithoutGain = 0;
                } else {
                    consecutiveResetsWithoutGain++;
                    System.out.println("   (Status: " + consecutiveResetsWithoutGain + "/" + MAX_USELESS_RESETS + " resets w/o gain)");
                }

                if (consecutiveResetsWithoutGain >= MAX_USELESS_RESETS) {
                    System.out.println(">>> STOPPING: Converged at " + bestCostFound + ".");
                    break; // ออกเพราะ status 5/5
                }

                population[0] = cloneAgent(bestAgentFound);
                int mutants = (int)(POPULATION_SIZE * 0.4);
                for(int i=1; i<=mutants; i++) {
                    population[i] = cloneAgent(bestAgentFound);
                    mutateScramble(population[i]);
                }
                for(int i=mutants+1; i<POPULATION_SIZE; i++) {
                    population[i] = new Agent(currentGenomeLength, rows * cols);
                    generateBiasedDNA(population[i]);
                }
                noImprovement = 0;
            }

            if (gen % 100 == 0) {
                String status = (bestCostFound == Integer.MAX_VALUE) ? "Searching..." : String.valueOf(bestCostFound);
                long elapsed = System.currentTimeMillis() - startTime;
                System.out.println("Gen " + gen + " | Best: " + status + " | Time: " + elapsed/1000.0 + "s");
            }

            // 4. Breeding
            Agent[] nextGen = new Agent[POPULATION_SIZE];
            int eliteCount = (int)(POPULATION_SIZE * 0.02);
            for (int i = 0; i < eliteCount; i++) nextGen[i] = cloneAgent(population[i]);

            for (int i = eliteCount; i < POPULATION_SIZE; i++) {
                Agent p1 = tournamentSelect(population);
                Agent p2 = tournamentSelect(population);
                Agent child = crossover(p1, p2);
                mutate(child);
                nextGen[i] = child;
            }
            population = nextGen;
            gen++;
        }

        // ===== FINAL RESULT =====
        if (bestAgentFound != null && isGoalReached(bestAgentFound)) {
            cleanCoordinateAgent(bestAgentFound);
            long totalTime = System.currentTimeMillis() - startTime;
            System.out.println("=== FINAL RESULT ===");
            System.out.println("Total Time: " + totalTime/1000.0 + " seconds");
            System.out.println("Final Lowest Cost: " + bestAgentFound.getCalculatedTime());
            System.out.println("Path Length: " + bestAgentFound.pathMap.size() + " steps");
        } else {
            System.out.println("Failed to find path.");
        }
    }

    private void evaluateAgent(Agent agent) {
        if (agent.isEvaluated()) return;
        agent.clearPath();

        boolean[] visited = new boolean[rows * cols];
        int r = startRow;
        int c = startCol;
        int startCoord = r * cols + c;
        agent.addPath(startCoord);
        visited[startCoord] = true;

        int cost = 0;
        int[] dna = agent.getDirect();
        boolean reached = false;
        int limit = Math.min(dna.length, currentGenomeLength);

        for (int k = 0; k < limit; k++) {
            int move = dna[k];
            int nr = r + dRow[move];
            int nc = c + dCol[move];

            if (nr < 0 || nr >= rows || nc < 0 || nc >= cols || nc >= map[nr].length || map[nr][nc] == -1) {
                continue;
            }

            int nextCoord = nr * cols + nc;
            if (visited[nextCoord]) continue;

            r = nr; c = nc;
            cost += map[r][c];
            agent.addPath(nextCoord);
            visited[nextCoord] = true;

            if (r == goalRow && c == goalCol) {
                reached = true;
                break;
            }
        }

        double fitness;
        if (reached) {
            fitness = 100_000_000.0 + (1_000_000.0 - cost);
        } else {
            int dist = Math.abs(goalRow - r) + Math.abs(goalCol - c);
            fitness = 10_000.0 - (dist * 10.0);
        }
        agent.setFitness((int)fitness);
        agent.setCalculatedTime(cost);
        agent.setEvaluated(true);
    }

    private Agent tournamentSelect(Agent[] pop) {
        Agent best = null;
        for (int i = 0; i < 5; i++) {
            Agent cand = pop[rand.nextInt(POPULATION_SIZE)];
            if (best == null || cand.getFitness() > best.getFitness()) best = cand;
        }
        return best;
    }

    private Agent crossover(Agent p1, Agent p2) {
        int len1 = p1.getDirect().length;
        int len2 = p2.getDirect().length;
        int targetLen = currentGenomeLength;
        Agent child = new Agent(targetLen, rows * cols);

        int[] c = child.getDirect();
        int[] d1 = p1.getDirect();
        int[] d2 = p2.getDirect();

        int cut = rand.nextInt(targetLen);
        int limit = Math.min(targetLen, Math.min(len1, len2));

        for(int i=0; i<limit; i++) c[i] = (i < cut) ? d1[i] : d2[i];

        child.setEvaluated(false);
        return child;
    }

    private void mutate(Agent agent) {
        int[] dna = agent.getDirect();
        int limit = Math.min(dna.length, currentGenomeLength);
        for(int i=0; i<limit; i++) {
            if(rand.nextDouble() < 0.01) dna[i] = rand.nextInt(4);
        }
        agent.setEvaluated(false);
    }

    private void mutateScramble(Agent agent) {
        int[] dna = agent.getDirect();
        int limit = Math.min(dna.length, currentGenomeLength);
        int start = rand.nextInt(Math.max(1, limit / 2));
        for(int i=start; i<limit; i++) dna[i] = rand.nextInt(4);
        agent.setEvaluated(false);
    }

    private boolean isGoalReached(Agent agent) {
        if (agent == null || agent.pathMap == null || agent.pathMap.isEmpty()) return false;
        int last = agent.pathMap.get(agent.pathMap.size()-1);
        return (last/cols == goalRow && last%cols == goalCol);
    }

    private Agent cloneAgent(Agent original) {
        int len = original.getDirect().length;
        if (len < currentGenomeLength) len = currentGenomeLength;

        Agent clone = new Agent(len, rows * cols);
        int copyLimit = Math.min(original.getDirect().length, len);
        System.arraycopy(original.getDirect(), 0, clone.getDirect(), 0, copyLimit);

        clone.setFitness(original.getFitness());
        clone.setCalculatedTime(original.getCalculatedTime());
        clone.pathMap = new ArrayList<>(original.pathMap);
        clone.setEvaluated(original.isEvaluated());

        return clone;
    }

    private void cleanCoordinateAgent(Agent agent){
        List<Integer> raw = agent.pathMap;
        int gCoord = (goalRow * cols) + goalCol;
        int idx = raw.indexOf(gCoord);
        if(idx != -1 && idx < raw.size()-1) raw.subList(idx+1, raw.size()).clear();
    }

    public int findFinalTime() {
        if (bestAgentFound != null && isGoalReached(bestAgentFound)) return bestAgentFound.getCalculatedTime();
        return -1;
    }

    public List<Integer> getCoordinate() {
        if (bestAgentFound != null && isGoalReached(bestAgentFound)) return new ArrayList<>(bestAgentFound.pathMap);
        return new ArrayList<>();
    }
}
