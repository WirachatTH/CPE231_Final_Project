package th.ac.kmutt.cpe.algorithm.project.main;

import java.util.*;
import th.ac.kmutt.cpe.algorithm.project.graph.*;
import th.ac.kmutt.cpe.algorithm.project.strategy.*;

public class Main_Dijkstra {
    public static void main(String[] args) {
        /* read pls
         * -1 ใน Array หมายถึง wall
         * Integer.MIN_VALUE ใน Array หมายถึง start
         * Integer.MAX_VALUE ใน Array หมายถึง End
         * วิธีใช้เปิด Command Prompt
         * javac  -d bin -sourcepath src .\src\th\ac\kmutt\cpe\algorithm\project\main\*
         * javac -d bin -sourcepath src .\src\th\ac\kmutt\cpe\algorithm\project\graph\*
         * javac -d bin -sourcepath src .\src\th\ac\kmutt\cpe\algorithm\project\strategy\*
         * ไปที่ folder input แล้ว คลิกขวาที่ filetxt สักอันแล้วก็อปปี้ path
         * java -cp bin th.ac.kmutt.cpe.algorithm.project.main.Main_Dijkstra < แปะfilepath
         */

        // Part1: รับ input ล้วนๆ
        ArrayList<ArrayList<Integer>> maze = new ArrayList<>();
        ReadFile fileReader  = new ReadFile();
        maze = fileReader.getInput();

        // for (int i = 0; i < maze.size(); i++) {
        //     System.out.println(maze.get(i));
        // }




        //Part2: สร้าง Graph เน้น ๆ กูอยากตาย
        Graph graph = new Graph(maze);
        // graph.printGraph();





        //Part3: ยัดเข้า Strategy ต่าง ๆ
        PathFindingStrategy shortestPath = new DijkstraStrategy();
        PathFindingResult result = shortestPath.solve(graph);
        // System.out.println(result.getPath());
        result.visualizePath(maze, graph, result);
        System.out.println("Total "+result.getTotalCost());
    }
}
