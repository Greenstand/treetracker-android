package com.qalliance.treetracker.TreeTracker.ui;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.RelativeLayout;
import android.widget.TextView;




public class MenuItemAdapter extends ArrayAdapter<MenuItem>{
	private Context context;
	private List<MenuItem> objects;


	public MenuItemAdapter(Context context, int resourceId, int textViewResourceId,
			List<MenuItem> objects) {
		super(context, textViewResourceId, objects);
		this.context = context;
		this.objects = objects;
	}
	
	
    public static class ViewHolder{
        public TextView item1;
    }
    
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
    	RelativeLayout row;
        if(convertView == null){
            LayoutInflater inflater = (LayoutInflater)context.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = (RelativeLayout)inflater.inflate(android.R.layout.simple_list_item_1, null);
        }else{
            row = (RelativeLayout)convertView;
        }
        
        String menuItem;
		try {
			menuItem = objects.get(position).getName();
		} catch (Exception e) {
			return row;
		}


       	((TextView)(row.findViewById(android.R.id.text1))).setText(menuItem);
       	
        return row;
    }

    
    public Filter getFilter() {
		return new Filter() {
			
			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {
				objects = (ArrayList<MenuItem>) results.values;
	            notifyDataSetChanged();
				Log.d("objects count", Integer.toString(objects.size()));	            
			}
			
			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				final FilterResults oReturn = new FilterResults();
	            final ArrayList<MenuItem> results = new ArrayList<MenuItem>();
	            final ArrayList<MenuItem> orig = (ArrayList<MenuItem>) objects;
	            
	            if (orig != null) {
	            	if (constraint != null) {
	            		if (orig.size() > 0) {
	            			for (final MenuItem b : orig) {
	            				if (b.getName().toLowerCase().contains(constraint.toString().toLowerCase())) {
	            					results.add(b);
	            				}
	            			}
	            		}
	            		
	            		oReturn.values = results;
	            		oReturn.count = results.size();
	            	}
	            }
	            
				return oReturn;
			}
		};
    	
    }
	
}
