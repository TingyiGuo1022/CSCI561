import java.util.*;
import java.io.*;

// Store information of a point
class Node{
	int year, x, y;
	public Node(int[] point){
		year = point[0];
		x = point[1];
		y = point[2];
	}
	public String getValues(){
		return ""+year+" "+x+" "+y;
	}
	public int Hash_Node(){
		return getValues().hashCode();
	}
	public int cal_Cost(Node start){
		int cost = 0;
		cost += Math.abs(year - start.year);
		int diagonal = Math.min(Math.abs(x- start.x), Math.abs(y-start.y));
		cost += (14 * diagonal);
		cost += (10 * Math.max(Math.abs(x- start.x)-diagonal, Math.abs(y-start.y)-diagonal));
		return cost;
	}
}

// store the cost and location of current node
class Pair{
	int cost;
	Node point;

	public Pair(int c, Node p){
		cost = c;
		point = p;
	}
	public void printPair(){
		System.out.print(point.year+" "+point.x+" "+point.y+" "+cost+"\n");
	}
	public void writePair(FileWriter writer) throws IOException{
		writer.write(point.year+" "+point.x+" "+point.y+" "+cost+"\n");
	}

}

//build a Route construct to store cost and route
class Route{

	Node cur_Node;
	List<Pair> route;
	long total_cost;
	long except = 0;

	public Route(Route prev, Node cur, Pair cur_pair) {
		cur_Node = cur;
		if (prev == null) {
			route = new ArrayList<Pair>();
			total_cost = 0;
		} else {
			route = new ArrayList<Pair>(prev.route);
			total_cost = prev.total_cost + cur_pair.cost;
		}
		route.add(cur_pair);
	}
	public int cal_except(Node target){
		int x_dis = Math.abs(target.x - cur_Node.x), y_dis = Math.abs(target.y - cur_Node.y);
		int dia = Math.min(x_dis, y_dis);
		int stri = Math.max(x_dis-dia, y_dis-dia);
		return Math.abs(target.year-cur_Node.year) + 14*dia + 10*stri;
	}
	public int[] direction(int cur_x, int cur_y, int end_x, int end_y){
		int x = 0, y = 0;
		if(cur_x == end_x){
			x = 0;
		}
		if(cur_x < end_x){
			x = 1;
		}
		if(cur_x > end_x){
			x = -1;
		}
		if(cur_y == end_y){
			y = 0;
		}
		if(cur_y < end_y){
			y = 1;
		}
		if(cur_y > end_y){
			y = -1;
		}
		return new int[]{x, y};
	}
	public List<Pair> build_route(Route cur_ans){
		Node prev = null;
		List<Pair> route =new ArrayList<>();
		for(Pair p : cur_ans.route){
			if(prev == null || p.point.year!= prev.year || p.point.cal_Cost(prev) <=14){
				route.add(p);
				prev = p.point;
			}
			else{
				int cur_x = prev.x, cur_y = prev.y, end_x = p.point.x, end_y = p.point.y;
				int[] dir = direction(cur_x, cur_y, end_x, end_y);
				cur_x += dir[0]; cur_y += dir[1];
				dir = direction(cur_x, cur_y, end_x, end_y);
				while(cur_x != end_x || cur_y != end_y){
					Node cur = new Node(new int[]{prev.year, cur_x, cur_y});
					route.add(new Pair(cur.cal_Cost(prev), cur));
					prev = cur;
					cur_x += dir[0]; cur_y += dir[1];
					dir = direction(cur_x, cur_y, end_x, end_y);
				}
				route.add(new Pair(p.point.cal_Cost(prev), p.point));
				prev = p.point;

			}

		}
		return route;
	}
	public void writeRoute(FileWriter writer) throws IOException{
		writer.write(total_cost+"\n");
		writer.write(route.size()+"\n");
		for(Pair p: route){
			p.writePair(writer);
		}
	}

}

