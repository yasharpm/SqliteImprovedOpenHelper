package com.yashoid.data.sqlite;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Database Helper class with the possibility to load database from the assets.<br>
 * <br>
 * If you want to initiate your database from an existing file you can put it in your assets folder and
 * give it to the helper through {@code getAssetFileName} method.<br>
 * <br>
 * You can still use {@code onCreate} to add further changes to the database. {@code onUpgrade} and
 * {@code onDowngrade} methods are called like before.<br>
 * <br>
 * In order to have a copy of your database, two methods are available. {@code dumpDatabaseOnCreate} and
 * {@code dumpDatabase}. If in your code you are loading the initial data from different resources into the database
 * you can use these methods to have a copy of the result and put it into the assets folder to skip the loading time
 * of your app.
 * 
 * @author Yashar
 *
 */
public abstract class SQLiteImprovedOpenHelper extends SQLiteOpenHelper {
	
	private static final String TAG = "DbImprovedOpenHelper";
	
	private Context mContext;
	
	private Object mSynchronizeObject;
	
	private String mDatabaseName;
	private CursorFactory mCursorFactory;
	
	private boolean mHasDatabase;
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public SQLiteImprovedOpenHelper(Context context, String name,
			CursorFactory factory, int version,
			DatabaseErrorHandler errorHandler) {
		super(context, name, factory, version, errorHandler);
		initialize(context, name, factory);
	}

	public SQLiteImprovedOpenHelper(Context context, String name,
			CursorFactory factory, int version) {
		super(context, name, factory, version);
		initialize(context, name, factory);
	}

	private void initialize(Context context, String name, CursorFactory factory) {
		mContext = context;
		
		mSynchronizeObject = new Object();
		
		mDatabaseName = name;
		mCursorFactory = factory;
		
		mHasDatabase = false;
	}
	
	@Override
	public SQLiteDatabase getReadableDatabase() {
		synchronized (mSynchronizeObject) {
			if (!mHasDatabase) {
				createDatabase();
			}
			return super.getReadableDatabase();
		}
	}
	
	@Override
	public SQLiteDatabase getWritableDatabase() {
		synchronized (mSynchronizeObject) {
			if (!mHasDatabase) {
				createDatabase();
			}
			return super.getWritableDatabase();
		}
	}
	
	/**
	 * 
	 * @return The path and name of the database file in the assets folder. Return null if you don't have a database to be preloaded.
	 */
	protected abstract String getAssetFileName();
	
	private void createDatabase() {
		String assetFileName = getAssetFileName();
		
		if (assetFileName==null) {
			return;
		}
		
		File dbFile = mContext.getDatabasePath(mDatabaseName);
		
		try {
			SQLiteDatabase db = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), mCursorFactory, SQLiteDatabase.OPEN_READWRITE);
			mHasDatabase = true;
			db.close();
		} catch (Throwable t) {
			SQLiteDatabase db = mContext.openOrCreateDatabase(mDatabaseName, 8, mCursorFactory);
			db.close();
		}
		
		if (!mHasDatabase) {
			try {
				InputStream is = mContext.getAssets().open(assetFileName);
				
				OutputStream os = new FileOutputStream(dbFile);
				
				int len = 0;
				byte[] buffer = new byte[512];
				
				while (len!=-1) {
					len = is.read(buffer);
					
					if (len>0) {
						os.write(buffer, 0, len);
					}
				}
				
				os.flush();
				os.close();
				is.close();
				
				mHasDatabase = true;
			} catch (IOException e) {
				Log.e(TAG, "Could not create database file from assets", e);
			}
		}
	}
	
	/**
	 * Creates a database file from the onCreate method. You can call this method anywhere in your code.
	 * @param fileName The name of the file to be saved in the external downloads folder of the device.
	 * @return {@code true} if the operation is successful.
	 */
	public boolean dumpDatabaseOnCreate(String fileName) {
		try {
			File dbFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
			SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(dbFile, mCursorFactory);
			onCreate(db);
			db.close();
		} catch (Throwable t) {
			Log.e(TAG, "Failed to dump database.", t);
			
			return false;
		}
		
		return true;
	}
	
	/**
	 * Creates a database file from the current state of the database. <b>This method must be called only after there exists
	 * some database file. i.e. onCreate has been called (through getReadableDatabase or getWriteableDatabase).</b>
	 * @param fileName The name of the file to be saved in the external downloads folder of the device.
	 * @return {@code true} if the operation is successful.
	 */
	public boolean dumpDatabase(String fileName) {
		File dbInternal = mContext.getDatabasePath(mDatabaseName);
		File dbExternal = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
		
		try {
			InputStream is = new FileInputStream(dbInternal);
			OutputStream os = new FileOutputStream(dbExternal);
			
			int len = 0;
			byte[] buffer = new byte[512];
			
			while (len!=-1) {
				len = is.read(buffer);
				
				if (len>0) {
					os.write(buffer, 0, len);
				}
			}
			
			os.flush();
			os.close();
			is.close();
			
			return true;
		} catch (IOException e) {
			Log.e(TAG, "Failed to dump database. Has onCreate been called yet?", e);
		}
		
		return false;
	}

}
