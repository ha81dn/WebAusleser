package com.ha81dn.webausleser.backend;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.StringRes;

import com.ha81dn.webausleser.MainActivity;
import com.ha81dn.webausleser.R;

import java.util.ArrayList;

public class DatabaseHandler extends SQLiteOpenHelper {

    private static DatabaseHandler mInstance = null;

    private DatabaseHandler(Context context) {
        super(context, "maindb", null, 1);
    }

    public static DatabaseHandler getInstance(Context context) {

        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (mInstance == null) mInstance = new DatabaseHandler(context.getApplicationContext());
        return mInstance;
    }

    public static String getUniqueCopiedSourceName(MainActivity activity, SQLiteDatabase db, String name) {
        Cursor c;
        int num = 1;
        String newName = name;
        try {
            c = db.rawQuery("select null from sources where name = ?", new String[]{name});
            if (c != null) {
                if (c.moveToFirst()) {
                    c.close();
                    newName = activity.getString(R.string.itemCopy, name);
                    c = db.rawQuery("select null from sources where name = ?", new String[]{newName});
                    if (c != null) {
                        if (c.moveToFirst()) {
                            do {
                                c.close();
                                newName = activity.getString(R.string.itemNextCopy, name, Integer.toString(++num));
                                c = db.rawQuery("select null from sources where name = ?", new String[]{newName});
                                if (c == null) break;
                            } while (c.moveToFirst());
                            if (c != null) c.close();
                        } else c.close();
                    }
                } else c.close();
            }
        } catch (Exception ignored) {
        }
        return newName;
    }

    public static String getUniqueCopiedActionName(MainActivity activity, SQLiteDatabase db, String name, int sourceId) {
        Cursor c;
        int num = 1;
        String newName = name, id = Integer.toString(sourceId);
        try {
            c = db.rawQuery("select null from actions where source_id = ? and name = ?", new String[]{id, name});
            if (c != null) {
                if (c.moveToFirst()) {
                    c.close();
                    newName = activity.getString(R.string.itemCopy, name);
                    c = db.rawQuery("select null from actions where source_id = ? and name = ?", new String[]{id, newName});
                    if (c != null) {
                        if (c.moveToFirst()) {
                            do {
                                c.close();
                                newName = activity.getString(R.string.itemNextCopy, name, Integer.toString(++num));
                                c = db.rawQuery("select null from actions where source_id = ? and name = ?", new String[]{id, newName});
                                if (c == null) break;
                            } while (c.moveToFirst());
                            if (c != null) c.close();
                        } else c.close();
                    }
                } else c.close();
            }
        } catch (Exception ignored) {
        }
        return newName;
    }

    public static String getNavTitleHTML(Context context, SQLiteDatabase db, String table, int id) {
        Cursor c;
        String html = null, tmp;
        int insertPos = 0;
        boolean first = false;
        @StringRes int prefix = -1;
        while (table != null && id >= 0) {
            switch (table) {
                case "sources":
                    table = null;
                    c = db.rawQuery("select name from sources where id = ?", new String[]{Integer.toString(id)});
                    if (c != null) {
                        if (c.moveToFirst()) {
                            if (html == null) {
                                html = context.getString(R.string.actionsFor, c.getString(0)) + " (";
                                insertPos = html.length();
                            } else {
                                tmp = "<a href='SRC" + id + "'>" + c.getString(0) + "</a>";
                                if (first)
                                    tmp += " / ";
                                else
                                    first = true;
                                html = html.substring(0, insertPos) + tmp + html.substring(insertPos);
                            }
                        }
                        c.close();
                    }
                    break;
                case "actions":
                    table = null;
                    c = db.rawQuery("select source_id,name from actions where id = ?", new String[]{Integer.toString(id)});
                    if (c != null) {
                        if (c.moveToFirst()) {
                            if (html == null) {
                                html = context.getString(R.string.stepsFor, c.getString(1)) + " (";
                                insertPos = html.length();
                            } else {
                                tmp = "<a href='ACT" + id + "'>" + c.getString(1) + "</a>";
                                if (first)
                                    tmp += " / ";
                                else
                                    first = true;
                                html = html.substring(0, insertPos) + tmp + html.substring(insertPos);
                            }
                            id = c.getInt(0);
                            table = "sources";
                        }
                        c.close();
                    }
                    break;
                case "steps":
                    table = null;
                    c = db.rawQuery("select action_id,function,parent_id from steps where id = ?", new String[]{Integer.toString(id)});
                    if (c != null) {
                        if (c.moveToFirst()) {
                            if (html == null) {
                                html = context.getString(R.string.paramsFor, c.getString(1)) + " (";
                                insertPos = html.length();
                            } else {
                                tmp = "<a href='STP" + id + "'>" + c.getString(1) + "</a>";
                                if (first)
                                    tmp += " / ";
                                else
                                    first = true;
                                if (prefix != -1) {
                                    tmp = context.getString(prefix, tmp);
                                    prefix = -1;
                                }
                                html = html.substring(0, insertPos) + tmp + html.substring(insertPos);
                            }
                            id = c.getInt(2);
                            if (id == -1) {
                                table = "actions";
                                id = c.getInt(0);
                            } else
                                table = "params";
                        }
                        c.close();
                    }
                    break;
                case "params":
                    table = null;
                    c = db.rawQuery("select step_id,value from params where id = ?", new String[]{Integer.toString(id)});
                    if (c != null) {
                        if (c.moveToFirst()) {
                            switch (c.getString(1)) {
                                case "then":
                                    prefix = R.string.thensFor;
                                    break;
                                case "else":
                                    prefix = R.string.elsesFor;
                                    break;
                            }
                            id = c.getInt(0);
                            table = "steps";
                        }
                        c.close();
                    }
                    break;
                default:
                    table = null;
            }
        }
        if (html != null) html += ")";
        return html;
    }

