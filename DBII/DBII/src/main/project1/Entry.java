package project1;

import java.io.Serializable;
import java.util.*;

public class Entry implements Serializable {

	public Object clusteringKey;
	public Point colsValues;
	public String pagePath;
	public boolean entered;
	public Vector<Duplicates> duplicatesPaths;

	public Entry(Object clusteringKey, Point colsValues, String pagePath) {
		this.clusteringKey = clusteringKey;
		this.colsValues = colsValues;
		this.pagePath = pagePath;
		this.entered = false;
		this.duplicatesPaths = new Vector<Duplicates>();

	}

	public boolean hasDuplicates() {
		if (duplicatesPaths.size() > 0)
			return true;
		else
			return false;
	}

}
