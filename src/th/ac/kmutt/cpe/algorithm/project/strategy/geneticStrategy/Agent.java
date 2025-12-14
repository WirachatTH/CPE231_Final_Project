package th.ac.kmutt.cpe.algorithm.project.strategy.geneticStrategy;

import java.util.*;

public class Agent {
    private int[] direct;
    private int fitness;
    private int calculatedTime; 

    private boolean isEvaluated = false; 

    public List<Integer> pathMap; 
    public boolean[] visitedFlags; 
    private int mapSize; 

    public Agent(int directSize, int mapSize){
        this.direct = new int[directSize];
        
        this.pathMap = new ArrayList<>(directSize > 100 ? directSize : 100); 
        
        this.mapSize = mapSize;
        this.visitedFlags = new boolean[mapSize];

        pathMap.add(1001); 
        if (1001 < mapSize) visitedFlags[1001] = true;
    }

    public void addPath(int coordinate) {
        pathMap.add(coordinate);
        if (coordinate >= 0 && coordinate < mapSize) {
            visitedFlags[coordinate] = true;
        }
    }

    public void clearPath() {
        pathMap.clear();
        Arrays.fill(visitedFlags, false);
    }

    public boolean isVisited(int coordinate) {
        if (coordinate < 0 || coordinate >= mapSize) return false;
        return visitedFlags[coordinate];
    }
    
    public boolean isEvaluated() {
        return isEvaluated;
    }

    public void setEvaluated(boolean evaluated) {
        this.isEvaluated = evaluated;
    }

    public void markDirty() {
        this.isEvaluated = false;
    }

    public void setFitness(int fitness){
        this.fitness = fitness;
    }

    public int getFitness() {
        return fitness;
    }

    public int[] getDirect() {
        return direct;
    }

    public void setDirect(int[] direct) {
        this.direct = direct;
        this.markDirty();
    }
    
    public void setCalculatedTime(int time) {
        this.calculatedTime = time;
    }
    
    public int getCalculatedTime() {
        return calculatedTime;
    }
    
    public List<Integer> getPath(){
        return pathMap;
    }
}