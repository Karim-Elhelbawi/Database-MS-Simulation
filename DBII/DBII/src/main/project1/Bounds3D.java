package project1;

import java.time.*;
import java.util.*;
import java.io.Serializable;
import java.text.*;

public class Bounds3D implements Serializable {
	public Point minPoint;
	public Point maxPoint;

	public Bounds3D(Point minPoint, Point maxPoint) {
		this.minPoint = minPoint;
		this.maxPoint = maxPoint;
	}


	public boolean containsPoint(Point point) {
		boolean flag=true;
		
		if(point.getX()!=null) {
			flag = flag && compare(point.getX(), minPoint.getX()) >= 0 && compare(point.getX(), maxPoint.getX()) <= 0;
		}
		
		if(point.getY()!=null) {
			flag = flag && compare(point.getY(), minPoint.getY()) >= 0 && compare(point.getY(), maxPoint.getY()) <= 0;
		}
		
		if(point.getZ()!=null) {
			flag = flag && compare(point.getZ(), minPoint.getZ()) >= 0 && compare(point.getZ(), maxPoint.getZ()) <= 0;

		}
		
		return flag;


	}
	

	

	public int compare(Object key1, Object key2) {

		if (key1 instanceof java.lang.Integer) {
			return ((Integer) key1).compareTo((Integer) key2);
		} else if (key1 instanceof java.lang.Double) {
			return ((Double) key1).compareTo((Double) key2);
		} else if (key1 instanceof java.util.Date) {
			return ((Date) key1).compareTo((Date) key2);
		} else {
			return ((key1 + "")).compareToIgnoreCase((key2 + ""));
		}
	}

	public Bounds3D[] split() {


		Object midX = midPoint(minPoint.getX(), maxPoint.getX());
		Object midY = midPoint(minPoint.getY(), maxPoint.getY());
		Object midZ = midPoint(minPoint.getZ(), maxPoint.getZ());
		

		Bounds3D[] bounds = new Bounds3D[8];
		
		bounds[0] = new Bounds3D(minPoint, new Point(midX, midY, midZ));
		bounds[1] = new Bounds3D(new Point(midX, minPoint.getY(), minPoint.getZ()),
				new Point(maxPoint.getX(), midY, midZ));
		bounds[2] = new Bounds3D(new Point(midX, midY, minPoint.getZ()),
				new Point(maxPoint.getX(), maxPoint.getY(), midZ));
		bounds[3] = new Bounds3D(new Point(minPoint.getX(), midY, minPoint.getZ()),
				new Point(midX, maxPoint.getY(), midZ));
		bounds[4] = new Bounds3D(new Point(minPoint.getX(), minPoint.getY(), midZ),
				new Point(midX, midY, maxPoint.getZ()));
		bounds[5] = new Bounds3D(new Point(midX, minPoint.getY(), midZ),
				new Point(maxPoint.getX(), midY, maxPoint.getZ()));
		bounds[6] = new Bounds3D(new Point(midX, midY, midZ), maxPoint);
		bounds[7] = new Bounds3D(new Point(minPoint.getX(), midY, midZ),
				new Point(midX, maxPoint.getY(), maxPoint.getZ()));

		return bounds;
	}

	public static Object midPoint(Object a, Object b) {
		if (a instanceof Integer) {
			return Integer.valueOf((Integer) a + ((Integer) b - (Integer) a) / 2);
		} else if (a instanceof Double) {
			return Double.valueOf((Double) a + ((Double) b - (Double) a) / 2);
		} else if (a instanceof String) {
			
			return printMiddleString((String) a, (String) b);
	
		} else {
			long mid = ((Date) a).getTime() + (((Date) b).getTime() - ((Date) a).getTime()) / 2;
			return new Date(mid);
		} 

	}

	public static String printMiddleString(String S, String T) {
		
	//	S=S.toLowerCase();
	//	T=T.toLowerCase();
		
		String result="";
		int N = S.length() > T.length() ? S.length() : T.length();

		if (S.length() != T.length()) {
			if (S.length() > T.length()) {
				T = T + S.substring(T.length());
			} else {
				S = S + T.substring(S.length());
			}
		}
		// Stores the base 26 digits after addition
		int[] a1 = new int[N + 1];

		for (int i = 0; i < N; i++) {
			a1[i + 1] = (int) S.charAt(i) - 97 + (int) T.charAt(i) - 97;
		}

		// Iterate from right to left
		// and add carry to next position
		for (int i = N; i >= 1; i--) {
			a1[i - 1] += (int) a1[i] / 26;
			a1[i] %= 26;
		}

		// Reduce the number to find the middle
		// string by dividing each position by 2
		for (int i = 0; i <= N; i++) {

			// If current value is odd,
			// carry 26 to the next index value
			if ((a1[i] & 1) != 0) {

				if (i + 1 <= N) {
					a1[i + 1] += 26;
				}
			}

			a1[i] = (int) a1[i] / 2;
		}

		for (int i = 1; i <= N; i++) {
			result+=(char) (a1[i] + 97);
		}
		
		return result;
	}
	
	public String  display() {
		return "Min point is " + minPoint.display() + " : Max point is " + maxPoint.display();
	}
	


	public static void main(String[] args) throws DBAppException, ParseException {

	}

}
