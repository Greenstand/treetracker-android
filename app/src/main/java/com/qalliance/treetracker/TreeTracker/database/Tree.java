package com.qalliance.treetracker.TreeTracker.database;

public class Tree implements Comparable<Tree> {

	private int distance;
	private Object[] restOfData;
	
	public Tree(int distance, Object[] rest) {
        this.distance = distance;
        this.restOfData = rest;
    }

	public int compareTo(Tree tree) {
			return (int) (this.getDistance() - tree.getDistance());
	}

	public int getDistance() {
		return distance;
	}

	public void setDistance(int distance) {
		this.distance = distance;
	}

	public Object[] getRestOfData() {
		return restOfData;
	}

	public void setRestOfData(Object[] restOfData) {
		this.restOfData = restOfData;
	}

}
