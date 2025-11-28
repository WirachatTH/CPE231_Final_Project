package th.ac.kmutt.cpe.algorithm.project.graph;
import java.util.*;

public class Graph {
    //Map ที่เก็บ Node
    private Map<Integer, Node> nodes = new HashMap<>();
    //Map ที่เก็บ adjacencylist (เพื่อนบ้าน)
    private Map<Integer, ArrayList<Edge>> adj = new HashMap<>();
    private int startId = -1;
    private int goalId = -1;
    private int rows, cols;

    public Graph(ArrayList<ArrayList<Integer>> grid) {
        this.rows = grid.size();
        this.cols = grid.get(0).size();
        buildGraph(grid);
    }

    public Map<Integer, Node> getNodeMap(){
        return nodes;
    }

    public Map<Integer, ArrayList<Edge>> getAdjacencyListMap(){
        return adj;
    }

    public int getStartId(){
        return startId;
    }

    public int getGoalId(){
        return goalId;
    }

    
    private void buildGraph(ArrayList<ArrayList<Integer>> grid) {
        ArrayList<ArrayList<Integer>> idMap = new ArrayList<>();

        for (int r = 0; r < rows; r++) {
            ArrayList<Integer> list = new ArrayList<>();
            for (int c = 0; c < cols; c++) {
                list.add(-1);
            }
            idMap.add(list);
        }

        //idCounter คือ nodeName
        int idCounter = 0;

        // create nodes
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {

                int val = grid.get(r).get(c);

                if (val == -1) continue; 

                boolean isStart = (val == Integer.MIN_VALUE); 
                boolean isGoal  = (val == Integer.MAX_VALUE);

                idMap.get(r).set(c, idCounter);

                Node node = new Node(idCounter, r, c, isStart, isGoal);
                nodes.put(idCounter, node);
                adj.put(idCounter, new ArrayList<>());

                if (isStart) startId = idCounter;
                if (isGoal) goalId = idCounter;

                idCounter++;
            }
        }

        ArrayList<ArrayList<Integer>> dirs = new ArrayList<>();
        ArrayList<Integer> down  = new ArrayList<>();
        down.add(1);  
        down.add(0);
        dirs.add(down);

        ArrayList<Integer> up    = new ArrayList<>();
        up.add(-1);   
        up.add(0);
        dirs.add(up);

        ArrayList<Integer> right = new ArrayList<>();
        right.add(0); 
        right.add(1);
        dirs.add(right);

        ArrayList<Integer> left  = new ArrayList<>();
        left.add(0); 
        left.add(-1);
        dirs.add(left);

        // create edges
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int fromId = idMap.get(r).get(c);
                if (fromId == -1) continue;

                for (ArrayList<Integer> d : dirs) {
                    int nr = r + d.get(0);  // dx
                    int nc = c + d.get(1);  // dy

                    if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue;

                    int toId = idMap.get(nr).get(nc);
                    if (toId == -1) continue;

                    int toVal = grid.get(nr).get(nc);
                    int weight;
    
                    if (toVal == Integer.MIN_VALUE || toVal == Integer.MAX_VALUE) {
                        weight = 0;
                    } else {
                        weight = toVal;
                    }

                    // ตรวจสอบว่ามี edge อยู่แล้วหรือไม่
                    boolean exists = false;
                    for (Edge e : adj.get(fromId)) {
                        if (e.getTo() == toId) {
                            exists = true;
                            break;
                        }
                    }
    
                    if (!exists) {
                        adj.get(fromId).add(new Edge(toId, weight));
                    }
                }
            }
        }
    }

    //print graph chatgen มา 
    public void printGraph() {
        for (int id : adj.keySet()) {
            Node node = nodes.get(id);

            System.out.print("Node " + id +
                " (" + node.getRow() + "," + node.getCol() + ")" +
                (node.isStart() ? " [START]" : "") +
                (node.isGoal() ? " [GOAL]" : "") +
                " -> ");

            for (Edge e : adj.get(id)) {
                System.out.print("[" + e.getTo() + ", w=" + e.getWeight() + "] ");
            }
            System.out.println();
        }
    }
}
