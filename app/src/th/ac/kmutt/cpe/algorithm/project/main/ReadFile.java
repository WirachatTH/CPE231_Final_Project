package th.ac.kmutt.cpe.algorithm.project.main;
import java.util.*;

public class ReadFile {
    private ArrayList<ArrayList<Integer>> input;

    public void readFile(){
        Scanner sc = new Scanner(System.in);
        ArrayList<ArrayList<Integer>> maze = new ArrayList<>();
        while(sc.hasNextLine()) {
            String temp = sc.nextLine().trim();
            ArrayList<Integer> row = new ArrayList<>();

            for (int j = 0; j < temp.length(); j++) {
                if(temp.charAt(j)  == '"'){
                    continue;
                }

                if(temp.charAt(j)  == '#'){
                    row.add(-1);
                }else if (temp.charAt(j)  == 'S') {
                    row.add(Integer.MIN_VALUE);
                }else if (temp.charAt(j)  == 'G') {
                    row.add(Integer.MAX_VALUE);
                }else if (Character.isDigit(temp.charAt(j))) {
                    StringBuilder num = new StringBuilder(); //สร้างไว้กันเคสเลข 2,3 หลัก
                    num.append(temp.charAt(j));

                    //เช็คว่าตัวต่อไปเป็นเลขไหม ถ้าเป็นก็เพิ่มไปใน num
                    while (j + 1 < temp.length() && Character.isDigit(temp.charAt(j + 1))) {
                        num.append(temp.charAt(++j));
                    }

                    int value = Integer.parseInt(num.toString());
                    row.add(value);
                }
            }
            maze.add(row);
        } 

        sc.close();
        input = maze;
    }

    public ArrayList<ArrayList<Integer>> getInput(){
        return input;
    }
}
