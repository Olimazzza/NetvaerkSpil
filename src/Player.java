import java.io.Serializable;
import java.util.List;

public class Player implements Serializable {
	String name;
	int xpos;
	int ypos;
	int point;
	String direction;

	public Player(String name, int xpos, int ypos, String direction) {
		this.name = name;
		this.xpos = xpos;
		this.ypos = ypos;
		this.direction = direction;
		this.point = 0;
	}

	public String getName() {
		return name;
	}

	public int getXpos() {
		return xpos;
	}
	public void setXpos(int xpos) {
		this.xpos = xpos;
	}
	public int getYpos() {
		return ypos;
	}
	public void setYpos(int ypos) {
		this.ypos = ypos;
	}
	public String getDirection() {
		return direction;
	}
	public void setDirection(String direction) {
		this.direction = direction;
	}
	public void addPoints(int p) {
		point+=p;
	}

	public static int[] spawnRandomLocation(String[] map, List<Player> players) {
		int xpos;
		int ypos;
		do {
			xpos = (int) (Math.random() * 20);
			ypos = (int) (Math.random() * 20);
		} while (map[xpos].charAt(ypos) == 'w' && isAPlayerAtThisLocation(players, xpos, ypos));
		return new int[] {xpos, ypos};
	}

	private static boolean isAPlayerAtThisLocation(List<Player> players, int xpos, int ypos) {
		for (Player p : players) {
			if (p.getXpos() == xpos && p.getYpos() == ypos) {
				return true;
			}
		}
		return false;
	}

	public String toString() {
		return name+":   "+point;
	}
}