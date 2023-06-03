package project1;

import java.util.*;
import java.util.stream.Collectors;
import java.lang.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class DBApp {

	public DBApp() {
		this.init();
	}

	public void init() {
		try {
			File tables = new File("Tables");
			tables.mkdirs();

//			FileWriter writer = new FileWriter("metadata.csv");
//			String[] columnNames = { "Table Name", "Column Name", "Column Type", "Clustering Key", "Index Name",
//					"Index Type", "Min", "Max" };
//
//			// Write the column names as the first row
//			for (String columnName : columnNames) {
//				writer.append(columnName);
//				writer.append(",");
//			}
//			writer.append("\n");
//
//			writer.flush();
//			writer.close();
		} catch (Exception e) {

		}
	}

	public void createTable(String strTableName, String strClusteringKeyColumn,
			Hashtable<String, String> htblColNameType, Hashtable<String, String> htblColNameMin,
			Hashtable<String, String> htblColNameMax) throws DBAppException {

		try {
			// input validation
			for (String col : htblColNameType.values()) {
				if (!col.equals("java.lang.Integer") && !col.equals("java.lang.String")
						&& !col.equals("java.lang.Double") && !col.equals("java.util.Date"))
					throw new DBAppException();
			}

			for (String col : htblColNameType.keySet()) {
				if (!htblColNameMin.containsKey(col) || !htblColNameMax.containsKey(col))
					throw new DBAppException();
			}

			for (String col : htblColNameMin.keySet()) {
				if (!htblColNameType.containsKey(col) || !htblColNameMax.containsKey(col))
					throw new DBAppException();
			}

			for (String col : htblColNameMax.keySet()) {
				if (!htblColNameType.containsKey(col) || !htblColNameMin.containsKey(col))
					throw new DBAppException();
			}

			// reads from the metadata csv to check if the table already exists or not
			Vector<String> values = new Vector<>();
			String line;
			String[] headers = null;
			int columnIdx = 0; // TODO: ??????
			FileReader fr = new FileReader("metadata.csv");
			BufferedReader br = new BufferedReader(fr);

			while ((line = br.readLine()) != null) {
				String[] row = line.split(",");

				if (headers == null) {
					headers = row;

					for (int i = 0; i < headers.length; i++) {
						if (headers[i].equals("Table Name")) {
							columnIdx = i;
							break;
						}
					}
				} else {
					values.add(row[columnIdx]);
				}
			}

			if (values.contains(strTableName))
				throw new DBAppException();

			// creating the table instance and adding it to the table folder, serializing it
			// too
			Table newTable = new Table(strTableName, strClusteringKeyColumn);
			String path = "Tables/" + strTableName + ".ser";

			serializeObject(newTable, path);

			// adding to the metadata
			FileWriter writer = new FileWriter("metadata.csv", true);

			for (String key : htblColNameType.keySet()) {
				writer.append(strTableName);
				writer.append(",");
				writer.append(key);
				writer.append(",");
				writer.append(htblColNameType.get(key));
				writer.append(",");
				writer.append(key.toLowerCase().equals(strClusteringKeyColumn.toLowerCase()) + "");
				writer.append(",");
				writer.append("null");
				writer.append(",");
				writer.append("null");
				writer.append(",");
				writer.append(htblColNameMin.get(key));
				writer.append(",");
				writer.append(htblColNameMax.get(key));
				writer.append("\n");

			}

			writer.flush();
			writer.close();

			File forPages = new File(strTableName);
			forPages.mkdirs();
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBAppException();
		}

	}

	// this method is for validating that the record matches the table's data types
	// and number of
	// attributes
	public boolean validate(String strTableName, Hashtable<String, Object> htblColNameValue) throws DBAppException {

		try {
			Hashtable<String, Object> maxHash = getMax(strTableName);
			Hashtable<String, Object> minHash = getMin(strTableName);
			Hashtable<String, String> typeHash = getType(strTableName);
			Set<String> columnNames = htblColNameValue.keySet();

			for (String name : columnNames) {
				if (!typeHash.containsKey(name)) {
					return false;
				}
				Object currentMax = maxHash.get(name);
				Object currentMin = minHash.get(name);
				String className = typeHash.get(name);
				Object currentVal = htblColNameValue.get(name);
				String currentType = "class " + className;
				String valType = currentVal.getClass() + "";
				if (!valType.equals(currentType)) {
					return false;
				}
				if (compare(currentVal, currentMin) < 0 || compare(currentVal, currentMax) > 0) {
					return false;
				}

			}
			return true;

		} catch (Exception e) {
			e.printStackTrace();
			throw new DBAppException();
		}
	}

	public void insertIntoTable(String strTableName, Hashtable<String, Object> htblColNameValue) throws DBAppException {

		// validating that the record matches the table's data types and number of
		// attributes

		try {
			boolean flag = validate(strTableName, htblColNameValue);
			if (!flag)
				throw new DBAppException();

			Table tableObj = (Table) deserializeObject("Tables/" + strTableName + ".ser");

			// check to see if record contains the clustering key or not

			Set<String> columnNames = htblColNameValue.keySet();
			ArrayList<String> tableColumns = getColumns(strTableName);

			// Convert column names to lowercase
			List<String> lowercaseColumnNames = new ArrayList<>();
			for (String columnName : columnNames) {
				lowercaseColumnNames.add(columnName.toLowerCase());
			}

			// Convert table columns to lowercase
			List<String> lowercaseTableColumns = new ArrayList<>();
			for (String tableColumn : tableColumns) {
				lowercaseTableColumns.add(tableColumn.toLowerCase());
			}

			// Check if the lists are equal
			boolean haveSameValues = true;
			if (lowercaseColumnNames.size() != lowercaseTableColumns.size()) {
				haveSameValues = false;
			} else {
				for (int i = 0; i < lowercaseColumnNames.size(); i++) {
					String columnName = lowercaseColumnNames.get(i);
					if (!lowercaseTableColumns.contains(columnName)) {
						haveSameValues = false;
						break;
					}
				}
			}

			if (!haveSameValues)
				throw new DBAppException();

			// case where the table has no record yet

			if (tableObj.numOfPages == 0) {
				// System.out.println("first record to ever be inserted");
				tableObj.increaseID();
				Page pageObj = new Page(tableObj.TableName + "" + (tableObj.pageID));
				tableObj.pagePaths.add(strTableName + "/" + pageObj.ID + ".ser");
				tableObj.increasePage();

				pageObj.addRecord(htblColNameValue);
				pageObj.increaseRecords();

				pageObj.addKey(htblColNameValue.get(tableObj.ClusteringKey));

				pageObj.maxKey = htblColNameValue.get(tableObj.ClusteringKey);
				pageObj.minKey = htblColNameValue.get(tableObj.ClusteringKey);

				if (tableObj.hasIndex) {
					insertEntryIntoIndex(strTableName + "/" + pageObj.ID + ".ser", tableObj, htblColNameValue);
				}

				serializeObject(pageObj, strTableName + "/" + pageObj.ID + ".ser");
				serializeObject(tableObj, "Tables/" + strTableName + ".ser");

				return;

			}

			else {

				// checks to see if a duplicate primary key exists
				for (int i = 0; i < tableObj.pagePaths.size(); i++) {
					Page pageObj = (Page) deserializeObject(tableObj.pagePaths.get(i));
					if (pageObj.primaryKeys.contains(htblColNameValue.get(tableObj.ClusteringKey))) {
						serializeObject(pageObj, tableObj.pagePaths.get(i));
						serializeObject(tableObj, "Tables/" + strTableName + ".ser");
						throw new DBAppException();
					}

					serializeObject(pageObj, tableObj.pagePaths.get(i));
				}
			}

			int pageIdx = whichPageIdx(tableObj, htblColNameValue);

			if (pageIdx > 0) {
				pageIdx = pageIdx - 1;
			}

			// System.out.println(pageIdx);

			Page pageObj = (Page) deserializeObject(tableObj.pagePaths.elementAt(pageIdx));

			if (pageObj.numOfRecords < getMaxRows()) {

				// System.out.println("page is NOT full so there is a spot for
				// "+htblColNameValue.get(tableObj.ClusteringKey));

				insertIntoPage(tableObj, pageObj, htblColNameValue);

				pageObj.increaseRecords();

				if (tableObj.hasIndex) {
					insertEntryIntoIndex(tableObj.pagePaths.elementAt(pageIdx), tableObj, htblColNameValue);
				}

				serializeObject(pageObj, tableObj.pagePaths.elementAt(pageIdx));
				serializeObject(tableObj, "Tables/" + strTableName + ".ser");
				return;
			}

			// page is full
			else if (pageObj.numOfRecords == getMaxRows()) {

				// System.out.println("page is full and current clustering key is
				// "+htblColNameValue.get(tableObj.ClusteringKey));

				boolean greaterThanAllKeys = greaterThanAllKeys(pageObj.primaryKeys,
						htblColNameValue.get(tableObj.ClusteringKey));

				if (!greaterThanAllKeys) {
					// System.out.println(htblColNameValue.get(tableObj.ClusteringKey)+ " has a spot
					// in this full page");
					Hashtable<String, Object> lastElement = pageObj.records.remove(getMaxRows() - 1);
					pageObj.primaryKeys.remove(lastElement.get(tableObj.ClusteringKey));

					insertIntoPage(tableObj, pageObj, htblColNameValue);

					if (tableObj.hasIndex) {
						insertEntryIntoIndex(tableObj.pagePaths.elementAt(pageIdx), tableObj, htblColNameValue);
					}
					serializeObject(pageObj, tableObj.pagePaths.elementAt(pageIdx));
					insertIntoNextPage(tableObj, pageIdx + 1, lastElement);
					serializeObject(tableObj, "Tables/" + strTableName + ".ser");
					return;

				}

				else if (greaterThanAllKeys) {

					// System.out.println("has no spot in this page");

					serializeObject(pageObj, tableObj.pagePaths.elementAt(pageIdx));
					insertIntoNextPage(tableObj, pageIdx + 1, htblColNameValue);
					serializeObject(tableObj, "Tables/" + strTableName + ".ser");
					return;

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBAppException();
		}

	}

	public void insertIntoPage(Table tableObj, Page pageObj, Hashtable<String, Object> htblColNameValue)
			throws DBAppException {
		try {
			// Set up the binary search range
			int low = 0;
			int high = pageObj.records.size() - 1;
			Comparator<Hashtable<String, Object>> comparator = (o1, o2) -> compare(o1.get(tableObj.ClusteringKey),
					o2.get(tableObj.ClusteringKey));

			// Perform the binary search
			int index = Collections.binarySearch(pageObj.records.subList(low, high + 1), htblColNameValue, comparator);

			// Determine the insertion position based on the binary search result
			int insertIndex = (index >= 0) ? index : -(index + 1);
			if (insertIndex == 0) {
				pageObj.minKey = htblColNameValue.get(tableObj.ClusteringKey);
			}

			// Insert the record at the determined position

			pageObj.records.add(insertIndex, htblColNameValue);
			// System.out.println("Record number " + insertIndex + " in page number " +
			// pageObj.ID);

			// Update the maxKey if necessary
			if (insertIndex == pageObj.records.size() - 1) {
				pageObj.maxKey = htblColNameValue.get(tableObj.ClusteringKey);
			}

			// Add the clustering key to the page's key set
			pageObj.addKey(htblColNameValue.get(tableObj.ClusteringKey));
		} catch (Exception e) {
			throw new DBAppException();
		}
	}

	public void insertIntoNextPage(Table tableObj, int nextPageIdx, Hashtable<String, Object> htblColNameValue)
			throws DBAppException {

		try {

			if (nextPageIdx == tableObj.numOfPages) {
				// System.out.println("i am here creating a new
				// page"+htblColNameValue.get(tableObj.ClusteringKey));
				tableObj.increaseID();
				Page extraPage = new Page(tableObj.TableName + "" + (tableObj.pageID));
				tableObj.pagePaths.add(tableObj.TableName + "/" + extraPage.ID + ".ser");
				tableObj.increasePage();

				extraPage.addRecord(htblColNameValue);
				extraPage.increaseRecords();
				extraPage.addKey(htblColNameValue.get(tableObj.ClusteringKey));
				extraPage.minKey = htblColNameValue.get(tableObj.ClusteringKey);
				extraPage.maxKey = htblColNameValue.get(tableObj.ClusteringKey);

				if (tableObj.hasIndex) {
					insertEntryIntoIndex(tableObj.TableName + "/" + extraPage.ID + ".ser", tableObj, htblColNameValue);
				}

				serializeObject(extraPage, tableObj.TableName + "/" + extraPage.ID + ".ser");

			}

			else {

				Page pageObj = (Page) deserializeObject(tableObj.pagePaths.elementAt(nextPageIdx));
				// System.out.println("i am here"+htblColNameValue.get(tableObj.ClusteringKey));

				if (pageObj.numOfRecords == getMaxRows()) {

					// System.out.println("i am here with
					// "+htblColNameValue.get(tableObj.ClusteringKey)+" in a full page");

					Hashtable<String, Object> lastElement = pageObj.records.remove(getMaxRows() - 1);
					pageObj.primaryKeys.remove(lastElement.get(tableObj.ClusteringKey));

					pageObj.records.add(0, htblColNameValue);
					pageObj.primaryKeys.add(htblColNameValue.get(tableObj.ClusteringKey));
					pageObj.minKey = htblColNameValue.get(tableObj.ClusteringKey);
					if (tableObj.hasIndex) {
						insertEntryIntoIndex(tableObj.TableName + "/" + pageObj.ID + ".ser", tableObj,
								htblColNameValue);
					}
					serializeObject(pageObj, tableObj.TableName + "/" + pageObj.ID + ".ser");
					insertIntoNextPage(tableObj, nextPageIdx + 1, lastElement);
				}

				else {
					// System.out.println("i am here with
					// "+htblColNameValue.get(tableObj.ClusteringKey)+" in a less than full page");

					pageObj.records.add(0, htblColNameValue);
					pageObj.primaryKeys.add(htblColNameValue.get(tableObj.ClusteringKey));
					pageObj.minKey = htblColNameValue.get(tableObj.ClusteringKey);
					pageObj.increaseRecords();
					if (tableObj.hasIndex) {
						insertEntryIntoIndex(tableObj.TableName + "/" + pageObj.ID + ".ser", tableObj,
								htblColNameValue);
					}
					serializeObject(pageObj, tableObj.TableName + "/" + pageObj.ID + ".ser");
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new DBAppException();
		}
	}

	public void insertEntryIntoIndex(String path, Table tableObj, Hashtable<String, Object> htblColNameValue)
			throws DBAppException {

		if (tableObj.hasIndex) {
			for (String indexPath : tableObj.indexPaths) {
				Octree tree = (Octree) deserializeObject(indexPath);
				Point point = new Point(htblColNameValue.get(tree.indexColumns.get("x")),
						htblColNameValue.get(tree.indexColumns.get("y")),
						htblColNameValue.get(tree.indexColumns.get("z")));
				Entry newEntry = new Entry(htblColNameValue.get(tableObj.ClusteringKey), point, path);
				tree.insert(newEntry);
				serializeObject(tree, indexPath);

			}
		}
	}

	public void deleteFromTable(String strTableName, Hashtable<String, Object> htblColNameValue) throws DBAppException {

		try {

			Table tableObj = (Table) deserializeObject("Tables/" + strTableName + ".ser");

			// first case: the htblColNameValue hashtable is empty so we just delete all the
			// pages of a table

			if (htblColNameValue.isEmpty()) {

				for (int i = 0; i < tableObj.pagePaths.size(); i++) {
					Page pageObj = (Page) deserializeObject(tableObj.pagePaths.get(i));
					pageObj = null;
					delete(tableObj.pagePaths.get(i));

				}

				System.gc();

				tableObj.deleteAllTrees();

				Table newTable = new Table(tableObj.TableName, tableObj.ClusteringKey);
				// tableObj.numOfPages = 0;
				newTable.indexPaths = tableObj.indexPaths;
				newTable.hasIndex = tableObj.hasIndex;
				delete("Tables/" + strTableName + ".ser");
				serializeObject(newTable, "Tables/" + strTableName + ".ser");

				return;
			}

			// checks to see if all columns we reference in the delete are existing columns
			// in the table or not
			ArrayList<String> tableColumns = getColumns(strTableName);
			Set<String> keyNames = htblColNameValue.keySet();

			for (String key : keyNames) {
				if (!(tableColumns.contains(key))) {
					serializeObject(tableObj, "Tables/" + strTableName + ".ser");
					throw new DBAppException();
					// return;
				}
			}
			// checks if datatypes are correct
			Hashtable<String, String> type = getType(strTableName);
			for (String key : keyNames) {
				String currentType = "class " + type.get(key);
				String typeToCheck = htblColNameValue.get(key).getClass() + "";
				if (!typeToCheck.equals(currentType)) {

					serializeObject(tableObj, "Tables/" + strTableName + ".ser");
					throw new DBAppException();
				}
			}
			// checks for max/min violations
			Hashtable<String, Object> maxHash = getMax(strTableName);
			Hashtable<String, Object> minHash = getMin(strTableName);
			// displayHash(maxHash);
			// displayHash(minHash);
			for (String key : keyNames) {
				Object currentVal = htblColNameValue.get(key);
				Object currentMax = maxHash.get(key);
				Object currentMin = minHash.get(key);

				if (compare(currentVal, currentMin) < 0 || compare(currentVal, currentMax) > 0) {
					serializeObject(tableObj, "Tables/" + strTableName + ".ser");
					throw new DBAppException();
				}
			}

			// start actual deletion of record by record process

			// check first if the table has an index built on it

			if (tableObj.hasIndex) {

				boolean usedIndex = deleteusingIndex(tableObj, htblColNameValue);
				if (usedIndex) {
					return;
				}

			}

			for (int i = 0; i < tableObj.pagePaths.size(); i++) {
				Page pageObj = (Page) deserializeObject(tableObj.pagePaths.get(i));
				deleteFromPage(tableObj, pageObj, htblColNameValue, tableObj.pagePaths.get(i));
				// serializeObject(pageObj, tableObj.pagePaths.get(i));

			}

			serializeObject(tableObj, "Tables/" + strTableName + ".ser");
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBAppException();
		}

	}

	public boolean deleteusingIndex(Table tableObj, Hashtable<String, Object> htblColNameValue) throws DBAppException {

		boolean usedIndex = false;

		Set<String> keyNames = htblColNameValue.keySet();
		Octree myIndex = null;
		String octreePath = "";

		outerloop: for (String indexPath : tableObj.indexPaths) {
			Octree tree = (Octree) deserializeObject(indexPath);
			for (String key : keyNames) {
				if (tree.indexColumns.contains(key)) {
					myIndex = tree;
					octreePath = indexPath;
					break outerloop;
				}
			}
			serializeObject(tree, indexPath);
		}

		if (myIndex != null) {

			usedIndex = true;

			String x = null;
			String y = null;
			String z = null;

			for (String key : keyNames) {
				if (myIndex.indexColumns.containsValue(key)) {
					String whichDimension = getKeyByValue(myIndex.indexColumns, key);
					switch (whichDimension) {
					case "x":
						x = key;
						break;
					case "y":
						y = key;
						break;
					case "z":
						z = key;
						break;
					default:
						break;
					}
				}
			}

			Object xDim = null;
			if (x != null)
				xDim = htblColNameValue.get(x);

			Object yDim = null;
			if (y != null)
				yDim = htblColNameValue.get(y);

			Object zDim = null;
			if (z != null)
				zDim = htblColNameValue.get(z);

			Point toSearchFor = new Point(xDim, yDim, zDim);

			List<Entry> entriesToDelete = myIndex.search(toSearchFor);
			ArrayList<Duplicates> dupsToRemove = new ArrayList<Duplicates>();
			ArrayList<Entry> removeFromIndex = new ArrayList<Entry>();
			boolean wasItDeleted = false;

			for (int i = 0; i < entriesToDelete.size(); i++) {

				Page pageObj = (Page) deserializeObject(entriesToDelete.get(i).pagePath);
				int rowNum = binarySearchPage(pageObj, tableObj, entriesToDelete.get(i).clusteringKey);
				wasItDeleted = deleteFromPageUsingIndex(tableObj, pageObj, rowNum, htblColNameValue,
						entriesToDelete.get(i).pagePath, octreePath);
				if (wasItDeleted)
					removeFromIndex.add(entriesToDelete.get(i));
				// serializeObject(pageObj, entriesToDelete.get(i).pagePath);

				for (int j = 0; j < entriesToDelete.get(i).duplicatesPaths.size(); j++) {

					Page page = (Page) deserializeObject(entriesToDelete.get(i).duplicatesPaths.get(j).pagePaths);
					int rowIndex = binarySearchPage(page, tableObj,
							entriesToDelete.get(i).duplicatesPaths.get(j).clusteringKey);
					boolean wasDupDeleted = deleteFromPageUsingIndex(tableObj, page, rowIndex, htblColNameValue,
							entriesToDelete.get(i).pagePath, octreePath);
					if (wasItDeleted) {
						if (wasDupDeleted) {
							wasItDeleted = true;
							removeFromIndex.add(new Entry(entriesToDelete.get(i).duplicatesPaths.get(j).clusteringKey,
									entriesToDelete.get(i).colsValues,
									entriesToDelete.get(i).duplicatesPaths.get(j).pagePaths));
						} else
							wasItDeleted = false;

					} else {
						if (wasDupDeleted) {
							dupsToRemove.add(entriesToDelete.get(i).duplicatesPaths.get(j));
						}
					}

					// serializeObject(page,
					// entriesToDelete.get(i).duplicatesPaths.get(j).pagePaths);
				}

				for (int j = 0; j < removeFromIndex.size(); j++) {
					myIndex.delete(removeFromIndex.get(j));
				}

				entriesToDelete.get(i).duplicatesPaths.removeAll(dupsToRemove);
				dupsToRemove = new ArrayList<Duplicates>();
				removeFromIndex = new ArrayList<Entry>();

			}

			serializeObject(myIndex, octreePath);
			serializeObject(tableObj, "Tables/" + tableObj.TableName + ".ser");

		}

		return usedIndex;
	}

	public boolean deleteFromPageUsingIndex(Table tableObj, Page pageObj, int i,
			Hashtable<String, Object> htblColNameValue, String pagePath, String indexUsedPath) throws DBAppException {

		Hashtable<String, Object> rowToDelete = pageObj.records.get(i);
		boolean sameValues = sharesValues(rowToDelete, htblColNameValue);
		if (sameValues) {
			pageObj.records.remove(i);
			pageObj.removeKey(rowToDelete.get(tableObj.ClusteringKey));
			pageObj.decreaseRecords();

			tableObj.deleteFromOtherIndices(rowToDelete, indexUsedPath);
		}

		if (pageObj.records.size() == 0) {
			pageObj = null;
			tableObj.pagePaths.remove(pagePath);
			delete(pagePath);
			tableObj.decreasePage();
		} else {
			updateMaxMin(tableObj.ClusteringKey, pageObj);
		}

		if (pageObj != null) {
			serializeObject(pageObj, pagePath);
		}

		return sameValues;

	}

	public void deleteFromPage(Table tableObj, Page pageObj, Hashtable<String, Object> htblColNameValue,
			String pagePath) throws DBAppException {

		Vector<Hashtable<String, Object>> recordsToBeDeleted = new Vector<Hashtable<String, Object>>();
		boolean recordsGotDeleted = false;

		for (int j = 0; j < pageObj.records.size(); j++) {
			Hashtable<String, Object> currRecord = pageObj.records.get(j);

			// columns that are in the deletion context
			boolean sameValues = sharesValues(currRecord, htblColNameValue);
			if (sameValues) {
				pageObj.removeKey(currRecord.get(tableObj.ClusteringKey));
				pageObj.decreaseRecords();
				recordsToBeDeleted.add(currRecord);
				recordsGotDeleted = true;
				tableObj.deleteFromOtherIndices(currRecord, "noIndexWasUsedInDeletion");

			}
		}

		if (recordsGotDeleted) {
			pageObj.records.removeAll(recordsToBeDeleted);
			if (pageObj.records.size() == 0) {
				pageObj = null;
				tableObj.pagePaths.remove(pagePath);
				delete(pagePath);
				tableObj.decreasePage();
			} else {
				updateMaxMin(tableObj.ClusteringKey, pageObj);
			}
		}

		if (pageObj != null)
			serializeObject(pageObj, pagePath);

	}

	public void updateTable(String strTableName, String strClusteringKeyValue,
			Hashtable<String, Object> htblColNameValue) throws DBAppException {

		try {
			Table tableObj = (Table) deserializeObject("Tables/" + strTableName + ".ser");

			// checks to see if all columns we reference in the update are existing columns
			// in the table or not
			ArrayList<String> tableColumns = getColumns(strTableName);
			Set<String> keyNames = htblColNameValue.keySet();
			// System.out.println(tableColumns);
			// System.out.println(keyNames);

			for (String key : keyNames) {
				if (!(tableColumns.contains(key))) {
					serializeObject(tableObj, "Tables/" + strTableName + ".ser");

					throw new DBAppException();
				}
			}
			// checks if datatypes are correct
			Hashtable<String, String> type = getType(strTableName);
			for (String key : keyNames) {
				if (type.containsKey(key)) {
					String currentType = "class " + type.get(key);
					String typeToCheck = htblColNameValue.get(key).getClass() + "";
					if (!typeToCheck.equals(currentType)) {

						serializeObject(tableObj, "Tables/" + strTableName + ".ser");

						throw new DBAppException();
					}
				}
			}
			// checks for min/max violations
			Hashtable<String, Object> maxHash = getMax(strTableName);
			Hashtable<String, Object> minHash = getMin(strTableName);
			for (String key : keyNames) {
				if (!getColumns(strTableName).contains(key)) {
					serializeObject(tableObj, "Tables/" + strTableName + ".ser");
					throw new DBAppException();
				} else {
					// System.out.println(key);
					Object currentVal = htblColNameValue.get(key);
					Object currentMax = maxHash.get(key);
					Object currentMin = minHash.get(key);
					// System.out.println(currentMax.getClass()+"");

					if (compare(currentVal, currentMin) < 0 || compare(currentVal, currentMax) > 0) {
						serializeObject(tableObj, "Tables/" + strTableName + ".ser");
						throw new DBAppException();
					}
				}
			}

			// throw an exception if i'm trying to update the clustering key
			if (keyNames.contains(tableObj.ClusteringKey)) {
				throw new DBAppException();
			}

			// find the record to update

			Object clusteringKey = parseStringToObject(strClusteringKeyValue);

			if (tableObj.hasIndex) {
				boolean usedIndex = updateUsingIndex(tableObj, strClusteringKeyValue, htblColNameValue);
				if (usedIndex) {
					return;
				}
			}

			// Hashtable<String, Object> recordToBeUpdated = new Hashtable<String,
			// Object>();
			Set<String> keys = htblColNameValue.keySet();

			outerloop: for (int i = 0; i < tableObj.pagePaths.size(); i++) {
				Page pageObj = (Page) deserializeObject(tableObj.pagePaths.get(i));
				if (pageObj.primaryKeys.contains(clusteringKey)) {
					for (Hashtable<String, Object> hashtable : pageObj.records) {
						if (hashtable.get(tableObj.ClusteringKey).equals(clusteringKey)) {
							// recordToBeUpdated = hashtable;
							tableObj.deleteFromOtherIndices(hashtable, "noIndexWasUsed");
							for (String key : keys) {
								Object value = htblColNameValue.get(key);
								hashtable.put(key, value);
							}
							tableObj.updateOnOtherIndices(hashtable, "noIndexWasUsed", tableObj.pagePaths.get(i),
									clusteringKey);
							serializeObject(pageObj, tableObj.pagePaths.get(i));
							break outerloop;
						}
					}
				}

				serializeObject(pageObj, tableObj.pagePaths.get(i));
			}

			serializeObject(tableObj, "Tables/" + strTableName + ".ser");
		} catch (Exception e) {
			e.printStackTrace();
			throw new DBAppException();
		}
	}

	public boolean updateUsingIndex(Table tableObj, String strClusteringKeyValue,
			Hashtable<String, Object> htblColNameValue) throws DBAppException {

		boolean usedIndex = false;

		Octree myIndex = null;
		String octreePath = "";

		for (String indexPath : tableObj.indexPaths) {
			Octree tree = (Octree) deserializeObject(indexPath);
			if (tree.indexColumns.contains(tableObj.ClusteringKey)) {
				myIndex = tree;
				octreePath = indexPath;
				break;

			}
			serializeObject(tree, indexPath);
		}

		if (myIndex != null) {

			usedIndex = true;

			Object clusteringKey = parseStringToObject(strClusteringKeyValue);

			String x = null;
			String y = null;
			String z = null;

			String whichDimension = getKeyByValue(myIndex.indexColumns, tableObj.ClusteringKey);
			switch (whichDimension) {
			case "x":
				x = tableObj.ClusteringKey;
				break;
			case "y":
				y = tableObj.ClusteringKey;
				break;
			case "z":
				z = tableObj.ClusteringKey;
				break;
			default:
				break;
			}

			Object xDim = null;
			if (x != null)
				xDim = clusteringKey;

			Object yDim = null;
			if (y != null)
				yDim = clusteringKey;

			Object zDim = null;
			if (z != null)
				zDim = clusteringKey;

			Point toSearchFor = new Point(xDim, yDim, zDim);
			List<Entry> entryToUpdate = myIndex.search(toSearchFor);

			Set<String> keys = htblColNameValue.keySet();

			outerloop: for (int i = 0; i < entryToUpdate.size(); i++) {
				String entryPath = entryToUpdate.get(i).pagePath;
				Page pageObj = (Page) deserializeObject(entryPath);
				for (Hashtable<String, Object> hashtable : pageObj.records) {
					if (hashtable.get(tableObj.ClusteringKey).equals(clusteringKey)) {
						tableObj.deleteFromOtherIndices(hashtable, octreePath);
						myIndex.delete(entryToUpdate.get(i));
						for (String key : keys) {
							Object value = htblColNameValue.get(key);
							hashtable.put(key, value);
						}
						Point newPoint = new Point(hashtable.get(myIndex.indexColumns.get("x")),
								hashtable.get(myIndex.indexColumns.get("y")),
								hashtable.get(myIndex.indexColumns.get("z")));
						myIndex.insert(new Entry(clusteringKey, newPoint, entryPath));
						tableObj.updateOnOtherIndices(hashtable, octreePath, entryPath, clusteringKey);
						serializeObject(pageObj, entryToUpdate.get(i).pagePath);
						break outerloop;
					}

				}

				serializeObject(pageObj, entryToUpdate.get(i).pagePath);

			}

			serializeObject(myIndex, octreePath);
			serializeObject(tableObj, "Tables/" + tableObj.TableName + ".ser");

		}

		return usedIndex;

	}

	public String getKeyByValue(Hashtable<String, String> hashtable, String value) {
		for (String key : hashtable.keySet()) {
			if (hashtable.get(key).equals(value)) {
				return key;
			}
		}
		return null; // Value not found
	}

	public int binarySearchPage(Page pageObj, Table tableObj, Object key) {

		int low = 0;
		int high = pageObj.records.size() - 1;

		while (low <= high) {
			int mid = (low + high) / 2;

			Hashtable<String, Object> record = pageObj.records.get(mid);
			Object recordKey = record.get(tableObj.ClusteringKey); // Replace "key" with the actual key field name

			if (recordKey.equals(key)) {
				return mid; // Key found, return the index
			} else if (compare(key, recordKey) < 0) {
				high = mid - 1; // Key is smaller, search in the lower half
			} else {
				low = mid + 1; // Key is larger, search in the upper half
			}
		}

		return -1; // Key not found
	}

	public int whichPageIdx(Table tableObj, Hashtable<String, Object> htblColNameValue) throws DBAppException {
		try {
			Vector<Object> pageMin = new Vector<Object>();
			Object key = htblColNameValue.get(tableObj.ClusteringKey);

			for (int i = 0; i < tableObj.pagePaths.size(); i++) {
				Page pageObj = (Page) deserializeObject(tableObj.pagePaths.get(i));
				pageMin.add(pageObj.minKey);
				serializeObject(pageObj, tableObj.pagePaths.get(i));
			}

			int low = 0;
			int high = pageMin.size() - 1;

			while (low <= high) {
				int mid = (low + high) / 2;
				Object midObj = pageMin.get(mid);
				int cmp = compare(key, midObj);
				if (cmp < 0) {
					high = mid - 1;
				} else if (cmp > 0) {
					low = mid + 1;
				} else {
					// if newObj has the same value as midObj, insert it after midObj
					return mid + 1;
				}
			}

			return low;
		} catch (Exception e) {
			throw new DBAppException();
		}

	}

	public void updateMaxMin(String clusteringKey, Page pageObj) {
		pageObj.minKey = pageObj.records.get(0).get(clusteringKey);
		int lastIndex = pageObj.records.size() - 1;
		pageObj.maxKey = pageObj.records.get(lastIndex).get(clusteringKey);
	}

	public boolean sharesValues(Hashtable<String, Object> currRecord, Hashtable<String, Object> htblColNameValue) {
		Set<String> sharedKeys = new HashSet<String>(currRecord.keySet());
		sharedKeys.retainAll(htblColNameValue.keySet()); // retain only the keys that are in both hashtables

		boolean sameValues = true;
		for (String key : sharedKeys) {
			if (!currRecord.get(key).equals(htblColNameValue.get(key))) {
				sameValues = false;
				break;
			}
		}

		return sameValues;
	}

	public void delete(String path) {
		File f = new File(path);
		f.delete();
	}

	public void displayHash(Hashtable<String, Object> records) {
		records.entrySet().forEach(entry -> {
			System.out.println(entry.getKey() + "->" + entry.getValue());
		});
	}

	public boolean greaterThanAllKeys(Vector<Object> pageKeys, Object key) {

		int num = 0;

		for (int i = 0; i < pageKeys.size(); i++) {
			if (compare(pageKeys.get(i), key) < 0)
				num++;
		}

		if (num == pageKeys.size())
			return true;
		else
			return false;
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

	public int getMaxRows() throws DBAppException {

		try {
			Properties prop = new Properties();
			InputStream input = null;

			input = new FileInputStream("src\\main\\resources\\DBApp.config");

			// load a properties file
			prop.load(input);

			return Integer.parseInt(prop.getProperty("MaximumRowsCountinPage"));
		} catch (Exception e) {
			throw new DBAppException(e.getMessage());
		}

	}

	public int getMaxEntries() throws DBAppException {

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

	public static void serializeObject(Object o, String path) throws DBAppException {

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

	public ArrayList<String> getColumns(String tableName) throws DBAppException {
		ArrayList<String> columnNames = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader("metadata.csv"))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] values = line.split(",");
				if (values[0].equals(tableName)) {
					columnNames.add(values[1]); // add the column name to the list
				}
			}
		} catch (Exception e) {
			throw new DBAppException();
		}
		return columnNames;
	}

	public Hashtable<String, Object> getMax(String tableName) throws DBAppException {
		Hashtable<String, Object> maxHash = new Hashtable<String, Object>();
		try (BufferedReader br = new BufferedReader(new FileReader("metadata.csv"))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] values = line.split(",");
				if (values[0].equals(tableName)) {
					Object max = parseStringToObject(values[7]);
					maxHash.put(values[1], max); // add the column name to the list
				}
			}
		} catch (Exception e) {
			throw new DBAppException();
		}
		return maxHash;
	}

	public Hashtable<String, String> getType(String tableName) throws DBAppException {
		Hashtable<String, String> type = new Hashtable<String, String>();
		try (BufferedReader br = new BufferedReader(new FileReader("metadata.csv"))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] values = line.split(",");
				if (values[0].equals(tableName)) {
					type.put(values[1], values[2]); // add the column name to the list
				}
			}
		} catch (Exception e) {
			throw new DBAppException();
		}
		return type;
	}

	public Hashtable<String, Object> getMin(String tableName) throws DBAppException {
		Hashtable<String, Object> minHash = new Hashtable<String, Object>();
		try (BufferedReader br = new BufferedReader(new FileReader("metadata.csv"))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] values = line.split(",");
				if (values[0].equals(tableName)) {
					Object min = parseStringToObject(values[6]);
					minHash.put(values[1], min); // add the column name to the list
				}
			}
		} catch (Exception e) {
			throw new DBAppException();
		}
		return minHash;
	}

	public void displayTable(String strTableName) throws DBAppException {

		try {
			Table tableObj = (Table) deserializeObject("Tables/" + strTableName + ".ser");

			System.out.println("the number of pages this table has " + tableObj.pagePaths.size());

			for (int j = 0; j < tableObj.pagePaths.size(); j++) {
				Page pageObj = (Page) deserializeObject(tableObj.pagePaths.get(j));
				for (int i = 0; i < pageObj.records.size(); i++) {
					System.out.println("i am in page " + j);
					System.out.println("with record number " + i);
					displayHash(pageObj.records.get(i));


				}

				serializeObject(pageObj, tableObj.pagePaths.get(j));

			}

			serializeObject(tableObj, "Tables/" + strTableName + ".ser");

		} catch (Exception e) {
			e.printStackTrace();
			throw new DBAppException();
		}

	}

	public static Object parseStringToObject(String str) {
		try {
			// Try parsing as integer
			return Integer.parseInt(str);
		} catch (NumberFormatException e) {
			try {
				// Try parsing as double
				return Double.parseDouble(str);
			} catch (NumberFormatException e2) {
				try {
					// Try parsing as date
					SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
					Date date = format.parse(str);
					return date;
				} catch (ParseException e3) {
					// Default to string
					return str;
				}
			}
		}
	}

	public void indexCSVUpdate(List<String[]> rowsToUpdate, String newIndexName) throws DBAppException {

		String filePath = "metadata.csv";

		String newIndexType = "Octree";

		try {
			// Read the CSV file
			List<String> lines = Files.readAllLines(Paths.get(filePath));

			// Update the specific rows
			for (int i = 0; i < lines.size(); i++) {
				String[] columns = lines.get(i).split(",");
				String[] col1 = rowsToUpdate.get(0);
				String[] col2 = rowsToUpdate.get(1);
				String[] col3 = rowsToUpdate.get(2);

				// Check if the row matches the criteria for update

				if((col1[0].equals(columns[0])&&col1[1].equals(columns[1]))||
						(col2[0].equals(columns[0])&&col2[1].equals(columns[1]))||
						(col3[0].equals(columns[0])&&col3[1].equals(columns[1]))) {




					columns[4] = newIndexName;
					columns[5] = newIndexType;

					// Construct the updated row
					String updatedRow = String.join(",", columns);
					lines.set(i, updatedRow);

				}
			}

			// Write the updated lines back to the file
			Files.write(Paths.get(filePath), lines);

		} catch (Exception e) {
			e.printStackTrace();
			throw new DBAppException();
		}

	}



	public void createIndex(String strTableName, String[] strarrColName) throws DBAppException {

		// validation checking for the parameters im getting
		ArrayList<String> cols = getColumns(strTableName);

		if (strarrColName.length != 3)
			throw new DBAppException();

		for (int i = 0; i < strarrColName.length; i++) {
			if (!cols.contains(strarrColName[i]))
				throw new DBAppException();
		}

		Table tableObj = (Table) deserializeObject("Tables/" + strTableName + ".ser");

		if (tableObj.hasIndex) {
			for (String indexPath : tableObj.indexPaths) {
				Octree tree = (Octree) deserializeObject(indexPath);
				if (tree.indexColumns.contains(strarrColName[0]) || tree.indexColumns.contains(strarrColName[1])
						|| tree.indexColumns.contains(strarrColName[2])) {
					throw new DBAppException();
				}
			}
		}

		// get the min and max for each columns to set the bounds of the cube

		Hashtable<String, Object> minCols = getMin(strTableName);
		Hashtable<String, Object> maxCols = getMax(strTableName);

		Object x = minCols.get(strarrColName[0]);
		Object y = minCols.get(strarrColName[1]);
		Object z = minCols.get(strarrColName[2]);

		Point minPoint = new Point(x, y, z);

		x = maxCols.get(strarrColName[0]);
		y = maxCols.get(strarrColName[1]);
		z = maxCols.get(strarrColName[2]);

		Point maxPoint = new Point(x, y, z);

		Bounds3D orgCube = new Bounds3D(minPoint, maxPoint);

		// update the csv file to show that an index has been created on a certain 3
		// columns

		List<String[]> rowsToUpdate = new ArrayList<>();
		rowsToUpdate.add(new String[] { strTableName, strarrColName[0] });
		rowsToUpdate.add(new String[] { strTableName, strarrColName[1] });
		rowsToUpdate.add(new String[] { strTableName, strarrColName[2] });

		String newIndexName = "";

		for (int i = 0; i < rowsToUpdate.size(); i++) {
			String columnName = rowsToUpdate.get(i)[1];
			newIndexName += columnName;
		}

		newIndexName += "Index";

		indexCSVUpdate(rowsToUpdate, newIndexName);

		// set up the octree's constructor parameters, for easy access to find which
		// columns had an index built on them

		Hashtable<String, String> indexColumns = new Hashtable<String, String>();
		indexColumns.put("x", strarrColName[0]);
		indexColumns.put("y", strarrColName[1]);
		indexColumns.put("z", strarrColName[2]);

		// the third parameter of the octree constructor is for serializing the index
		// object

		Octree tree = new Octree(orgCube, indexColumns, newIndexName);

		// start inserting into the octree records

		for (int i = 0; i < tableObj.pagePaths.size(); i++) {
			Page pageObj = (Page) deserializeObject(tableObj.pagePaths.get(i));
			for (int j = 0; j < pageObj.records.size(); j++) {
				Hashtable<String, Object> rec = pageObj.records.get(j);
				Set<String> keyNames = rec.keySet();
				if (keyNames.contains(strarrColName[0]) && keyNames.contains(strarrColName[1])
						&& keyNames.contains(strarrColName[2])) {
					Entry entry = new Entry(rec.get(tableObj.ClusteringKey),
							new Point(rec.get(strarrColName[0]), rec.get(strarrColName[1]), rec.get(strarrColName[2])),
							tableObj.pagePaths.get(i));
					tree.insert(entry);

				}
			}

			serializeObject(pageObj, tableObj.pagePaths.get(i));

		}

		// we use the indexPaths attribute of the table object to deserialize indices,
		// just like how we do with pages

		File forIndex = new File(strTableName + "Index");
		forIndex.mkdirs();

		tableObj.hasIndex = true;
		tableObj.indexPaths.add(strTableName + "Index/" + tree.ID + ".ser");

		serializeObject(tree, strTableName + "Index/" + tree.ID + ".ser");
		serializeObject(tableObj, "Tables/" + strTableName + ".ser");

	}

	public Iterator selectFromTable(SQLTerm[] arrSQLTerms, String[] strarrOperators) throws DBAppException {
		String tableName = arrSQLTerms[0]._strTableName;
		String columnName = arrSQLTerms[0]._strColumnName;
		String operator = arrSQLTerms[0]._strOperator;
		Object objValue = arrSQLTerms[0]._objValue;

		Hashtable<String, Object> maxHash = getMax(tableName);
		Hashtable<String, Object> minHash = getMin(tableName);
		Object min = minHash.get(columnName);
		Object max = maxHash.get(columnName);
		Hashtable<String, String> type = getType(tableName);
		ArrayList<Hashtable<String, Object>> result = new ArrayList();
		ArrayList<Entry> resultEntry = new ArrayList();
		ArrayList<Entry> resultEntry2 = new ArrayList();
		ArrayList<Entry> resultEntry3 = new ArrayList();
		int coordinate = 0;
		int indexIndex = 0;
		ArrayList<Hashtable<String, Object>> result2 = new ArrayList();
		if (type.containsKey(columnName)) {
			String currentType = "class " + type.get(columnName);
			String typeToCheck = objValue.getClass() + "";
			if (!currentType.equals(typeToCheck)) {
				throw new DBAppException();
			}

		} else {
			throw new DBAppException();
		}
		Table tableObj = (Table) deserializeObject("Tables/" + tableName + ".ser");

		boolean useIndex = false;
		if (tableObj.hasIndex) {
			if (arrSQLTerms.length == 3) {
				int andCounter = 0;
				for (int i = 0; i < strarrOperators.length && strarrOperators[i].equals("AND"); i++) {
					andCounter++;
				}
				if (andCounter == 2) {
					String tableName2 = arrSQLTerms[1]._strTableName;
					String columnName2 = arrSQLTerms[1]._strColumnName;
					String operator2 = arrSQLTerms[1]._strOperator;
					Object objValue2 = arrSQLTerms[1]._objValue;
					String tableName3 = arrSQLTerms[2]._strTableName;
					String columnName3 = arrSQLTerms[2]._strColumnName;
					String operator3 = arrSQLTerms[2]._strOperator;
					Object objValue3 = arrSQLTerms[2]._objValue;

					if (!columnName2.equals(columnName) && !columnName2.equals(columnName3)
							&& !columnName.equals(columnName3)) {

						for (int i = 0; i < tableObj.indexPaths.size(); i++) {
							int hitCount = 0;
							Octree indObj = (Octree) deserializeObject(tableObj.indexPaths.get(i));
							if (indObj.indexColumns.containsValue(objValue)) {
								hitCount++;
							}
							if (indObj.indexColumns.containsValue(objValue2)) {
								hitCount++;
							}
							if (indObj.indexColumns.containsValue(objValue3)) {
								hitCount++;
							}
							if (hitCount == 3) {
								indexIndex = i;

							}
							useIndex = true;
						}
					}
				}

			}
		}
		if (!useIndex) {

			for (int i = 0; i < tableObj.pagePaths.size(); i++) {
				Page pageObj = (Page) deserializeObject(tableObj.pagePaths.get(i));

				for (int j = 0; j < pageObj.records.size(); j++) {
					if (operator.equals("=")) {
						Hashtable<String, Object> currRecord = pageObj.records.get(j);
						if (compare(currRecord.get(columnName), objValue) == 0) {
							result.add(currRecord);
						}
					}
					if (operator.equals(">")) {
						Hashtable<String, Object> currRecord = pageObj.records.get(j);
						if (compare(currRecord.get(columnName), objValue) > 0) {
							result.add(currRecord);
						}
					}
					if (operator.equals("<")) {
						Hashtable<String, Object> currRecord = pageObj.records.get(j);
						if (compare(currRecord.get(columnName), objValue) < 0) {
							result.add(currRecord);
						}
					}
					if (operator.equals(">=")) {
						Hashtable<String, Object> currRecord = pageObj.records.get(j);
						if (compare(currRecord.get(columnName), objValue) >= 0) {
							result.add(currRecord);
						}
					}
					if (operator.equals("<=")) {
						Hashtable<String, Object> currRecord = pageObj.records.get(j);
						if (compare(currRecord.get(columnName), objValue) <= 0) {
							result.add(currRecord);
						}
					}
					if (operator.equals("!=")) {
						Hashtable<String, Object> currRecord = pageObj.records.get(j);
						if (compare(currRecord.get(columnName), objValue) != 0) {
							result.add(currRecord);
						}
					}
					serializeObject(pageObj, tableObj.pagePaths.get(i));

				}
			}

			for (int k = 1; k < arrSQLTerms.length; k++) {
				tableName = arrSQLTerms[k]._strTableName;
				columnName = arrSQLTerms[k]._strColumnName;
				operator = arrSQLTerms[k]._strOperator;
				objValue = arrSQLTerms[k]._objValue;
				type = getType(tableName);

				if (type.containsKey(columnName)) {
					String currentType = "class " + type.get(columnName);
					String typeToCheck = objValue.getClass() + "";
					if (!currentType.equals(typeToCheck)) {
						throw new DBAppException();
					}

				} else {
					throw new DBAppException();
				}

				for (int i = 0; i < tableObj.pagePaths.size(); i++) {
					Page pageObj = (Page) deserializeObject(tableObj.pagePaths.get(i));

					for (int j = 0; j < pageObj.records.size(); j++) {
						if (operator.equals("=")) {
							Hashtable<String, Object> currRecord = pageObj.records.get(j);
							if (compare(currRecord.get(columnName), objValue) == 0) {
								result2.add(currRecord);
							}
						}
						if (operator.equals(">")) {
							Hashtable<String, Object> currRecord = pageObj.records.get(j);
							if (compare(currRecord.get(columnName), objValue) > 0) {
								result2.add(currRecord);
							}
						}
						if (operator.equals("<")) {
							Hashtable<String, Object> currRecord = pageObj.records.get(j);
							if (compare(currRecord.get(columnName), objValue) < 0) {
								result2.add(currRecord);
							}
						}
						if (operator.equals(">=")) {
							Hashtable<String, Object> currRecord = pageObj.records.get(j);
							if (compare(currRecord.get(columnName), objValue) >= 0) {
								result2.add(currRecord);
							}
						}
						if (operator.equals("<=")) {
							Hashtable<String, Object> currRecord = pageObj.records.get(j);
							if (compare(currRecord.get(columnName), objValue) <= 0) {
								result2.add(currRecord);
							}
						}
						if (operator.equals("!=")) {
							Hashtable<String, Object> currRecord = pageObj.records.get(j);
							if (compare(currRecord.get(columnName), objValue) != 0) {
								result2.add(currRecord);
							}
						}
						serializeObject(pageObj, tableObj.pagePaths.get(i));

					}
				}

				String currOp = strarrOperators[k - 1];
				if (currOp.equals("OR")) {
					result.removeIf(n -> (result2.contains(n)));
					result.addAll(result2);
				}
				if (currOp.equals("AND")) {
					result.removeIf(n -> (!result2.contains(n)));
				}

				if (currOp.equals("XOR")) {
					ArrayList<Hashtable<String, Object>> resulttemp = result;
					ArrayList<Hashtable<String, Object>> resulttemp2 = result2;
					resulttemp2.removeIf(n -> (!resulttemp.contains(n)));
					resulttemp2.addAll(result);
					resulttemp.removeIf(n -> (!result2.contains(n)));
					resulttemp2.removeIf(n -> (!resulttemp.contains(n)));
					result = resulttemp;

				}
			}
		}

		// index
		else {
			Octree indObj = (Octree) deserializeObject(tableObj.indexPaths.get(indexIndex));
			String tableName2 = arrSQLTerms[1]._strTableName;
			String columnName2 = arrSQLTerms[1]._strColumnName;
			String operator2 = arrSQLTerms[1]._strOperator;
			Object objValue2 = arrSQLTerms[1]._objValue;
			String tableName3 = arrSQLTerms[2]._strTableName;
			String columnName3 = arrSQLTerms[2]._strColumnName;
			String operator3 = arrSQLTerms[2]._strOperator;
			Object objValue3 = arrSQLTerms[2]._objValue;
			ArrayList<Hashtable<String, Object>> result3 = new ArrayList();
			Object x = null;
			Object y = null;
			Object z = null;
			String opx = "";
			String opy = "";
			String opz = "";
			String colx = "";
			String coly = "";
			String colz = "";
			Object maxx = null;
			Object maxy = null;
			Object maxz = null;
			Object minx = null;
			Object miny = null;
			Object minz = null;

			if (getKeyByValue(indObj.indexColumns, columnName).equals("x")) {
				x = objValue;
				opx = operator;
				colx = columnName;
				minx = minHash.get(columnName);
				maxx = maxHash.get(columnName);


			}
			if (getKeyByValue(indObj.indexColumns, columnName2).equals("x")) {
				x = objValue2;
				opx = operator2;
				colx = columnName2;
				minx = minHash.get(columnName2);
				maxx = maxHash.get(columnName2);

			}
			if (getKeyByValue(indObj.indexColumns, columnName3).equals("x")) {
				x = objValue3;
				opx = operator3;
				colx = columnName3;
				minx = minHash.get(columnName3);
				maxx = maxHash.get(columnName3);

			}
			if (getKeyByValue(indObj.indexColumns, columnName).equals("y")) {
				y = objValue;
				opy = operator;
				coly = columnName;
				miny = minHash.get(columnName);
				maxy = maxHash.get(columnName);

			}
			if (getKeyByValue(indObj.indexColumns, columnName2).equals("y")) {
				y = objValue2;
				opy = operator2;
				coly = columnName2;
				miny = minHash.get(columnName2);
				maxy = maxHash.get(columnName2);

			}
			if (getKeyByValue(indObj.indexColumns, columnName3).equals("y")) {
				y = objValue3;
				opy = operator3;
				coly = columnName3;
				miny = minHash.get(columnName3);
				maxy = maxHash.get(columnName3);

			}
			if (getKeyByValue(indObj.indexColumns, columnName).equals("z")) {
				z = objValue;
				opz = operator;
				colz = columnName;
				minz = minHash.get(columnName);
				maxz = maxHash.get(columnName);

			}
			if (getKeyByValue(indObj.indexColumns, columnName2).equals("z")) {
				z = objValue2;
				opz = operator2;
				colz = columnName2;
				minz = minHash.get(columnName2);
				maxz = maxHash.get(columnName2);

			}
			if (getKeyByValue(indObj.indexColumns, columnName3).equals("z")) {
				z = objValue3;
				opz = operator3;
				colz = columnName3;
				minz = minHash.get(columnName3);
				maxz = maxHash.get(columnName3);

			}
			Point point = new Point(x, y, z);

			if (opz.equals("=")) {

				searchz(resultEntry, indObj.root, z, z);


			}
			if (opz.equals(">=")) {
				searchz(resultEntry, indObj.root, maxz, z);

			}
			if (opz.equals("<=")) {
				searchz(resultEntry, indObj.root, z, minz);

			}
			if (opz.equals(">")) {
				searchz(resultEntry, indObj.root, maxz, z);
				resultEntry.removeIf(n -> (compare(n.colsValues.getZ(), point.getZ()) == 0));

			}
			if (opz.equals("<")) {
				searchz(resultEntry, indObj.root, z, minz);

				resultEntry.removeIf(n -> (compare(n.colsValues.getZ(), point.getZ()) == 0));

			}
			if (opz.equals("!=")) {
				searchz(resultEntry, indObj.root, z, minz);
				searchz(resultEntry, indObj.root, maxz, z);
				resultEntry.removeIf(n -> (compare(n.colsValues.getZ(), point.getZ()) == 0));

			}

			if (opx.equals("=")) {

				searchx(resultEntry2, indObj.root, x, x);


			}
			if (opx.equals(">=")) {
				searchx(resultEntry2, indObj.root, maxx, x);

			}
			if (opx.equals("<=")) {
				searchx(resultEntry2, indObj.root, x, minx);

			}
			if (opx.equals(">")) {
				searchx(resultEntry2, indObj.root, maxx, x);
				resultEntry2.removeIf(n -> (compare(n.colsValues.getX(), point.getX()) == 0));

			}
			if (opx.equals("<")) {
				searchx(resultEntry2, indObj.root, x, minx);

				resultEntry2.removeIf(n -> (compare(n.colsValues.getX(), point.getX()) == 0));

			}
			if (opx.equals("!=")) {
				searchx(resultEntry2, indObj.root, x, minx);
				searchx(resultEntry2, indObj.root, maxx, x);
				resultEntry2.removeIf(n -> (compare(n.colsValues.getX(), point.getX()) == 0));

			}

			if (opy.equals("=")) {

				searchy(resultEntry3, indObj.root, y, y);



			}
			if (opy.equals(">=")) {
				searchy(resultEntry3, indObj.root, maxy, y);

			}
			if (opy.equals("<=")) {
				searchy(resultEntry3, indObj.root, y, miny);

			}
			if (opy.equals(">")) {
				searchy(resultEntry3, indObj.root, maxy, y);

				resultEntry3.removeIf(n -> (compare(n.colsValues.getY(), n.colsValues.getY()) == 0));

			}
			if (opy.equals("<")) {
				searchy(resultEntry3, indObj.root, y, miny);

				resultEntry3.removeIf(n -> (compare(n.colsValues.getY(), point.getY()) == 0));

			}
			if (opy.equals("!=")) {
				searchy(resultEntry3, indObj.root, y, miny);
				searchy(resultEntry3, indObj.root, maxy, y);
				resultEntry3.removeIf(n -> (compare(n.colsValues.getY(), point.getY()) == 0));

			}
			System.out.println(resultEntry2.size());
			System.out.println(resultEntry3.size());
			System.out.println(resultEntry.size());



			resultEntry.removeIf(n -> (!resultEntry2.contains(n)));
			System.out.println(resultEntry.size());
			resultEntry.removeIf(n -> (!resultEntry3.contains(n)));
			System.out.println(resultEntry.size());

			for (int i = 0; i < resultEntry.size(); i++) {
				Entry currEntry = resultEntry.get(i);
				Page entryPage = (Page) deserializeObject(currEntry.pagePath);
				for (int j = 0; j < entryPage.records.size(); j++) {
					Hashtable<String, Object> currRecord = entryPage.records.get(j);
					if (compare(currRecord.get(colx), x) == 0&&compare(currRecord.get(coly), y) == 0&&
							compare(currRecord.get(colz), z) == 0) {
						if(!result.contains(currRecord))
							result.add(currRecord);
					}

				}
				serializeObject(entryPage, currEntry.pagePath);

			}
			serializeObject(indObj,tableObj.indexPaths.get(indexIndex));
		}


		serializeObject(tableObj, "Tables/" + tableName + ".ser");
		Iterator itr = result.iterator();

		return itr;
	}


	public void searchx(ArrayList<Entry> result, Node root, Object valueMax, Object valueMin) {
		Object min = root.bounds.minPoint.getX();
		Object max = root.bounds.maxPoint.getX();

		if (root.isLeaf) {
			if (compare(valueMin, max) >= 0 || compare(valueMax, min) <= 0) {
				return;
			} else {
				for (int i = 0; i < root.entries.size(); i++) {
					Entry entry = root.entries.get(i);
					if (compare(entry.colsValues.getX(), valueMax) <= 0
							|| compare(entry.colsValues.getX(), valueMin) >= 0) {
						result.add(entry);

					}
				}
				return;
			}
		} else {
			searchx(result, root.children[0], valueMax, valueMin);
			searchx(result, root.children[1], valueMax, valueMin);
			searchx(result, root.children[2], valueMax, valueMin);
			searchx(result, root.children[3], valueMax, valueMin);
			searchx(result, root.children[4], valueMax, valueMin);
			searchx(result, root.children[5], valueMax, valueMin);
			searchx(result, root.children[6], valueMax, valueMin);
			searchx(result, root.children[7], valueMax, valueMin);

		}
	}

	public void searchy(ArrayList<Entry> result, Node root, Object valueMax, Object valueMin) {
		Object min = root.bounds.minPoint.getY();
		Object max = root.bounds.maxPoint.getY();

		if (root.isLeaf) {
			if (compare(valueMin, max) >= 0 || compare(valueMax, min) <= 0) {
				return;
			} else {
				for (int i = 0; i < root.entries.size(); i++) {
					Entry entry = root.entries.get(i);
					if (compare(entry.colsValues.getY(), valueMax) <= 0
							|| compare(entry.colsValues.getY(), valueMin) >= 0) {
						result.add(entry);

					}
				}
				return;
			}
		} else {
			searchy(result, root.children[0], valueMax, valueMin);
			searchy(result, root.children[1], valueMax, valueMin);
			searchy(result, root.children[2], valueMax, valueMin);
			searchy(result, root.children[3], valueMax, valueMin);
			searchy(result, root.children[4], valueMax, valueMin);
			searchy(result, root.children[5], valueMax, valueMin);
			searchy(result, root.children[6], valueMax, valueMin);
			searchy(result, root.children[7], valueMax, valueMin);

		}
	}

	public void searchz(ArrayList<Entry> result, Node root, Object valueMax, Object valueMin) {
		Object min = root.bounds.minPoint.getZ();
		Object max = root.bounds.maxPoint.getZ();

		if (root.isLeaf) {
			if (compare(valueMin, max) >= 0 || compare(valueMax, min) <= 0) {
				return;
			} else {
				for (int i = 0; i < root.entries.size(); i++) {
					Entry entry = root.entries.get(i);
					if (compare(entry.colsValues.getZ(), valueMax) <= 0
							|| compare(entry.colsValues.getZ(), valueMin) >= 0) {
						result.add(entry);

					}
				}
				return;
			}
		} else {
			searchz(result, root.children[0], valueMax, valueMin);
			searchz(result, root.children[1], valueMax, valueMin);
			searchz(result, root.children[2], valueMax, valueMin);
			searchz(result, root.children[3], valueMax, valueMin);
			searchz(result, root.children[4], valueMax, valueMin);
			searchz(result, root.children[5], valueMax, valueMin);
			searchz(result, root.children[6], valueMax, valueMin);
			searchz(result, root.children[7], valueMax, valueMin);

		}
	}



	public static void main(String[] args) throws DBAppException {


	}
}
