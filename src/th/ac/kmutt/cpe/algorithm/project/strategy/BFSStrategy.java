package th.ac.kmutt.cpe.algorithm.project.strategy;

import java.util.*;
import th.ac.kmutt.cpe.algorithm.project.graph.*;

public class BFSStrategy implements PathFindingStrategy {
    public PathFindingResult solve(Graph graph) {
        int start = graph.getStartId();
        int goal = graph.getGoalId();
        Map<Integer, Node> nodes = graph.getNodeMap();
        Map<Integer, ArrayList<Edge>> adj = graph.getAdjacencyListMap();
        int n = nodes.size();
        int w = getMaxWeight(adj);
        int maxDist = w*(n-1);
        ArrayList<Integer> dist = new ArrayList<>();
        ArrayList<Integer> prev  = new ArrayList<>();
        ArrayList<Boolean> visited = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            dist.add(Integer.MAX_VALUE);
            prev.add(-1);
            visited.add(false);
        }

        dist.set(start , 0);
        ArrayList<Deque<Integer>> buckets = new ArrayList<>();
        for(int i = 0 ; i < maxDist + 1 ; i++){
            buckets.add(new ArrayDeque<>());
        }



        buckets.get(0).add(start);
        for (int d = 0; d <= maxDist; d++) {

            Deque<Integer> dq = buckets.get(d);
            while (!dq.isEmpty()) {

                int u = dq.pollFirst();
                if (dist.get(u) < d) {
                    continue;
                }

                for (Edge e : adj.get(u)) {
                    int v = e.getTo();
                    int weight = e.getWeight();

                    int nd = dist.get(u) + weight;

                    // ถ้าได้ระยะทางดีกว่า และอยู่ในขอบเขต maxDist
                    if (nd < dist.get(v) && nd <= maxDist) {
                        dist.set(v, nd);
                        prev.set(v, u);
                        buckets.get(nd).addLast(v);
                    }
                }
            }
        }
        return reconstruct(dist, prev, start, goal);
    }

    private int getMaxWeight(Map<Integer, ArrayList<Edge>> adj){
        int max = 0;
        for (int i : adj.keySet()) {
            for (Edge e : adj.get(i)) {
                if (e.getWeight() > max) {
                    max = e.getWeight();
                }
            }
        }
        return max;
    }

    private PathFindingResult reconstruct(
            ArrayList<Integer> dist,
            ArrayList<Integer> prev,
            int startId,
            int goalId) {

        ArrayList<Integer> path = new ArrayList<>();

        // unreachable case
        if (dist.get(goalId) == Integer.MAX_VALUE) {
            return new PathFindingResult(Integer.MAX_VALUE, path);
        }

        int cur = goalId;
        HashSet<Integer> detectLoop = new HashSet<>();

        
        while (cur != -1) {
            if (detectLoop.contains(cur)) {
                return new PathFindingResult(Integer.MAX_VALUE, new ArrayList<>());
            }
            detectLoop.add(cur);

            path.add(cur);
            cur = prev.get(cur);
        }

        Collections.reverse(path);

        return new PathFindingResult(dist.get(goalId), path);
    }
}
