package org.vt.hokiehelper;

import android.content.Context;
import android.content.Intent;

public class ActionBarHelper {
	
	public static final String ACTION_MAP = "org.vt.hokiehelper.action.MAP";
	public static final String ACTION_LIST = "org.vt.hokiehelper.action.LIST";
	public static final String ACTION_REFRESH = "org.vt.hokiehelper.action.REFRESH";
	
	
    public static Intent createHomeIntent(Context context) {
        Intent i = new Intent(context, HokieHelperMainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return i;
    }

    public static Intent createListIntent(Context context, Class<?> cls) {
    	Intent i = new Intent(ACTION_LIST, null, context, cls);
    	i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    	return i;
    }
    
    public static Intent createSearchIntent(Context context, Class<?> cls) {
    	Intent i = new Intent(Intent.ACTION_SEARCH, null, context, cls);
    	i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    	return i;
    }
    
    public static Intent createMapIntent(Context context, Class<?> cls) {
    	Intent i = new Intent(ACTION_MAP, null, context, cls);
    	i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    	return i;
    }
    
    public static Intent createRefreshIntent(Context context, Class<?> cls) {
    	Intent i = new Intent(ACTION_REFRESH, null, context, cls);
    	i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    	return i;
    }
}
