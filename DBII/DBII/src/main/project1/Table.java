package project1;

import java.io.*;
import java.util.*;

public class Table implements Serializable {

	public String TableName;
	public String ClusteringKey;
	public Vector<String> pagePaths;
	public Vector<String> indexPaths;
	public int numOfPages;
	public int pageID;
	public boolean hasIndex;

	public Table(String TableName, String ClusteringKey) {
		this.TableName = TableName;
		this.ClusteringKey = ClusteringKey;
		this.pagePaths = new Vector<String>();
		this.indexPaths = new Vector<String>();
		this.numOfPages = 0;
		this.pageID = 0;
		this.hasIndex = false;

	}

	public void deleteFromOtherIndices(Hashtable<String, Object> rowToDelete, String indexUsedPath)
			throws DBAppException {
		for (int i = 0; i < indexPaths.size(); i++) {

			if (!indexPaths.get(i).equals(indexUsedPath)) {
				Octree tree = (Octree) deserializeObject(indexPaths.get(i));
				Point colsValues = new Point(rowToDelete.get(tree.indexColumns.get("x")),
						rowToDelete.get(tree.indexColumns.get("y")), rowToDelete.get(tree.indexColumns.get("z")));
				Entry deleteEntry = new Entry(rowToDelete.get(ClusteringKey), colsValues, "");
				tree.delete(deleteEntry);
				serializeObject(tree, indexPaths.get(i));

			}

		}
	}

	public void updateOnOtherIndices(Hashtable<String, Object> rowToUpdate, String indexUsedPath, String pagePath,
			Object ClusteringKey) throws DBAppException {
		for (int i = 0; i < indexPaths.size(); i++) {

			if (!indexPaths.get(i).equals(indexUsedPath)) {
				Octree tree = (Octree) deserializeObject(indexPaths.get(i));
				Point colsValues = new Point(rowToUpdate.get(tree.indexColumns.get("x")),
						rowToUpdate.get(tree.indexColumns.get("y")), rowToUpdate.get(tree.indexColumns.get("z")));
				Entry updateEntry = new Entry(ClusteringKey, colsValues, pagePath);
				tree.insert(updateEntry);
				serializeObject(tree, indexPaths.get(i));


			}

		}
	}

	public void deleteAllTrees() throws DBAppException {
		for (int i = 0; i < indexPaths.size(); i++) {
			
			Octree tree = (Octree) deserializeObject(indexPaths.get(i));
			Octree newTree = new Octree(tree.root.bounds, tree.indexColumns, tree.ID);
			tree=null;
			delete(indexPaths.get(i));
			serializeObject(newTree, indexPaths.get(i));


		}
		
		System.gc();

	}
	
	public  void serializeObject(Object o, String path) throws DBAppException {

		try {
			FileOutputStream fileOut = new FileOutputStream(path);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(o);
			out.close();
			fileOut.close();
		} catch (Exception e) {
			throw new DBAppException();
		}
	}

	public static Object deserializeObject(String path) throws DBAppException {

		try {
			FileInputStream fileIn = new FileInputStream(path);
			ObjectInputStream objectIn = new ObjectInputStream(fileIn);
			Object o = objectIn.readObject();
			objectIn.close();
			fileIn.close();
			return o;
		} catch (Exception e) {
			throw new DBAppException();
		}
	}
	
	public void delete(String path) {
		File f = new File(path);
		f.delete();
	}

	public void increasePage() {
		numOfPages++;
	}

	public void decreasePage() {
		numOfPages--;
	}

	public void increaseID() {
		pageID++;
	}

}
