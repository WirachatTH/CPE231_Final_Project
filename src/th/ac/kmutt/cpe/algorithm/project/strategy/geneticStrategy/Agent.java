package th.ac.kmutt.cpe.algorithm.project.strategy.geneticStrategy;

import java.util.*;

public class Agent {
    private int[] direct;
    private int fitness;
    public List<Integer> pathMap;  //พิกัด(col 3ตัวแรกที่เหลือคือ row) index= ก้าวที่เดิน

    // public Agent(int[] direct){
    //     this.direct = direct;
    //     pathMap = new HashMap<>();
    //     pathMap.put(0, 001001); //เริ่มต้นที่จุดเริ่มต้นเสมอ
    // }
    public Agent(int directSize){
        direct = new int[directSize];
        pathMap = new ArrayList<>();
        pathMap.add(0, 1001); //เริ่มต้นที่จุดเริ่มต้นเสมอ
    }

    public void setPath(int stepIndex, int coordinate){
        pathMap.add(stepIndex, coordinate);
    }

    public void setFitness(int fitness){
        this.fitness = fitness;
    }

    public List<Integer> getPath(){
        return pathMap;
    }

    public int getFitness() {
        return fitness;
    }

    public int[] getDirect() {
        return direct;
    }

    public void setDirect(int[] direct) {
        this.direct = direct;
    }
}