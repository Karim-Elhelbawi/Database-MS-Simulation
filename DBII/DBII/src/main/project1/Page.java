package project1;
import java.io.*;
import java.util.*;


//added a vector of objects for all the primary key values that a page contains so i can binary search easily
//removed the tuple class logic, records are now stored as hashtables


public class Page implements Serializable{
	
	public Vector<Hashtable<String, Object>> records;
	public int numOfRecords;
	public String ID;
	public Vector<Object> primaryKeys;
	public Object minKey;
	public Object maxKey;
	 
	public Page(String ID) {
		this.ID=ID;
		this.records = new Vector<Hashtable<String, Object>>();
		this.numOfRecords = 0;
		this.primaryKeys = new Vector<Object>();

				
	}
	
	public void increaseRecords() {
		numOfRecords++;
	}
	
	public void decreaseRecords() {
		numOfRecords--;
	}
	
	public void addRecord(Hashtable<String, Object> newRec) {
		records.add(newRec);
	}
	
	public void addKey(Object key) {
		primaryKeys.add(key);
	}
	
	public void removeKey(Object key) {
		primaryKeys.remove(key);
	}
	
	 
	
}
