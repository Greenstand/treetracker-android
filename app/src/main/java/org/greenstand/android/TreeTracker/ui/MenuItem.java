package org.greenstand.android.TreeTracker.ui;

public class MenuItem {

	private String name;
	private String id;
	
	public MenuItem (String name ) {
		this.setName(name);
	}
	
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}
