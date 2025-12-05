package th.ac.kmutt.cpe.algorithm.project.strategy;

import java.util.*;

public class GeneticStrategy {
    private int[][] map;
    private int rows, cols;
    private int startRow, startCol;
    private int goalRow, goalCol;

    public void solve(int[][] map){
        this.map = map;
        this.rows = map.length;
        this.cols = map[0].length;
        this.startRow = 1;
        this.startCol = 1;
        this.goalRow = rows-2;
        this.goalCol = cols-2;


        int[][] population = createInitial();
        this.printPopulation(population);
    }

    public int[][] createInitial(){
        int popSize = 5;
        int[][] population = new int[popSize][];
        for(int i = 0; i < popSize; i++){
            population[i] = randomPath();
        }

        return population;
    }

    public int[] randomPath(){
        // ให้ 0=down, 1=right, 2=up, 3=left
        int sizePath = rows*cols/5; //กรณีแย่สุดคือต้องเดินทุกช่อง แต่เป็นไปไม่ได้เลยหารออก เพื่อประหยัด memory
        int[] path = new int[sizePath];

        int[] currentNode = {startRow, startCol};
        int[] delta = {1, 1, -1, -1};
        Random rand = new Random();
        List<Integer> randomList = new ArrayList<>();

        for(int i = 0; i < sizePath; i++){
            randomList.clear();
            for(int j = 0; j < 4; j++){
                int[] nextNode = currentNode.clone();

                nextNode[j%2] += delta[j];
                if(map[nextNode[0]][nextNode[1]] != -1){
                    randomList.add(j);
                }
            }
            // System.out.println("i"+i+": "+randomList);
            if(i != 0){
                Integer opposite = (path[i-1]+2)%4;
                if(randomList.contains(opposite) && (randomList.size() > 1)){
                    randomList.remove(opposite);
                }
            }
            
            int index = rand.nextInt(randomList.size());
            path[i] = randomList.get(index);
            currentNode[path[i]%2] += delta[path[i]];
        }
        // System.out.println("--------");
        return path;
    }

    public void getFitness(int[] path){
        
    }

    public void crossover(){

    }

    public void mutate(){

    }

    public void printPopulation(int[][] pop){
        for(int i = 0; i < pop.length; i++){
            for(int j = 0; j < pop[0].length; j++){
                System.out.print(pop[i][j]+" ");
            }
            System.out.println();
        }
    }
}
