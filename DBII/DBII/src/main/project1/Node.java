package project1;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.*;

public class Node implements Serializable {
	public int MAX_ENTRIES;
	public Vector<Entry> entries;
	public Node[] children;
	public Bounds3D bounds;
	public boolean isLeaf;

	public Node(Bounds3D bounds) throws DBAppException {
		this.bounds = bounds;
		MAX_ENTRIES = getMaxEntries();
		entries = new Vector<Entry>();
		children = null;
		isLeaf = true;
	}

	public Node searchPointNode(Point point) {
		if (!bounds.containsPoint(point)) {
			return null;
		}

		if (children != null) {
			for (Node child : children) {
				Node matchingNode = child.searchPointNode(point);
				if (matchingNode != null) {
					return matchingNode;
				}
			}
		} else {
			return this;
		}

		return null;
	}

	public List<Entry> searchPoint(Point point) {
		List<Entry> matchingEntries = new ArrayList<>();

		if (!bounds.containsPoint(point)) {
			return matchingEntries;
		}

		if (children != null) {
			for (Node child : children) {
				List<Entry> childMatchingEntries = child.searchPoint(point);
				matchingEntries.addAll(childMatchingEntries);
			}
		} else {
			for (Entry entry : entries) {
				if (entry.colsValues.equals(point)) {
					matchingEntries.add(entry);
				}
			}
		}

		return matchingEntries;
	}

	public boolean pageChange(Entry entry, Vector<Entry> entries) {
		boolean pageChange = false;

		for (int i = 0; i < entries.size(); i++) {
			Entry curr = entries.get(i);
			if (curr.clusteringKey.equals(entry.clusteringKey)) {
				entries.get(i).pagePath=entry.pagePath;
				pageChange = true;
				break;
			}
		}

		if (!pageChange) {
		outerLoop:	for (int i = 0; i < entries.size(); i++) {
				Entry curr = entries.get(i);
				for (int j = 0; j < curr.duplicatesPaths.size(); j++) {
					Duplicates currDup = curr.duplicatesPaths.get(j);
					if (currDup.clusteringKey.equals(entry.clusteringKey)) {
						currDup.pagePaths=entry.pagePath;
						pageChange = true;
						break outerLoop;
					}
				}
			}
			
		}
		
		return pageChange;
	}

	public void insert(Entry entry) throws DBAppException {
		if (!bounds.containsPoint(entry.colsValues)) {
			return;
		}
		if (children != null) {
			for (Node child : children) {
				if (!entry.entered)
					child.insert(entry);
				else
					break;
			}
		} else {
			entry.entered = true;

			boolean isDuplicate = false;
			boolean pageChange = pageChange( entry, entries);

	

			if (!pageChange) {
				for (int i = 0; i < entries.size(); i++) {
					Entry curr = entries.get(i);
					if (curr.colsValues.equals(entry.colsValues) && entry.clusteringKey != curr.clusteringKey) {
						entries.get(i).duplicatesPaths.add(new Duplicates(entry.clusteringKey, entry.pagePath));
						isDuplicate = true;
						break;
					}
				}
			

			if (!isDuplicate) {
				entries.add(entry);
				if (entries.size() > MAX_ENTRIES) {
					split();
				}

			}
			}

		}
	}

	public void delete(Entry entry) throws DBAppException {
		if (!bounds.containsPoint(entry.colsValues)) {
			return;
		}
		if (children != null) {
			for (Node child : children) {
				child.delete(entry);
			}
		} else {

			for (int i = 0; i < entries.size(); i++) {
				Entry curr = entries.get(i);
				if (curr.colsValues.equals(entry.colsValues) && curr.clusteringKey.equals(entry.clusteringKey)) {
					entries.remove(entries.get(i));
					if (curr.hasDuplicates()) {
						Entry newEntry = new Entry(curr.duplicatesPaths.get(0).clusteringKey, curr.colsValues,
								curr.duplicatesPaths.get(0).pagePaths);
						curr.duplicatesPaths.remove(0);
						newEntry.duplicatesPaths = curr.duplicatesPaths;
						insert(newEntry);
					}
					return;
				}
				
				else if(curr.colsValues.equals(entry.colsValues) && !curr.clusteringKey.equals(entry.clusteringKey)) {
					for(int j=0; j<curr.duplicatesPaths.size(); j++) {
						Duplicates dup = curr.duplicatesPaths.get(i);
						if(dup.clusteringKey.equals(entry.clusteringKey)) {
							curr.duplicatesPaths.remove(j);
							return;

						}
					}
				}
				
			}
			// entry = null;
		}
	}

//	public void update(Entry oldEntry, Entry newEntry) throws DBAppException {
//		if (!bounds.containsPoint(oldEntry.colsValues)) {
//			return;
//		}
//		if (children != null) {
//			for (Node child : children) {
//				child.update(oldEntry, newEntry);
//			}
//		} else {
//			for (int i = 0; i < entries.size(); i++) {
//				Entry currentEntry = entries.get(i);
//				if (currentEntry.colsValues.equals(oldEntry.colsValues)) {
//					delete(oldEntry);
//					insert(newEntry);
//					break;
//				}
//			}
//		}
//	}

	public void split() throws DBAppException {
		children = new Node[8];
		Bounds3D[] childrenBounds = bounds.split();

		children[0] = new Node(childrenBounds[0]);
		children[1] = new Node(childrenBounds[1]);
		children[2] = new Node(childrenBounds[2]);
		children[3] = new Node(childrenBounds[3]);
		children[4] = new Node(childrenBounds[4]);
		children[5] = new Node(childrenBounds[5]);
		children[6] = new Node(childrenBounds[6]);
		children[7] = new Node(childrenBounds[7]);

		for (Entry entry : entries) {
			for (Node child : children) {
				if (child.bounds.containsPoint(entry.colsValues)) {
					child.entries.add(entry);
					break;
				}
			}
		}
		entries.clear();

		this.isLeaf = false;
	}

//    public List<Entry> queryRange(Bounds range) {
//        List<Entry> result = new ArrayList<>();
//        if (!bounds.intersects(range)) {
//            return result;
//        }
//        for (Entry entry : entries) {
//            if (range.contains(entry)) {
//                result.add(entry);
//            }
//        }
//        if (children != null) {
//            for (Node child : children) {
//                result.addAll(child.queryRange(range));
//            }
//        }
//        return result;
//    }
//    
	public static int getMaxEntries() throws DBAppException {

		try {
			Properties prop = new Properties();
			InputStream input = null;

			input = new FileInputStream("src\\main\\resources\\DBApp.config");

			// load a properties file
			prop.load(input);

			return Integer.parseInt(prop.getProperty("MaximumEntriesinOctreeNode"));
		} catch (Exception e) {
			throw new DBAppException(e.getMessage());
		}

	}

}