    public static void selectAsList(SQLiteDatabase db, String query, String[] params, ArrayList<Integer> int1Result, ArrayList<Integer> int2Result, ArrayList<String> strResult, ArrayList<Boolean> blnResult) {
        boolean strFlag = strResult != null;
        boolean int1Flag = int1Result != null;
        boolean int2Flag = int2Result != null;
        boolean blnFlag = blnResult != null;
        if (!int1Flag && !int2Flag && !strFlag && !blnFlag) return;
        Cursor c;
        try {
            c = db.rawQuery(query, params);
            if (c != null) {
                if (c.moveToFirst()) {
                    do {
                        if (int1Flag) {
                            int1Result.add(c.getInt(0));
                            if (int2Flag) {
                                int2Result.add(c.getInt(1));
                                if (strFlag) {
                                    strResult.add(c.getString(2));
                                    if (blnFlag) blnResult.add(c.getInt(3) != 0);
                                } else if (blnFlag) blnResult.add(c.getInt(2) != 0);
                            } else if (strFlag) {
                                strResult.add(c.getString(1));
                                if (blnFlag) blnResult.add(c.getInt(2) != 0);
                            } else if (blnFlag) blnResult.add(c.getInt(1) != 0);
                        } else if (int2Flag) {
                            int2Result.add(c.getInt(0));
                            if (strFlag) {
                                strResult.add(c.getString(1));
                                if (blnFlag) blnResult.add(c.getInt(2) != 0);
                            } else if (blnFlag) blnResult.add(c.getInt(1) != 0);
                        } else if (strFlag) {
                            strResult.add(c.getString(0));
                            if (blnFlag) blnResult.add(c.getInt(1) != 0);
                        } else blnResult.add(c.getInt(0) != 0);
                    } while (c.moveToNext());
                }
                c.close();
            }
        } catch (Exception ignored) {
        }
    }

    public static int getNewId(SQLiteDatabase db, String tableName) {
        Cursor c;
        int id = 0;
        try {
            c = db.rawQuery("select max(id) from " + tableName, null);
            if (c != null) {
                if (c.moveToFirst()) id = c.getInt(0) + 1;
                c.close();
            }
        } catch (Exception ignored) {
        }
        return id;
    }

    public static ArrayList<String> getVariablesBySourceId(SQLiteDatabase db, int sourceId) {
        Cursor c;
        ArrayList<String> varList = new ArrayList<>();
        try {
            c = db.rawQuery("select distinct value from params p, steps s, actions a where p.variable_flag=1 and p.step_id=s.id and s.action_id=a.id and a.source_id=? order by value", new String[]{Integer.toString(sourceId)});
            if (c != null) {
                if (c.moveToFirst()) {
                    do {
                        varList.add(c.getString(0));
                    } while (c.moveToNext());
                }
                c.close();
            }
        } catch (Exception ignored) {
        }
        return varList;
    }

    public static ArrayList<String> getListsBySourceId(SQLiteDatabase db, int sourceId) {
        Cursor c;
        ArrayList<String> lstList = new ArrayList<>();
        try {
            c = db.rawQuery("select distinct value from params p, steps s, actions a where p.list_flag=1 and p.step_id=s.id and s.action_id=a.id and a.source_id=? order by value", new String[]{Integer.toString(sourceId)});
            if (c != null) {
                if (c.moveToFirst()) {
                    do {
                        lstList.add(c.getString(0));
                    } while (c.moveToNext());
                }
                c.close();
            }
        } catch (Exception ignored) {
        }
        return lstList;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table if not exists sources (id integer primary key, name text collate nocase)");
        db.execSQL("create table if not exists actions (id integer primary key, source_id integer references sources(id) on delete cascade, sort_nr integer, name text collate nocase)");
        db.execSQL("create table if not exists steps (id integer primary key, action_id integer references actions(id) on delete cascade, sort_nr integer, function text, call_flag integer, parent_id integer)");
        db.execSQL("create table if not exists params (id integer primary key, step_id integer references steps(id) on delete cascade, idx integer, value text, variable_flag integer, list_flag integer)");
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //db.execSQL("drop table if exists logfile");
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //db.execSQL("drop table if exists logfile");
        onCreate(db);
    }
}