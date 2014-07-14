package beat.collector;

import java.util.Random;

public class IdCreator {
	
	public static Random random = new Random();
	
	public static int createId(){
		return Math.abs(random.nextInt());
	}
}
