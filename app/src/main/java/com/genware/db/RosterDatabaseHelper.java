package com.genware.db;

import com.genware.db.RosterProvider.RosterConstants;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class RosterDatabaseHelper extends SQLiteOpenHelper {
	
	private static final String DATABASE_NAME = "roster.db";
	private static final int DATABASE_VERSION = 4;

	public RosterDatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {

		db.execSQL("CREATE TABLE " + RosterProvider.TABLE_ROSTER + " (" + RosterConstants._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ RosterConstants.JID + " TEXT UNIQUE ON CONFLICT REPLACE, " + RosterConstants.ALIAS + " TEXT, "
				+ RosterConstants.STATUS_MODE + " INTEGER, " + RosterConstants.STATUS_MESSAGE + " TEXT, "
				+ RosterConstants.GROUP + " TEXT);");
		db.execSQL("CREATE INDEX idx_roster_group ON " + RosterProvider.TABLE_ROSTER + " (" + RosterConstants.GROUP + ")");
		db.execSQL("CREATE INDEX idx_roster_alias ON " + RosterProvider.TABLE_ROSTER + " (" + RosterConstants.ALIAS + ")");
		db.execSQL("CREATE INDEX idx_roster_status ON " + RosterProvider.TABLE_ROSTER + " (" + RosterConstants.STATUS_MODE + ")");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		switch (oldVersion) {
		default:
			db.execSQL("DROP TABLE IF EXISTS " + RosterProvider.TABLE_GROUPS);
			db.execSQL("DROP TABLE IF EXISTS " + RosterProvider.TABLE_ROSTER);
			onCreate(db);
		}
	}

	/**
	 * @param context
	 * @return 删除数据库
	 */
	public boolean deleteDatabase(Context context) {
		return context.deleteDatabase(DATABASE_NAME);
	}
}
