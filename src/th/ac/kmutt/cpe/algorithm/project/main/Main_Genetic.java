package th.ac.kmutt.cpe.algorithm.project.main;

import th.ac.kmutt.cpe.algorithm.project.strategy.GeneticStrategy;

public class Main_Genetic {
    public static void main(String[] args) {
        ReadFile fileReader = new ReadFile();
        int rows = fileReader.getRows();
        int cols = fileReader.getCols();
        int[][] maze = new int[rows][cols];
        maze = fileReader.getGAMap();

        GeneticStrategy result = new GeneticStrategy();
        result.solve(maze);
    }
}
