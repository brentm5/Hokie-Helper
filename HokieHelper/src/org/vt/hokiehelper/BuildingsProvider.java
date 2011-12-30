package org.vt.hokiehelper;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

public class BuildingsProvider extends ContentProvider {
	
	public static String AUTHORITY = "org.vt.hokiehelper.buildingsprovider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/buildings");	
	
    private static final int SEARCH_SUGGEST = 0;
    private static final int SHORTCUT_REFRESH = 1;
    private static final UriMatcher sURIMatcher = buildUriMatcher();
    
    private MapsDatabaseAdapter buildingsDb_;
	
    private static final String[] COLUMNS = {
    	"_id", // must include this column
    	SearchManager.SUGGEST_COLUMN_TEXT_1,
    	SearchManager.SUGGEST_COLUMN_INTENT_DATA
    };

    /**
     * Sets up a uri matcher for search suggestion and shortcut refresh queries.
     */
    private static UriMatcher buildUriMatcher() {
        UriMatcher matcher =  new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_SHORTCUT, SHORTCUT_REFRESH);
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_SHORTCUT + "/*", SHORTCUT_REFRESH);
        return matcher;
    }
    
    public boolean onCreate() {
    	// Hold off opening the database until the first query
    	buildingsDb_ = new MapsDatabaseAdapter(getContext());
        return true;
    }
    
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
    	if(!buildingsDb_.isOpen()) {
    		buildingsDb_.open(); // if not open, open the database adapter
    	}
        if (!TextUtils.isEmpty(selection)) {
            throw new IllegalArgumentException("selection not allowed for " + uri);
        }
        if (selectionArgs != null && selectionArgs.length != 0) {
            throw new IllegalArgumentException("selectionArgs not allowed for " + uri);
        }
        if (!TextUtils.isEmpty(sortOrder)) {
            throw new IllegalArgumentException("sortOrder not allowed for " + uri);
        }
        if (projection != null && projection.length != 0) {
            throw new IllegalArgumentException("projection not allowed for " + uri);
        }
        switch (sURIMatcher.match(uri)) {
            case SEARCH_SUGGEST:
                String query = null;
                if (uri.getPathSegments().size() > 1) {
                    query = uri.getLastPathSegment().toLowerCase();
                }
                return getSuggestions(query);
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }
    }
    
    private Cursor getSuggestions(String query) { 
    	if(query != null) {
    		query = query.toLowerCase();
    		return buildingsDb_.searchLocations(query);
    	}
    	else {
    		return null;
    	}
    	
    }

	@Override
	public String getType(Uri uri) {
		 switch (sURIMatcher.match(uri)) {
		 	case SEARCH_SUGGEST:
		 		return SearchManager.SUGGEST_MIME_TYPE;
		 	case SHORTCUT_REFRESH:
                return SearchManager.SHORTCUT_MIME_TYPE;
		 	default:
                throw new IllegalArgumentException("Unknown URL " + uri);
		 }
		 	
	}

    // Other required implementations...

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }
}