// Rebuild Comparator to compare the priority of different route
class RouteComparator implements Comparator<Route>{
	public int compare(Route a, Route b){
		long a_res = (a.total_cost+a.except);
		long b_res = (b.total_cost+b.except);
		//System.out.println(a_res+" "+a.total_cost+" "+b_res+" "+b.total_cost);
		if(a_res > b_res)
			return 1;
		return 0;
	}
}

public class homework {
	//BFS Algorithm
	public static Route BFS_Algorithm(Node start, Node end, Map<Integer,List<Integer>> channels, List<int[]> direction, int height, int width){
		Route ans = null;
		Queue<Route> q = new LinkedList<>();
		HashSet<Integer> inqueue_point = new HashSet<>();
		q.add(new Route(null,start, new Pair(0, start)));
		while(!q.isEmpty()){
			Route cur_route = q.poll();
			Node cur_node = cur_route.cur_Node;
			if(ans!= null){
				break;
			}
			if(cur_node.Hash_Node() == end.Hash_Node()){
				ans = cur_route;
				break;
			}
			if(channels.get(cur_node.Hash_Node())!=null){
				for(int jump_year: channels.get(cur_node.Hash_Node())){
					Node jump_point = new Node(new int[]{jump_year, cur_node.x, cur_node.y});
					if(!inqueue_point.contains(jump_point.Hash_Node())){
						Route jump_Route =new Route(cur_route, jump_point, new Pair(1, jump_point));
						q.add(jump_Route);
						inqueue_point.add(jump_point.Hash_Node());
						if(jump_point.Hash_Node() == end.Hash_Node())
							ans = jump_Route;
					}

				}
			}
			for(int[] direct: direction){
				int new_x = cur_node.x + direct[0], new_y = cur_node.y + direct[1];
				if(new_x>=0 && new_x<width && new_y>=0 && new_y<height){
					Node next = new Node(new int[]{cur_node.year, new_x, new_y});
					if(!inqueue_point.contains(next.Hash_Node())) {
						Route next_Route = new Route(cur_route, next, new Pair(1, next));
						q.add(next_Route);
						inqueue_point.add(next.Hash_Node());
						if (next.Hash_Node() == end.Hash_Node())
							ans = next_Route;
					}
				}
			}
		}
		return ans;

	}
	// original UCS algorithm
	public static Route UCS_Algorithm(Node start, Node end, Map<Integer,List<Integer>> channels, List<int[]> direction, int height, int width){
		Route ans = null;
		HashSet<Integer> walked_point = new HashSet<>();
		PriorityQueue<Route> pq = new PriorityQueue<>(new RouteComparator());
		pq.add(new Route(null,start, new Pair(0, start)));
		while(!pq.isEmpty()){
			Route cur = pq.poll();
			Node cur_node = cur.cur_Node;
			if(walked_point.contains(cur_node.Hash_Node())){
				continue;
			}
			walked_point.add(cur_node.Hash_Node());
			if(cur_node.Hash_Node() == end.Hash_Node()){
				ans = cur;
				break;
			}
			if(channels.containsKey(cur_node.Hash_Node())){
				for(int next_year: channels.get(cur_node.Hash_Node())){
					Node next = new Node(new int[]{next_year, cur_node.x, cur_node.y});
					Route next_route = new Route(cur, next, new Pair(Math.abs(next.year-cur_node.year), next));
					if(!walked_point.contains(next.Hash_Node())){
						pq.add(next_route);
					}
				}
			}
			for(int[] direct: direction){
				int new_x = cur_node.x + direct[0], new_y = cur_node.y + direct[1];
				if(new_x>=0 && new_x<width && new_y>=0 && new_y<height){
					Node next = new Node(new int[]{cur_node.year, new_x, new_y});
					if(!walked_point.contains(next.Hash_Node())){
						Route next_Route;
						if(direct[0]!=0 && direct[1]!=0){
							next_Route = new Route(cur, next, new Pair(14, next));
						}
						else{
							next_Route = new Route(cur, next, new Pair(10, next));
						}
						pq.add(next_Route);
					}

				}
			}

		}
		return ans;
	}
	//original A* algorithm
	public static Route A_algorithm(Node start, Node end, Map<Integer,List<Integer>> channels, List<int[]> direction, int height, int width){
		Route ans = null;
		HashSet<Integer> walked_point = new HashSet<>();
		PriorityQueue<Route> pq = new PriorityQueue<>(new RouteComparator());
		Route start_route = new Route(null,start, new Pair(0, start));
		start_route.except = start_route.cal_except(end);
		pq.add(start_route);
		while(!pq.isEmpty()){
			Route cur = pq.poll();
			Node cur_node = cur.cur_Node;
			if(walked_point.contains(cur_node.Hash_Node())){
				continue;
			}
			walked_point.add(cur_node.Hash_Node());
//			System.out.println(cur_node.getValues()+"  "+ cur.total_cost+"  "+cur.except+" "+(cur.total_cost+cur.except));
			if(cur_node.Hash_Node() == end.Hash_Node()){
				ans = cur;
				break;
			}
			if(channels.containsKey(cur_node.Hash_Node())){
				for(int next_year: channels.get(cur_node.Hash_Node())){
					Node next = new Node(new int[]{next_year, cur_node.x, cur_node.y});
					if(!walked_point.contains(next.Hash_Node())){
						Route next_route = new Route(cur, next, new Pair(Math.abs(next.year-cur_node.year), next));
						next_route.except = next_route.cal_except(end);
						pq.add(next_route);
					}
				}
			}
			for(int[] direct: direction){
				int new_x = cur_node.x + direct[0], new_y = cur_node.y + direct[1];
				if(new_x>=0 && new_x<width && new_y>=0 && new_y<height){
					Node next = new Node(new int[]{cur_node.year, new_x, new_y});
					if(!walked_point.contains(next.Hash_Node())){
						Route next_Route;
						if(direct[0]!=0 && direct[1]!=0){
							next_Route = new Route(cur, next, new Pair(14, next));
						}
						else{
							next_Route = new Route(cur, next, new Pair(10, next));
						}
						next_Route.except = next_Route.cal_except(end);
						pq.add(next_Route);
					}
				}
			}
		}
		return ans;
	}
	// the improvement of UCS
	public static Route UCS_improve(Node start, Node end, Map<Integer,List<Integer>> channels, Map<Integer,List<int[]>> channel_location, List<int[]> direction, int height, int width){
		Route ans = null;
		HashSet<Integer> walked_point = new HashSet<>();
		PriorityQueue<Route> pq = new PriorityQueue<>(new RouteComparator());
		pq.add(new Route(null,start, new Pair(0, start)));
		while(!pq.isEmpty()){
			Route cur = pq.poll();
			Node cur_node = cur.cur_Node;
			if(walked_point.contains(cur_node.Hash_Node())){
				continue;
			}
			walked_point.add(cur_node.Hash_Node());
			if(cur_node.Hash_Node() == end.Hash_Node()){
				ans = cur;
				break;
			}
			if(cur_node.year == end.year){
				pq.add(new Route(cur, end, new Pair(end.cal_Cost(cur_node), end)));
				continue;
			}
			if(channels.containsKey(cur_node.Hash_Node())){
				for(int next_year: channels.get(cur_node.Hash_Node())){
					Node next = new Node(new int[]{next_year, cur_node.x, cur_node.y});
					Route next_route = new Route(cur, next, new Pair(Math.abs(next.year-cur_node.year), next));
					if(!walked_point.contains(next.Hash_Node())){
						pq.add(next_route);
					}
				}
			}
			if(!channel_location.containsKey(cur_node.year)){
				continue;
			}
			if(channel_location.get(cur_node.year).size()> 1000){
				for(int[] direct: direction){
					int new_x = cur_node.x + direct[0], new_y = cur_node.y + direct[1];
					if(new_x>=0 && new_x<width && new_y>=0 && new_y<height){
						Node next = new Node(new int[]{cur_node.year, new_x, new_y});
						if(!walked_point.contains(next.Hash_Node())){
							Route next_Route;
							if(direct[0]!=0 && direct[1]!=0){
								next_Route = new Route(cur, next, new Pair(14, next));
							}
							else{
								next_Route = new Route(cur, next, new Pair(10, next));
							}
							pq.add(next_Route);
						}

					}
				}
			}
			else{
				for(int[] location: channel_location.get(cur_node.year)){
					Node next = new Node(new int[]{cur_node.year, location[0], location[1]});
					Route next_route = new Route(cur, next, new Pair(next.cal_Cost(cur_node), next));
					if(!walked_point.contains(next.Hash_Node())){
						pq.add(next_route);
					}
				}
			}

		}
		return ans;
	}
	// better A* algorithm
	public static Route A_alrorithm_improve(Node start, Node end, Map<Integer,List<Integer>> channels, Map<Integer,List<int[]>> channel_location, List<int[]> direction, int height, int width){
		Route ans = null;
		HashSet<Integer> walked_point = new HashSet<>();
		PriorityQueue<Route> pq = new PriorityQueue<>(new RouteComparator());
		Route start_route = new Route(null,start, new Pair(0, start));
		start_route.except = start_route.cal_except(end);
		pq.add(start_route);
		while(!pq.isEmpty()){
			Route cur = pq.poll();
			Node cur_node = cur.cur_Node;
			if(walked_point.contains(cur_node.Hash_Node())){
				continue;
			}
			walked_point.add(cur_node.Hash_Node());
			//System.out.println(cur_node.getValues()+"  "+ cur.total_cost+"  "+cur.except+" "+(cur.total_cost+cur.except));
			if(cur_node.Hash_Node() == end.Hash_Node()){
				ans = cur;
				break;
			}
			if(cur_node.year == end.year){
				pq.add(new Route(cur, end, new Pair(end.cal_Cost(cur_node), end)));
				continue;
			}
			if(channels.containsKey(cur_node.Hash_Node())){
				for(int next_year: channels.get(cur_node.Hash_Node())){
					Node next = new Node(new int[]{next_year, cur_node.x, cur_node.y});
					if(!walked_point.contains(next.Hash_Node())){
						Route next_route = new Route(cur, next, new Pair(Math.abs(next.year-cur_node.year), next));
						next_route.except = next_route.cal_except(end);
						pq.add(next_route);
					}
				}
			}
			if(!channel_location.containsKey(cur_node.year)){
				continue;
			}
			if(channel_location.get(cur_node.year).size()> 1000){
				for(int[] direct: direction){
					int new_x = cur_node.x + direct[0], new_y = cur_node.y + direct[1];
					if(new_x>=0 && new_x<width && new_y>=0 && new_y<height){
						Node next = new Node(new int[]{cur_node.year, new_x, new_y});
						if(!walked_point.contains(next.Hash_Node())){
							Route next_Route;
							if(direct[0]!=0 && direct[1]!=0){
								next_Route = new Route(cur, next, new Pair(14, next));
							}
							else{
								next_Route = new Route(cur, next, new Pair(10, next));
							}
							next_Route.except = next_Route.cal_except(end);
							pq.add(next_Route);
						}
					}
				}
			}
			else{
				for(int[] location: channel_location.get(cur_node.year)){
					Node next = new Node(new int[]{cur_node.year, location[0], location[1]});
					if(!walked_point.contains(next.Hash_Node())){
						Route next_route = new Route(cur, next, new Pair(next.cal_Cost(cur_node), next));
						next_route.except = next_route.cal_except(end);
						pq.add(next_route);
					}

				}
			}

		}
		return ans;
	}

