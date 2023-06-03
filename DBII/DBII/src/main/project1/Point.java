package project1;

import java.io.Serializable;
import java.util.*;


public class Point implements Serializable {
	public HashMap<String, Object> point;


	public Point(Object x, Object y, Object z) {
		point = new HashMap<String, Object>();
		point.put("x", x);
		point.put("y", y);
		point.put("z", z);

	}

	public String  display() {
		return "(" + point.get("x").toString() + ", " + point.get("y").toString() + ", " + point.get("z").toString() + ")";
	}

	public Object getX() {
		Object value = point.get("x");
		return value;
	}
	
	public Object getY() {
		Object value = point.get("y");
		return value;
	}
	
	public Object getZ() {
		Object value = point.get("z");
		return value;
	}
	

	public boolean equals(Point other) {
		boolean flag =true;
		
		if(other.getX()!=(null))
			flag = flag && getX().equals(other.getX());
		
		
		if(other.getY()!=(null))
			flag = flag && getY().equals(other.getY());

		
		if(other.getZ()!=(null))
			flag = flag && getZ().equals(other.getZ());
		
		
		 return flag;
	}
	
	
	
	
}
