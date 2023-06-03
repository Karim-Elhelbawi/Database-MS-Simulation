package project1;

import java.io.Serializable;
import java.util.*;

public class Octree implements Serializable {
	
    public Node root;
	public String ID;
	public Hashtable<String, String> indexColumns;
    

    public Octree(Bounds3D bounds, Hashtable<String, String> indexColumns, String ID) throws DBAppException {
        this.root = new Node(bounds);   
    	this.indexColumns = indexColumns;
        this.ID = ID;
    }

    public void insert(Entry entry) throws DBAppException {
        root.insert(entry);
    }
    
    public void delete(Entry entry) throws DBAppException {
        root.delete(entry);
    }

//    public void update(Entry oldEntry, Entry newEntry) throws DBAppException {
//        root.update(oldEntry, newEntry);
//    }
    
    public  List<Entry> search(Point point) throws DBAppException {
       return root.searchPoint(point);
    }
    
    public  Node searchForNode(Point point) throws DBAppException {
        return root.searchPointNode(point);
     }
    
    public void display() {
        System.out.println("Octree ID: " + ID);
        System.out.println("Index Columns: " + indexColumns);
        System.out.println("--- Octree Structure ---");
        displayNode(root, 0);
    }

    private void displayNode(Node node, int depth) {
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            indent.append("\t");
        }

        System.out.println(indent.toString() + "Node (Leaf: " + node.isLeaf + ")");
        System.out.println(indent.toString() + "Bounds: " + node.bounds.display());

        if (!node.isLeaf) {
            for (int i = 0; i < 8; i++) {
                System.out.println(indent.toString() + "Child " + i + ":");
                displayNode(node.children[i], depth + 1);
            }
        } else {
            System.out.println(indent.toString() + "Entries:");
            for (Entry entry : node.entries) {
                System.out.println(indent.toString() + "\t" + entry.colsValues.display() +" " + entry.duplicatesPaths.size() + " " +entry.hasDuplicates() + " " + entry.pagePath);
            }
        }
    }


//    public List<Entry> queryRange(Bounds range) {
//        List<Entry> results = new ArrayList<>();
//        results = root.queryRange(range);
//        return results;
//    }
//    

    
    
    public static void main(String[] args) throws DBAppException {
    	
    	Point maxPoint = new Point(100, 100, 100);
    	Point minPoint = new Point(0, 0, 0);

    	Point point1 = new Point(50, 75, 20);
    	Point point2 = new Point(0, 100, 50);
    	Point point3 = new Point(80, 30, 70);
    	Point point4 = new Point(10, 20, 30);
    	Point point5 = new Point(40, 50, 60);
    	Point point6 = new Point(70, 80, 90);
    	Point point7 = new Point(15, 75, 45);
    	Point point8 = new Point(5, 95, 25);
    	Point point9 = new Point(80, 10, 50);
    	Point point10 = new Point(30, 70, 90);
    	Point point11 = new Point(25, 55, 15);
    	Point point12 = new Point(60, 40, 80);
    	Point point13 = new Point(5, 95, 70);
    	Point point14 = new Point(50, 50, 50);
    	Point point15 = new Point(0, 63, 50);
    	Point point16 = new Point(18, 59, 33);
    	Point point17 = new Point(33, 86, 25);
    	Point point18 = new Point(42, 99, 2);


    	
    	Entry entry1 = new Entry(1,point1, "");
    	Entry entry2 = new Entry(2,point2, "");
    	Entry entry3 = new Entry(3,point3, "");
    	Entry entry4 = new Entry(4,point4, "");
    	Entry entry5 = new Entry(5,point5, "");
    	Entry entry6 = new Entry(6,point6, "");
    	Entry entry7 = new Entry(7,point7, "");
    	Entry entry8 = new Entry(8,point8, "");
    	Entry entry9 = new Entry(9,point9, "");
    	Entry entry10 = new Entry(10,point10, "");
    	Entry entry11 = new Entry(11,point11, "");
    	Entry entry12 = new Entry(12,point12, "");
    	Entry entry13 = new Entry(13,point13, "");
    	Entry entry14 = new Entry(14,point14, "");
    	Entry entry15 = new Entry(15,point15, "");
    	Entry entry16 = new Entry(16,point16, "");
    	Entry entry17 = new Entry(17,point17, "");
    	Entry entry18 = new Entry(18,point18, "");

    	
    	Bounds3D cube = new Bounds3D(minPoint, maxPoint);
    	
    	Hashtable<String, String> indexColumns = new Hashtable<String, String>() ;
    	indexColumns.put("x", "name");
    	indexColumns.put("y", "age");
    	indexColumns.put("z", "salary");


    	Octree tree = new Octree(cube, indexColumns,"");
    	tree.insert(entry1);
    	tree.insert(entry2);
    	tree.insert(entry14);
    	tree.insert(entry3);
    	tree.insert(entry4);
    	tree.insert(entry5);
    	tree.insert(entry6);
    	tree.insert(entry7);
    	tree.insert(entry8);
    	tree.insert(entry9);
    	tree.insert(entry10);
    	tree.insert(entry11);
    	tree.insert(entry12);
    	tree.insert(entry13);
    	tree.insert(entry5);
    	tree.insert(entry6);
    	tree.insert(entry7);
    	tree.insert(entry8);


    	
    	//System.out.println("done");


    	tree.display();


    	tree.delete(entry6);
    	tree.delete(entry7);
    	tree.delete(entry8);
    	tree.delete(entry9);
    	
    	System.out.println("\n");

    	tree.display();



    }
}