	//main
	public static void main(String[] args) throws IOException{
		// write your code here
		//read input file
		long startime= System.currentTimeMillis();

		FileInputStream input = new FileInputStream("input.txt");
		BufferedReader Reader = new BufferedReader(new InputStreamReader(input));

		//setup basic information
		String method_name = Reader.readLine();
		int[] grid_szie = Arrays.stream(Reader.readLine().split("\\s+")).mapToInt(Integer::parseInt).toArray();
		int width = grid_szie[0], height = grid_szie[1];
		int[] start_point = Arrays.stream(Reader.readLine().split("\\s+")).mapToInt(Integer::parseInt).toArray();
		Node start = new Node(start_point);
		int[] end_point = Arrays.stream(Reader.readLine().split("\\s+")).mapToInt(Integer::parseInt).toArray();
		Node end = new Node(end_point);
		Map<Integer, List<Integer>> channels = new HashMap<>();
		Map<Integer,List<int[]>> channel_location = new HashMap<>();
		int channel_count = Integer.parseInt(Reader.readLine());
		String line = null;
		for (int i = 0; i<channel_count;i++){
			line = Reader.readLine();
			int[] channel_inf = Arrays.stream(line.split("\\s+")).mapToInt(Integer::parseInt).toArray();
			if(channel_inf[1]<0 || channel_inf[1]>=width)
				continue;
			if(channel_inf[2]<0 || channel_inf[2]>=height)
				continue;
			Node point = new Node(Arrays.copyOfRange(channel_inf, 0, channel_inf.length-1));
			Node point_1 = new Node(new int[]{channel_inf[3], channel_inf[1], channel_inf[2]});
			int hash_point = point.Hash_Node();
			if(!channels.containsKey(hash_point)){
				channels.put(hash_point, new ArrayList<Integer>());
			}
			if(!channel_location.containsKey(point.year)){
				channel_location.put(point.year, new ArrayList<int[]>());
			}
			int hash_point_1 = point_1.Hash_Node();
			if(!channels.containsKey(hash_point_1)){
				channels.put(hash_point_1, new ArrayList<Integer>());
			}
			if(!channel_location.containsKey(point_1.year)){
				channel_location.put(point_1.year, new ArrayList<int[]>());
			}
			//System.out.println(point.getValues()+" "+point_1.getValues()+" "+channel_inf[0]);
			channels.get(hash_point).add(channel_inf[channel_inf.length-1]);
			channel_location.get(point.year).add(new int[]{point.x, point.y});
			channels.get(hash_point_1).add(channel_inf[0]);
			channel_location.get(point_1.year).add(new int[]{point_1.x, point_1.y});

		}
		//setup movement directions
		List<int[]> direction = new ArrayList<>();
		for(int i = -1; i<=1; i++){
			for( int j = -1; j<=1; j++){
				if(i != 0 || j != 0)
					direction.add(new int[]{i,j});
			}
		}
		Route ans = null;
		if(start.x>=0 && start.x<width && start.y >=0 && start.y<height && end.x>=0 && end.x<width && end.y >=0 && end.y<height){
			if(method_name.equals("BFS")){
				ans = BFS_Algorithm(start, end, channels, direction, height, width);
			}
			if(method_name.equals("UCS_original")){
				ans = UCS_Algorithm(start, end, channels, direction, height, width);
			}
			if(method_name.equals("A*_original")){
				ans = A_algorithm(start, end, channels, direction, height, width);
			}
			if(method_name.equals("UCS")){
				ans = UCS_improve(start, end, channels,channel_location, direction, height, width);
				if(ans != null)
					ans.route = ans.build_route(ans);
			}
			if(method_name.equals("A*")){
				ans = A_alrorithm_improve(start, end, channels,channel_location, direction, height, width);
				if(ans!= null)
					ans.route = ans.build_route(ans);
			}
		}

		String output = "output.txt";
		FileWriter writer = new FileWriter(output);
		if(ans == null){
			writer.write("FAIL");
		}
		else{
			ans.writeRoute(writer);
		}
		writer.flush();
		writer.close();
		long endtime=System.currentTimeMillis();
		System.out.println("run times: "+(endtime-startime)+" ms");

	}
}
