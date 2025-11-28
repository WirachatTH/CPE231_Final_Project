package th.ac.kmutt.cpe.algorithm.project.strategy;

import java.util.ArrayList;
import java.util.Map;
import th.ac.kmutt.cpe.algorithm.project.graph.*;


public class PathFindingResult {
    private int totalCost;
    private ArrayList<Integer> path;
    private String RESET    = "\u001B[0m";
    private String GREEN = "\u001B[42m"; // พื้นหลังเขียว
    private String YELLOW = "\u001B[43m";
    public PathFindingResult(int totalCost, ArrayList<Integer> path) {
        this.totalCost = totalCost;
        this.path = path;
    }

    public int getTotalCost() {
        return totalCost;
    }

    public ArrayList<Integer> getPath() {
        return path;
    }

    public void visualizePath(ArrayList<ArrayList<Integer>> maze, Graph graph, PathFindingResult result) {
        int rows = maze.size();
        if (rows == 0) return;
        int cols = maze.get(0).size();

        // 1) mark ว่า cell ไหนอยู่บน path
        boolean[][] onPath = new boolean[rows][cols];

        Map<Integer, Node> nodeMap = graph.getNodeMap();
        java.util.List<Integer> pathList = result.getPath();

        for (int id : pathList) {
            Node node = nodeMap.get(id);
            if (node == null) continue;

            int r = node.getRow();
            int c = node.getCol();
            if (r < 0 || r >= rows || c < 0 || c >= cols) continue;

            onPath[r][c] = true;
        }

        // 2) พิมพ์ maze โดยแสดงค่าจริง
        System.out.println("\n=== Path Visualization ===");
        for (int r = 0; r < rows; r++) {
            StringBuilder line = new StringBuilder();

            for (int c = 0; c < cols; c++) {
                int val = maze.get(r).get(c);
                String cellText;
                // อย่าแตะถ้าไม่จำเป็นขอร้อง
                // แปลงค่าใน array -> ข้อความที่จะแสดง
                if (val == -1) {
                    cellText = "### ";  // กำแพง
                } else if (val == Integer.MIN_VALUE) {
                    cellText = " S  ";  // Start
                } else if (val == Integer.MAX_VALUE) {
                    cellText = " G  ";  // Goal
                } else {
                    // แสดงค่าจริงของ cell (ปรับให้กว้าง 3 ตัวอักษร + 1 space)
                    if (val < 10) {
                        cellText = " " + val + "  ";  // เลข 1 หลัก: " 5  "
                    } else {
                        cellText = val + "  ";         // เลข 2 หลัก: "10  "
                    }
                }

                // ใส่สีถ้าอยู่บน path
                if (onPath[r][c]) {
                    if (val == Integer.MIN_VALUE || val == Integer.MAX_VALUE) {
                        // S และ G ใช้สีเหลือง
                        line.append(YELLOW).append(cellText).append(RESET);
                    } else {
                        // path ปกติใช้สีเขียว
                        line.append(GREEN).append(cellText).append(RESET);
                    }
                } else {
                    line.append(cellText);
                }
            }

            System.out.println(line.toString());
        }
    }
}

