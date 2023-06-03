package project1;

import java.io.Serializable;

public class Duplicates implements Serializable {
	
	public Object clusteringKey;
	public String pagePaths;
	
	public Duplicates(Object clusteringKey, String pagePaths){
		this.clusteringKey=clusteringKey;
		this.pagePaths=pagePaths;

	}

	

}
