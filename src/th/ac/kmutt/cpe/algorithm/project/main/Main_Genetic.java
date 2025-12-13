package th.ac.kmutt.cpe.algorithm.project.main;

import java.util.List;

import th.ac.kmutt.cpe.algorithm.project.strategy.PathFindingResult;
import th.ac.kmutt.cpe.algorithm.project.strategy.geneticStrategy.GeneticStrategy;

public class Main_Genetic {
    public static void main(String[] args) {
        ReadFile fileReader = new ReadFile();
        int rows = fileReader.getRows();
        int cols = fileReader.getCols();
        int[][] maze = new int[rows][cols];
        maze = fileReader.getGAMap();

        GeneticStrategy solver = new GeneticStrategy(maze);
        List<Integer> coordinate = solver.getCoordinate();
        int totalTime = solver.findFinalTime();

        PathFindingResult result = new PathFindingResult();
        result.visualizePath(maze, coordinate);
        System.out.println("Total " + totalTime);
    }
}

