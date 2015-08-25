package com.ha81dn.webausleser;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Html;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ha81dn.webausleser.backend.DatabaseHandler;
import com.ha81dn.webausleser.backend.tables.Action;
import com.ha81dn.webausleser.backend.tables.Param;
import com.ha81dn.webausleser.backend.tables.Source;
import com.ha81dn.webausleser.backend.tables.Step;
import com.ha81dn.webausleser.backend.tables.UniqueRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/* FixMe-Liste
*/

/* ToDo-Liste
- If- und Schleifen-Schachtelei mit Implikationen fürs Browsen, Kopieren, Löschen etc.
- Verschieben und Kopieren asynchron mit ProgressBar-Popup
- Step-Namen ausbuddeln, sprechend übersetzen
- Anlegen von Quellen/Aktionen: bei Namensgleichheit meckern
- Zeilennummern bei sortierten Adaptern
- assistentengestützte Parameter-Wertänderung
- Schritt-Assistent mit Kategorien (Zeichenfolgenfunktionen, Ablaufsteuerung ...)
- Anlegerei mit Assistent für sämtliche vorgefertigten Schritte
- Aktion zur Funktion umwandeln (als Option beim Verschieben und Kopieren)
- Kopfzeile ausbauen (mehrzeilig, Zusatzinfos)
- Datenbankmodell für Jobs und Meldungen
- Tabs für Fragment-Wechsel zu Funktionen, Einstellungen und Testcenter
- History-Back/Forward-Buttons für navTitle
- Hintergrund beim Swipen andersfarbig
- Cursor andersfarbig, weil schlecht zu erkennen
- aussagekräftiges Elementlayout inkl. Bezeichnungen von Kindern
- Absturzbehandlung wie beim MioKlicker, nur mit eigener Activity statt Tabpage
  dazu allgemeinen Exception-Ignorierer bauen (vgl. Uniface), den man in den Optionen aber dann doch
  aufplatschmäßig einschalten kann (also als Popup-Exception mit Fehlerbericht-Senden-Option)
- Testcenter entwickeln
- Widget entwickeln
- Import/Export entwickeln
- ERL Rippelei ordentlich machen
- ERL normalen onClick() erweitern um intentUpdate
- ERL alle Adapter nach Vorbild des SourceAdapter auf neue Drag-Swipe-Kontext-Logik umbauen
- ERL Hinzufüge-Item durch FAB (floating action button) ersetzen
- ERL Umbenennen per ActionMode (synchron)
- ERL Löschen debuggen, da kommen die Adapter-Indizes durcheinander!!!
- ERL Verschieben und Kopieren per ActionMode: synchron per Assistent, bisheriger Pfad vorausgewählt
*/

public class MainActivity extends AppCompatActivity {

    private static final long delay = 3000L;
    static String activeSection = "SOURCES";
    static String sourceName, actionName, stepName;
    static int sourceId = -1, actionId = -1, stepId = -1, selectedId = -1, parentId = -1;
    static ActionMode appActionMode = null;
    static ArrayList<Param> insertParams;
    protected MenuItem progressWheel;
    private boolean mRecentlyBackPressed = false;
    private Handler mExitHandler = new Handler();
    private Runnable mExitRunnable = new Runnable() {
        @Override
        public void run() {
            mRecentlyBackPressed = false;
        }
    };

    static void displaySection(Context context, String section, int id, String name, int focusId) {
        Intent intentUpdate = new Intent();
        intentUpdate.setAction("com.ha81dn.webausleser.ASYNC_MAIN");
        intentUpdate.addCategory(Intent.CATEGORY_DEFAULT);
        intentUpdate.putExtra("TAPITEM", section);
        intentUpdate.putExtra("ID", id);
        intentUpdate.putExtra("NAME", name);
        intentUpdate.putExtra("FOCUS", focusId);
        context.sendBroadcast(intentUpdate);
    }

    static void displaySection(Context context, String section, int id, String name) {
        Intent intentUpdate = new Intent();
        intentUpdate.setAction("com.ha81dn.webausleser.ASYNC_MAIN");
        intentUpdate.addCategory(Intent.CATEGORY_DEFAULT);
        intentUpdate.putExtra("TAPITEM", section);
        intentUpdate.putExtra("ID", id);
        intentUpdate.putExtra("NAME", name);
        context.sendBroadcast(intentUpdate);
    }

    static void insertRow(Context context, String section, String name, int id) {
        Intent intentUpdate = new Intent();
        intentUpdate.setAction("com.ha81dn.webausleser.ASYNC_MAIN");
        intentUpdate.addCategory(Intent.CATEGORY_DEFAULT);
        intentUpdate.putExtra("INSERT", section);
        intentUpdate.putExtra("NAME", name);
        intentUpdate.putExtra("ID", id);
        context.sendBroadcast(intentUpdate);
    }

    static void insertRow(Context context, String section, String name, int id, ArrayList<Param> params) {
        insertParams = params;
        insertRow(context, section, name, id);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        /* Alarmtest
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        Intent intentAlarm = new Intent(this, AlarmReceiver.class);
        intentAlarm = new Intent(this, AlarmReceiver.class);
        Calendar calAlarm = Calendar.getInstance();
        calAlarm.add(Calendar.SECOND, 5);
        long time = calAlarm.getTimeInMillis();
        alarmManager.set(AlarmManager.RTC_WAKEUP, time, PendingIntent.getBroadcast(this, 0, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT));
        */

        /* Datenbanktest
        SQLiteDatabase db = DatabaseHandler.getInstance(this).getWritableDatabase();
        ContentValues vals = new ContentValues();

        vals.put("id", 0);
        vals.put("name", "SPIEGEL ONLINE");
        try {
            db.insert("sources", null, vals);
        }
        catch (Exception e){};

        vals.clear();
        vals.put("id", 1);
        vals.put("name", "DWD");
        try {
            db.insert("sources", null, vals);
        }
        catch (Exception e){};

        vals.clear();
        vals.put("id", 0);
        vals.put("source_id", 0);
        vals.put("sort_nr", 0);
        vals.put("name", "http_get");
        try {
            db.insert("actions", null, vals);
        }
        catch (Exception e){};

        vals.clear();
        vals.put("id", 1);
        vals.put("source_id", 0);
        vals.put("sort_nr", 1);
        vals.put("name", "get_headline");
        try {
            db.insert("actions", null, vals);
        }
        catch (Exception e){};

        vals.clear();
        vals.put("id", 123);
        vals.put("source_id", -1);
        vals.put("sort_nr", 0);
        vals.put("name", "myFunction");
        try {
            db.insert("actions", null, vals);
        }
        catch (Exception e){};

        vals.clear();
        vals.put("id", 0);
        vals.put("action_id", 0);
        vals.put("sort_nr", 0);
        vals.put("function", "get");
        vals.put("call_flag", 0);
        vals.put("parent_id", -1);
        try {
            db.insert("steps", null, vals);
        }
        catch (Exception e){};

        vals.clear();
        vals.put("id", 0);
        vals.put("action_id", 0);
        vals.put("sort_nr", 1);
        vals.put("function", "123");
        vals.put("call_flag", 1);
        vals.put("parent_id", -1);
        try {
            db.insert("steps", null, vals);
        }
        catch (Exception e){};

        vals.clear();
        vals.put("id", 0);
        vals.put("step_id", 0);
        vals.put("idx", 0);
        vals.put("value", "http://www.spiegel.de");
        try {
            db.insert("params", null, vals);
        }
        catch (Exception e){};

        db.close();
        */
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        progressWheel = menu.findItem(R.id.action_progress);
        MenuItemCompat.getActionView(progressWheel);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (appActionMode == null) {
            // ToDo: back mit parents
            switch (activeSection) {
                case "SOURCES":
                case "FUNCTIONS":
                    if (mRecentlyBackPressed) {
                        if (mExitHandler != null) {
                            mExitHandler.removeCallbacks(mExitRunnable);
                            mExitHandler = null;
                        }
                        super.onBackPressed();
                    } else {
                        mRecentlyBackPressed = true;
                        Toast.makeText(this, "zum Beenden ein zweites Mal drücken", Toast.LENGTH_SHORT).show();
                        mExitHandler.postDelayed(mExitRunnable, delay);
                    }
                    break;
                case "ACTIONS":
                    if (sourceId == -1) {
                        displaySection(this, "FUNCTION", -1, null);
                    } else {
                        displaySection(this, "ROOT", -1, null);
                        sourceId = -1;
                        sourceName = null;
                    }
                    break;
                case "STEPS":
                    displaySection(this, "SOURCE", sourceId, sourceName);
                    actionId = -1;
                    actionName = null;
                    break;
                case "PARAMS":
                    displaySection(this, "ACTION", actionId, actionName);
                    stepId = -1;
                    stepName = null;
                    break;
            }
        } else
            super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void fabOnClick(View v) {
        Intent intentUpdate = new Intent();
        intentUpdate.setAction("com.ha81dn.webausleser.ASYNC_MAIN");
        intentUpdate.addCategory(Intent.CATEGORY_DEFAULT);
        intentUpdate.putExtra("TAPITEM", activeSection.substring(0, activeSection.length() - 1));
        intentUpdate.putExtra("ID", -1);
        v.getContext().sendBroadcast(intentUpdate);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        static TextView navTitle;
        static RecyclerView mRecyclerView;
        static FloatingActionButton fab;
        private RecyclerView.LayoutManager mLayoutManager;
        private SelectableAdapter mAdapter = null;
        private mainBroadcastReceiver mainBR;
        private ItemTouchHelper touchHelper;
        private boolean initializationFinished = false;

        public PlaceholderFragment() {
        }

        @Override
        public void onPause() {
            super.onPause();
            getActivity().unregisterReceiver(mainBR);
        }

        @Override
        public void onResume() {
            super.onResume();

            mainBR = new mainBroadcastReceiver();
            IntentFilter intentFilter = new IntentFilter("com.ha81dn.webausleser.ASYNC_MAIN");
            intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
            getActivity().registerReceiver(mainBR, intentFilter);

            if (!initializationFinished) {
                displaySection(getActivity(), "ROOT", -1, null);
                initializationFinished = true;
            }
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final Context context = container.getContext();

            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            navTitle = (TextView) rootView.findViewById(R.id.main_title);
            fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
            mRecyclerView = (RecyclerView) rootView.findViewById(R.id.main_recycler);
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            mRecyclerView.setHasFixedSize(true);
            // use a linear layout manager
            mLayoutManager = new LinearLayoutManager(context);
            mRecyclerView.setLayoutManager(mLayoutManager);
            navTitle.setText(getString(R.string.navTitleSources));

            return rootView;
        }

        private void makeLinkClickable(SpannableStringBuilder strBuilder, final URLSpan span) {
            int start = strBuilder.getSpanStart(span);
            int end = strBuilder.getSpanEnd(span);
            int flags = strBuilder.getSpanFlags(span);
            ClickableSpan clickable = new ClickableSpan() {
                public void onClick(View view) {
                    String url = span.getURL();
                    // ToDo: id ausschnipseln
                    if (url != null) {
                        switch (url.substring(0, 2)) {
                            case "SRC":
                                MainActivity.displaySection(view.getContext(), "SOURCE", sourceId, sourceName);
                                break;
                            case "ACT":
                                MainActivity.displaySection(view.getContext(), "ACTION", actionId, actionName);
                                break;
                            case "STP":
                                MainActivity.displaySection(view.getContext(), "STEP", stepId, stepName);
                                break;
                        }
                    }
                }
            };
            strBuilder.setSpan(clickable, start, end, flags);
            strBuilder.removeSpan(span);
        }

        private void setTextViewHTML(TextView text, String html) {
            CharSequence sequence = Html.fromHtml(html);
            SpannableStringBuilder strBuilder = new SpannableStringBuilder(sequence);
            URLSpan[] urls = strBuilder.getSpans(0, sequence.length(), URLSpan.class);
            for (URLSpan span : urls) {
                makeLinkClickable(strBuilder, span);
            }
            text.setText(strBuilder);
            text.setMovementMethod(LinkMovementMethod.getInstance());
        }

        protected void copyRecord(final boolean moveFlag,
                                  final Context context,
                                  final ArrayList datasetFrom,
                                  final List<Integer> itemsFrom,
                                  final String tableFrom,
                                  final int idShow,
                                  final String tableShow,
                                  final int sortNr) {
            final MainActivity activity = (MainActivity) getActivity();
            final SQLiteDatabase db = DatabaseHandler.getInstance(context).getWritableDatabase();
            final ArrayList<Integer> ids;
            final ArrayList<Integer> sortNrs;
            AlertDialog.Builder builder;
            AlertDialog dialog;

            // ToDo: Back-Buttons

            if (tableShow == null) {
                // ggf. erster Assistentenschritt
                switch (tableFrom) {
                    case "sources":
                        if (itemsFrom.size() == 1) {
                            // eine Quelle wurde zum Kopieren ausgewählt
                            builder = new AlertDialog.Builder(context);
                            builder.setTitle(getString(R.string.copySource));
                            builder.setMessage(getString(R.string.inputName));
                            final EditText input = createInput(context, false);
                            builder.setView(input);
                            final int pos = itemsFrom.get(0);
                            final Source s = (Source) datasetFrom.get(pos);
                            input.setText(DatabaseHandler.getUniqueCopiedSourceName(activity, db, s.getName()));
                            input.setSelectAllOnFocus(true);
                            builder.setPositiveButton(getString(R.string.ok),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            int newSourceId = DatabaseHandler.getNewId(db, "sources");
                                            activity.progressWheel.setVisible(true);
                                            try {
                                                copySource(db, s.getId(), newSourceId, DatabaseHandler.getUniqueCopiedSourceName(activity, db, input.getText().toString().trim()));
                                            } catch (Exception ignored) {
                                            }
                                            activity.progressWheel.setVisible(false);
                                            appActionMode.finish();
                                            db.close();
                                            displaySection(activity, "ROOT", -1, null, newSourceId);
                                        }
                                    });
                            builder.setNegativeButton(getString(R.string.cancel),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            appActionMode.finish();
                                            db.close();
                                        }
                                    });
                            dialog = builder.create();
                            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                            dialog.show();
                        } else {
                            // mehrere Quellen wurden zum Kopieren ausgewählt
                            Source s;
                            int newSourceId = -1;
                            activity.progressWheel.setVisible(true);
                            for (int pos : itemsFrom) {
                                s = (Source) datasetFrom.get(pos);
                                newSourceId = DatabaseHandler.getNewId(db, "sources");
                                try {
                                    copySource(db, s.getId(), newSourceId, DatabaseHandler.getUniqueCopiedSourceName(activity, db, s.getName()));
                                } catch (Exception ignored) {
                                }
                            }
                            activity.progressWheel.setVisible(false);
                            appActionMode.finish();
                            db.close();
                            displaySection(activity, "ROOT", -1, null, newSourceId);
                        }
                        break;
                    case "actions":
                        final ArrayList<String> actionSources = new ArrayList<>();
                        ids = new ArrayList<>();
                        final ArrayList<Boolean> actionSourceWithoutActions = new ArrayList<>();
                        DatabaseHandler.selectAsList(db, "select id,name,ifnull((select 0 from actions where actions.source_id=sources.id" + (moveFlag ? " and actions.id not in " + getInList(itemsFrom, datasetFrom) : "") + "),1) from sources order by name", null, ids, null, actionSources, actionSourceWithoutActions);
                        actionSources.add(0, getString(R.string.asFunction));
                        ids.add(0, -1);
                        actionSourceWithoutActions.add(0, true);

                        builder = new AlertDialog.Builder(context);
                        builder.setTitle(moveFlag ? getString(R.string.moveAction) : getString(R.string.copyAction));
                        selectedId = ids.indexOf(((Action) datasetFrom.get(itemsFrom.get(0))).getSourceId());
                        builder.setSingleChoiceItems(actionSources.toArray(new String[actionSources.size()]), selectedId, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                Button posButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                                if (actionSourceWithoutActions.get(id))
                                    posButton.setText(getString(R.string.insert));
                                else
                                    posButton.setText(getString(R.string.next));
                                selectedId = id;
                            }
                        });
                        builder.setPositiveButton(actionSourceWithoutActions.get(selectedId) ? getString(R.string.insert) : getString(R.string.next), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (actionSourceWithoutActions.get(selectedId)) {
                                    int newActionId = -1;
                                    int sortNr = 0;
                                    for (int pos : itemsFrom) {
                                        Action a = (Action) datasetFrom.get(pos);
                                        newActionId = DatabaseHandler.getNewId(db, "actions");
                                        copyAction(db, a.getId(), newActionId, sortNr++, a.getName(), ids.get(selectedId), moveFlag);
                                    }
                                    appActionMode.finish();
                                    db.close();
                                    displaySection(activity, "SOURCE", ids.get(selectedId), null, newActionId);
                                } else {
                                    copyRecord(moveFlag, context, datasetFrom, itemsFrom, tableFrom, ids.get(selectedId), "actions", -1);
                                }
                            }
                        });
                        builder.setNegativeButton(getString(R.string.cancel),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        appActionMode.finish();
                                        db.close();
                                    }
                                });
                        dialog = builder.create();
                        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                        dialog.show();

                        break;
                    case "steps":
                        final ArrayList<String> stepSources = new ArrayList<>();
                        Cursor c;
                        ids = new ArrayList<>();
                        DatabaseHandler.selectAsList(db, "select id,name from sources where exists (select null from actions where actions.source_id=sources.id) order by name", null, ids, null, stepSources, null);
                        c = db.rawQuery("select count(*) from actions where source_id=-1", null);
                        if (c != null) {
                            if (c.moveToFirst()) {
                                if (c.getInt(0) >= 1) {
                                    stepSources.add(0, getString(R.string.toFunction));
                                    ids.add(0, -1);
                                }
                            }
                            c.close();
                        }
                        selectedId = -1;
                        c = db.rawQuery("select source_id from actions where id = ?", new String[]{Integer.toString(((Step) datasetFrom.get(itemsFrom.get(0))).getActionId())});
                        if (c != null) {
                            if (c.moveToFirst()) selectedId = ids.indexOf(c.getInt(0));
                            c.close();
                        }

                        builder = new AlertDialog.Builder(context);
                        builder.setTitle(moveFlag ? getString(R.string.moveStep) : getString(R.string.copyStep));
                        builder.setSingleChoiceItems(stepSources.toArray(new String[stepSources.size()]), selectedId, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                selectedId = id;
                            }
                        });
                        builder.setPositiveButton(getString(R.string.next), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                copyRecord(moveFlag, context, datasetFrom, itemsFrom, tableFrom, ids.get(selectedId), "actions", -1);
                            }
                        });
                        builder.setNegativeButton(getString(R.string.cancel),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        appActionMode.finish();
                                        db.close();
                                    }
                                });
                        dialog = builder.create();
                        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                        dialog.show();

                        break;
                    default:
                        break;
                }
            } else if (tableShow.equals("actions")) {
                // nächster Assistentenschritt
                switch (tableFrom) {
                    case "actions":
                        final ArrayList<String> actions = new ArrayList<>();
                        ids = new ArrayList<>();
                        sortNrs = new ArrayList<>();
                        final boolean actionDestinationEqualsSource = ((Action) datasetFrom.get(itemsFrom.get(0))).getSourceId() == idShow;
                        DatabaseHandler.selectAsList(db, "select id,sort_nr,name from actions where source_id = ? order by sort_nr", new String[]{Integer.toString(idShow)}, ids, sortNrs, actions, null);
                        if (moveFlag && actionDestinationEqualsSource) {
                            // beim Verschieben innerhalb der Kopiequelle spielen die Quellaktionen als Angelpunkt keine Rolle
                            for (int pos : itemsFrom) {
                                int idx = ids.indexOf(((Action) datasetFrom.get(pos)).getId());
                                ids.remove(idx);
                                sortNrs.remove(idx);
                                actions.remove(idx);
                            }
                        }
                        actions.add(getString(R.string.insertLast));
                        ids.add(-1);
                        sortNrs.add(sortNrs.get(sortNrs.size() - 1) + 1);

                        builder = new AlertDialog.Builder(context);
                        builder.setTitle(moveFlag ? getString(R.string.moveAction) : getString(R.string.copyAction));
                        selectedId = actions.size() - 1;
                        builder.setSingleChoiceItems(actions.toArray(new String[actions.size()]), selectedId, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                selectedId = id;
                            }
                        });
                        builder.setPositiveButton(getString(R.string.next), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Action a;
                                int sortNr = sortNrs.get(selectedId);
                                int newActionId = -1;

                                if (itemsFrom.size() == 1) {
                                    // bei nur einer Aktion, die kopiert oder verschoben wird
                                    if (!moveFlag && actionDestinationEqualsSource) {
                                        // wenn sie in die selbe Quelle kopiert wird,
                                        // muss sie anders benannt werden
                                        copyRecord(false, context, datasetFrom, itemsFrom, tableFrom, idShow, "label", sortNr);
                                        return;
                                    } else if (!actionDestinationEqualsSource) {
                                        // wenn sie in eine andere Quelle kopiert oder verschoben wird,
                                        // muss sie bei Namenskollision anders benannt werden
                                        a = (Action) datasetFrom.get(itemsFrom.get(0));
                                        if (!a.getName().equals(DatabaseHandler.getUniqueCopiedActionName(activity, db, a.getName(), idShow))) {
                                            copyRecord(moveFlag, context, datasetFrom, itemsFrom, tableFrom, idShow, "label", sortNr);
                                            return;
                                        }
                                    }
                                }
                                for (int pos : itemsFrom) {
                                    // es kann immer eine Namenskollision auftreten, wenn mehrere Aktionen
                                    // in eine andere Quelle kopiert oder verschoben werden
                                    a = (Action) datasetFrom.get(pos);
                                    newActionId = DatabaseHandler.getNewId(db, "actions");
                                    copyAction(db, a.getId(), newActionId, sortNr++, DatabaseHandler.getUniqueCopiedActionName(activity, db, a.getName(), idShow), idShow, moveFlag);
                                }
                                appActionMode.finish();
                                db.close();
                                displaySection(activity, "SOURCE", idShow, null, newActionId);
                            }
                        });
                        builder.setNegativeButton(getString(R.string.cancel),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        appActionMode.finish();
                                        db.close();
                                    }
                                });
                        dialog = builder.create();
                        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                        dialog.show();

                        break;
                    case "steps":
                        final ArrayList<String> stepActions = new ArrayList<>();
                        ids = new ArrayList<>();
                        final ArrayList<Boolean> actionsWithoutSteps = new ArrayList<>();
                        DatabaseHandler.selectAsList(db, "select id,name,ifnull((select 0 from steps where steps.action_id=actions.id" + (moveFlag ? " and steps.id not in " + getInList(itemsFrom, datasetFrom) : "") + "),1) from actions where source_id = ? order by sort_nr", new String[]{Integer.toString(idShow)}, ids, null, stepActions, actionsWithoutSteps);

                        builder = new AlertDialog.Builder(context);
                        builder.setTitle(moveFlag ? getString(R.string.moveStep) : getString(R.string.copyStep));
                        selectedId = ids.indexOf(((Step) datasetFrom.get(itemsFrom.get(0))).getActionId());
                        builder.setSingleChoiceItems(stepActions.toArray(new String[stepActions.size()]), selectedId, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                Button posButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                                if (actionsWithoutSteps.get(id))
                                    posButton.setText(getString(R.string.insert));
                                else
                                    posButton.setText(getString(R.string.next));
                                selectedId = id;
                            }
                        });
                        builder.setPositiveButton(actionsWithoutSteps.get(selectedId) ? getString(R.string.insert) : getString(R.string.next), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (actionsWithoutSteps.get(selectedId)) {
                                    int newStepId = -1;
                                    int sortNr = 0;
                                    for (int pos : itemsFrom) {
                                        Step s = (Step) datasetFrom.get(pos);
                                        newStepId = DatabaseHandler.getNewId(db, "steps");
                                        copyStep(db, s.getId(), newStepId, sortNr++, s.getName(), s.getCallFlag(), -1, ids.get(selectedId), moveFlag);
                                    }
                                    appActionMode.finish();
                                    db.close();
                                    displaySection(activity, "ACTION", ids.get(selectedId), null, newStepId);
                                } else {
                                    copyRecord(moveFlag, context, datasetFrom, itemsFrom, tableFrom, ids.get(selectedId), "steps", -1);
                                }
                            }
                        });
                        builder.setNegativeButton(getString(R.string.cancel),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        appActionMode.finish();
                                        db.close();
                                    }
                                });
                        dialog = builder.create();
                        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                        dialog.show();

                        break;
                    default:
                        break;
                }
            } else if (tableShow.equals("steps")) {
                switch (tableFrom) {
                    case "steps":
                        final ArrayList<String> steps = new ArrayList<>();
                        ids = new ArrayList<>();
                        sortNrs = new ArrayList<>();
                        final boolean stepDestinationEqualsSource = ((Step) datasetFrom.get(itemsFrom.get(0))).getActionId() == idShow;

                        DatabaseHandler.selectAsList(db, "select id,sort_nr,function from steps where action_id = ? order by sort_nr", new String[]{Integer.toString(idShow)}, ids, sortNrs, steps, null);
                        if (moveFlag && stepDestinationEqualsSource) {
                            // beim Verschieben innerhalb der Kopiequelle spielen die Quellschritte als Angelpunkt keine Rolle
                            for (int pos : itemsFrom) {
                                int idx = ids.indexOf(((Step) datasetFrom.get(pos)).getId());
                                ids.remove(idx);
                                sortNrs.remove(idx);
                                steps.remove(idx);
                            }
                        }
                        steps.add(getString(R.string.insertLast));
                        ids.add(-1);
                        sortNrs.add(sortNrs.get(sortNrs.size() - 1) + 1);

                        builder = new AlertDialog.Builder(context);
                        builder.setTitle(moveFlag ? getString(R.string.moveStep) : getString(R.string.copyStep));
                        selectedId = steps.size() - 1;
                        builder.setSingleChoiceItems(steps.toArray(new String[steps.size()]), selectedId, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                selectedId = id;
                            }
                        });
                        builder.setPositiveButton(getString(R.string.next), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Step s;
                                int sortNr = sortNrs.get(selectedId);
                                int newStepId = -1;

                                for (int pos : itemsFrom) {
                                    s = (Step) datasetFrom.get(pos);
                                    newStepId = DatabaseHandler.getNewId(db, "steps");
                                    copyStep(db, s.getId(), newStepId, sortNr++, s.getName(), s.getCallFlag(), -1, idShow, moveFlag);
                                }
                                appActionMode.finish();
                                db.close();
                                displaySection(activity, "ACTION", idShow, null, newStepId);
                            }
                        });
                        builder.setNegativeButton(getString(R.string.cancel),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        appActionMode.finish();
                                        db.close();
                                    }
                                });
                        dialog = builder.create();
                        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                        dialog.show();

                        break;

                    default:
                        break;
                }

            } else if (tableShow.equals("label")) {
                switch (tableFrom) {
                    case "actions":
                        // einzelne Aktion wird in selbe Quelle kopiert und muss anders benannt werden
                        builder = new AlertDialog.Builder(context);
                        builder.setTitle(moveFlag ? getString(R.string.moveAction) : getString(R.string.copyAction));
                        builder.setMessage(getString(R.string.inputName));
                        final EditText input = createInput(context, false);
                        builder.setView(input);
                        final int pos = itemsFrom.get(0);
                        final Action a = (Action) datasetFrom.get(pos);
                        input.setText(DatabaseHandler.getUniqueCopiedActionName(activity, db, a.getName(), idShow));
                        input.setSelectAllOnFocus(true);
                        builder.setPositiveButton(getString(R.string.ok),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        int newActionId = DatabaseHandler.getNewId(db, "actions");
                                        copyAction(db, a.getId(), newActionId, sortNr, DatabaseHandler.getUniqueCopiedActionName(activity, db, input.getText().toString().trim(), sourceId), idShow, false);
                                        appActionMode.finish();
                                        db.close();
                                        displaySection(activity, "SOURCE", idShow, null, newActionId);
                                    }
                                });
                        builder.setNegativeButton(getString(R.string.cancel),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        appActionMode.finish();
                                        db.close();
                                    }
                                });
                        dialog = builder.create();
                        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                        dialog.show();

                        break;
                    default:
                        break;
                }
            }
        }

        private String getInList(List<Integer> itemsFrom, ArrayList datasetFrom) {
            String inList = "(";
            for (int pos : itemsFrom) {
                inList += ((UniqueRecord) datasetFrom.get(pos)).getId() + ",";
            }
            return inList.substring(0, inList.length() - 1) + ")";
        }

        private void copyStep(SQLiteDatabase db, int oldId, int newId, int sortNr, String function, boolean callFlag, int parentId, int actionId, boolean moveFlag) {
            Cursor cS, cP;
            ContentValues vals = new ContentValues();
            boolean exFlag = false;

            // beim Einfügen nachfolgende Sortiernummern inkrementieren
            cS = db.rawQuery("update steps set sort_nr=sort_nr+1 where action_id = ? and parent_id = ? and sort_nr >= ?", new String[]{Integer.toString(actionId), Integer.toString(parentId), Integer.toString(sortNr)});
            if (cS != null) {
                cS.moveToFirst();
                cS.close();
            }

            vals.put("id", newId);
            vals.put("action_id", actionId);
            vals.put("sort_nr", sortNr);
            vals.put("function", function);
            vals.put("call_flag", callFlag ? 1 : 0);
            vals.put("parent_id", parentId);
            try {
                db.insert("steps", null, vals);
            } catch (Exception ignored) {
                exFlag = true;
            }

            // den ganzen abhängigen Tabellenkram nachziehen (insert into actions ...)
            cP = db.rawQuery("select id, idx, value, variable_flag, list_flag from params where step_id = ?", new String[]{Integer.toString(oldId)});
            if (cP != null) {
                if (cP.moveToFirst()) {
                    do {
                        int newParamId = DatabaseHandler.getNewId(db, "params");
                        vals.clear();
                        vals.put("id", newParamId);
                        vals.put("step_id", newId);
                        vals.put("idx", cP.getInt(1));
                        vals.put("value", cP.getString(2));
                        vals.put("variable_flag", cP.getInt(3));
                        vals.put("list_flag", cP.getInt(4));
                        try {
                            db.insert("params", null, vals);
                        } catch (Exception ignored) {
                            exFlag = true;
                        }
                    }
                    while (cP.moveToNext());
                }
                cP.close();
            }

            if (!exFlag && moveFlag) {
                cS = db.rawQuery("delete from steps where id = ?", new String[]{Integer.toString(oldId)});
                if (cS != null) {
                    cS.moveToFirst();
                    cS.close();
                }
            }
        }

        private void copyAction(SQLiteDatabase db, int oldId, int newId, int sortNr, String name, int sourceId, boolean moveFlag) {
            Cursor cA, cS, cP;
            ContentValues vals = new ContentValues();
            boolean exFlag = false;

            // beim Einfügen nachfolgende Sortiernummern inkrementieren
            cA = db.rawQuery("update actions set sort_nr=sort_nr+1 where source_id = ? and sort_nr >= ?", new String[]{Integer.toString(sourceId), Integer.toString(sortNr)});
            if (cA != null) {
                cA.moveToFirst();
                cA.close();
            }

            vals.put("id", newId);
            vals.put("source_id", sourceId);
            vals.put("sort_nr", sortNr);
            vals.put("name", name);
            try {
                db.insert("actions", null, vals);
            } catch (Exception ignored) {
                exFlag = true;
            }

            // den ganzen abhängigen Tabellenkram nachziehen (insert into actions ...)
            cS = db.rawQuery("select id, sort_nr, function, call_flag, parent_id from steps where action_id = ?", new String[]{Integer.toString(oldId)});
            if (cS != null) {
                if (cS.moveToFirst()) {
                    do {
                        int newStepId = DatabaseHandler.getNewId(db, "steps");
                        vals.clear();
                        vals.put("id", newStepId);
                        vals.put("action_id", newId);
                        vals.put("sort_nr", cS.getInt(1));
                        vals.put("function", cS.getString(2));
                        vals.put("call_flag", cS.getInt(3));
                        vals.put("parent_id", cS.getInt(4));
                        try {
                            db.insert("steps", null, vals);
                        } catch (Exception ignored) {
                            exFlag = true;
                        }

                        cP = db.rawQuery("select id, idx, value, variable_flag, list_flag from params where step_id = ?", new String[]{Integer.toString(cS.getInt(0))});
                        if (cP != null) {
                            if (cP.moveToFirst()) {
                                do {
                                    int newParamId = DatabaseHandler.getNewId(db, "params");
                                    vals.clear();
                                    vals.put("id", newParamId);
                                    vals.put("step_id", newStepId);
                                    vals.put("idx", cP.getInt(1));
                                    vals.put("value", cP.getString(2));
                                    vals.put("variable_flag", cP.getInt(3));
                                    vals.put("list_flag", cP.getInt(4));
                                    try {
                                        db.insert("params", null, vals);
                                    } catch (Exception ignored) {
                                        exFlag = true;
                                    }
                                }
                                while (cP.moveToNext());
                            }
                            cP.close();
                        }
                    } while (cS.moveToNext());
                }
                cS.close();
            }
            if (!exFlag && moveFlag) {
                cA = db.rawQuery("delete from actions where id = ?", new String[]{Integer.toString(oldId)});
                if (cA != null) {
                    cA.moveToFirst();
                    cA.close();
                }
            }
        }

        private void copySource(SQLiteDatabase db, int oldId, int newId, String name) {
            Cursor cA, cS, cP;
            ContentValues vals = new ContentValues();

            vals.put("id", newId);
            vals.put("name", name);
            try {
                db.insert("sources", null, vals);
            } catch (Exception ignored) {
            }

            // den ganzen abhängigen Tabellenkram nachziehen (insert into actions ...)
            cA = db.rawQuery("select id, sort_nr, name from actions where source_id = ?", new String[]{Integer.toString(oldId)});
            if (cA != null) {
                if (cA.moveToFirst()) {
                    do {
                        int newActionId = DatabaseHandler.getNewId(db, "actions");
                        vals.clear();
                        vals.put("id", newActionId);
                        vals.put("source_id", newId);
                        vals.put("sort_nr", cA.getInt(1));
                        vals.put("name", cA.getString(2));
                        try {
                            db.insert("actions", null, vals);
                        } catch (Exception ignored) {
                        }

                        cS = db.rawQuery("select id, sort_nr, function, call_flag, parent_id from steps where action_id = ?", new String[]{Integer.toString(cA.getInt(0))});
                        if (cS != null) {
                            if (cS.moveToFirst()) {
                                do {
                                    int newStepId = DatabaseHandler.getNewId(db, "steps");
                                    vals.clear();
                                    vals.put("id", newStepId);
                                    vals.put("action_id", newActionId);
                                    vals.put("sort_nr", cS.getInt(1));
                                    vals.put("function", cS.getString(2));
                                    vals.put("call_flag", cS.getInt(3));
                                    vals.put("parent_id", cS.getInt(4));
                                    try {
                                        db.insert("steps", null, vals);
                                    } catch (Exception ignored) {
                                    }

                                    cP = db.rawQuery("select id, idx, value, variable_flag, list_flag from params where step_id = ?", new String[]{Integer.toString(cS.getInt(0))});
                                    if (cP != null) {
                                        if (cP.moveToFirst()) {
                                            do {
                                                int newParamId = DatabaseHandler.getNewId(db, "params");
                                                vals.clear();
                                                vals.put("id", newParamId);
                                                vals.put("step_id", newStepId);
                                                vals.put("idx", cP.getInt(1));
                                                vals.put("value", cP.getString(2));
                                                vals.put("variable_flag", cP.getInt(3));
                                                vals.put("list_flag", cP.getInt(4));
                                                try {
                                                    db.insert("params", null, vals);
                                                } catch (Exception ignored) {
                                                }
                                            }
                                            while (cP.moveToNext());
                                        }
                                        cP.close();
                                    }
                                } while (cS.moveToNext());
                            }
                            cS.close();
                        }
                    } while (cA.moveToNext());
                }
                cA.close();
            }
        }

        private ArrayList<UniqueRecord> getRecordsFromSelection(ArrayList dataset, List<Integer> items) {
            ArrayList<UniqueRecord> lst = new ArrayList<>(items.size());
            for (int i : items)
                lst.add((UniqueRecord) dataset.get(i));
            return lst;
        }

        protected EditText createInput(Context context, boolean numericOnly) {
            EditText input = new EditText(context);
            input.setSingleLine(true);
            if (numericOnly)
                input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            return input;
        }

        private interface ClickListener {
            void onItemClicked(int position);

            boolean onItemLongClicked(int position);
        }

        private interface ItemTouchHelperAdapter {

            /**
             * Called when an item has been dragged far enough to trigger a move. This is called every time
             * an item is shifted, and <strong>not</strong> at the end of a "drop" event.<br/>
             * <br/>
             * Implementations should call {@link RecyclerView.Adapter#notifyItemMoved(int, int)} after
             * adjusting the underlying data to reflect this move.
             *
             * @param fromPosition The start position of the moved item.
             * @param toPosition   Then resolved position of the moved item.
             * @see RecyclerView#getAdapterPositionFor(RecyclerView.ViewHolder)
             * @see RecyclerView.ViewHolder#getAdapterPosition()
             */
            void onItemMove(int fromPosition, int toPosition);


            /**
             * Called when an item has been dismissed by a swipe.<br/>
             * <br/>
             * Implementations should call {@link RecyclerView.Adapter#notifyItemRemoved(int)} after
             * adjusting the underlying data to reflect this removal.
             *
             * @param position The position of the item dismissed.
             * @see RecyclerView#getAdapterPositionFor(RecyclerView.ViewHolder)
             * @see RecyclerView.ViewHolder#getAdapterPosition()
             */
            void onItemDismiss(int position);
        }

        private class ItemTouchHelperCallback extends ItemTouchHelper.Callback {

            private final ItemTouchHelperAdapter mAdapter;

            public ItemTouchHelperCallback(ItemTouchHelperAdapter adapter) {
                mAdapter = adapter;
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }

            @Override
            public boolean isItemViewSwipeEnabled() {
                return true;
            }

            @Override
            public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                // ToDo: hier je nach ViewHolder einschreiten, wenn nicht drag- oder swipebar
                int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
                return makeMovementFlags(dragFlags, swipeFlags);
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                mAdapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }

            @Override
            public void onMoved(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, int fromPos, RecyclerView.ViewHolder target, int toPos, int x, int y) {
                SQLiteDatabase db = DatabaseHandler.getInstance(getActivity()).getWritableDatabase();
                Cursor c;

                if (activeSection.equals("ACTIONS")) {
                    ArrayList<Action> list = ((ActionAdapter) mAdapter).mDataset;
                    for (int i = Math.min(fromPos, toPos); i <= Math.max(fromPos, toPos); i++) {
                        c = db.rawQuery("update actions set sort_nr = ? where id = ?", new String[]{Integer.toString(i), Integer.toString(list.get(i).getId())});
                        c.moveToFirst();
                        c.close();
                    }
                } else if (activeSection.equals("STEPS")) {
                    ArrayList<Step> list = ((StepAdapter) mAdapter).mDataset;
                    for (int i = Math.min(fromPos, toPos); i <= Math.max(fromPos, toPos); i++) {
                        c = db.rawQuery("update steps set sort_nr = ? where id = ?", new String[]{Integer.toString(i), Integer.toString(list.get(i).getId())});
                        c.moveToFirst();
                        c.close();
                    }
                }
                db.close();
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                mAdapter.onItemDismiss(viewHolder.getAdapterPosition());
            }
        }

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        protected class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
            // each data item is just a string in this case
            public TextView mTextView;
            private ClickListener listener;

            public ViewHolder(TextView v, ClickListener listener) {
                super(v);
                v.setFocusable(true);
                v.setClickable(true);
                v.setLongClickable(true);
                v.setOnClickListener(this);
                v.setOnLongClickListener(this);
                mTextView = (TextView) v.findViewById(R.id.my_text_view);
                this.listener = listener;
            }

            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onItemClicked(getAdapterPosition());
                }
            }

            @Override
            public boolean onLongClick(View v) {
                return listener != null && listener.onItemLongClicked(getAdapterPosition());
            }
        }

        private class ActionAdapter extends SelectableAdapter<ViewHolder> implements ItemTouchHelperAdapter, ClickListener, ActionMode.Callback {
            public ArrayList<Action> mDataset;
            private Menu menu;

            // Provide a suitable constructor (depends on the kind of dataset)
            public ActionAdapter(ArrayList<Action> myDataset) {
                mDataset = myDataset;
            }

            // Create new views (invoked by the layout manager)
            @Override
            public ViewHolder onCreateViewHolder(ViewGroup parent,
                                                 int viewType) {
                // create a new view
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.view_main, parent, false);
                // set the view's size, margins, paddings and layout parameters
                return new ViewHolder((TextView) v, this);
            }

            // Replace the contents of a view (invoked by the layout manager)
            @Override
            public void onBindViewHolder(ViewHolder holder, int position) {
                // - get element from your dataset at this position
                // - replace the contents of the view with that element
                if (mDataset.get(position).getId() == -1) {
                    holder.mTextView.setText(Html.fromHtml("<i>" + mDataset.get(position).getName() + "</i>"));
                } else
                    holder.mTextView.setText(mDataset.get(position).getName());
                // Highlight the item if it's selected
                holder.itemView.setSelected(isSelected(position));
            }

            // Return the size of your dataset (invoked by the layout manager)
            @Override
            public int getItemCount() {
                return mDataset.size();
            }

            @Override
            public void onItemClicked(int position) {
                if (appActionMode == null) {
                    Intent intentUpdate = new Intent();
                    intentUpdate.setAction("com.ha81dn.webausleser.ASYNC_MAIN");
                    intentUpdate.addCategory(Intent.CATEGORY_DEFAULT);
                    intentUpdate.putExtra("TAPITEM", "ACTION");
                    intentUpdate.putExtra("ID", mDataset.get(position).getId());
                    intentUpdate.putExtra("NAME", mDataset.get(position).getName());
                    getActivity().sendBroadcast(intentUpdate);
                } else {
                    toggleSelection(position);
                }
            }

            @Override
            public void toggledSelection(int position) {
                if (appActionMode != null) {
                    int count = getSelectedItemCount();
                    if (count == 0) {
                        appActionMode.finish();
                    } else {
                        if (count >= 2)
                            menu.findItem(R.id.menu_item_rename).setEnabled(false);
                        else
                            menu.findItem(R.id.menu_item_rename).setEnabled(true);
                        if (count == mDataset.size())
                            menu.findItem(R.id.menu_item_select_all).setEnabled(false);
                        else
                            menu.findItem(R.id.menu_item_select_all).setEnabled(true);
                        appActionMode.setTitle(String.valueOf(count));
                        appActionMode.invalidate();
                    }
                }
            }

            @Override
            public boolean onItemLongClicked(int position) {
                if (appActionMode == null) {
                    appActionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(this);
                }
                toggleSelection(position);
                return true;
            }

            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                getActivity().getMenuInflater().inflate(R.menu.list_item_context, menu);
                this.menu = menu;
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
                clearSelection();
                appActionMode = null;
                menu = null;
            }

            @Override
            public boolean onActionItemClicked(final ActionMode actionMode, MenuItem menuItem) {
                if (getSelectedItemCount() == 0) return false;
                AlertDialog.Builder builder;
                final Context context = getActivity();

                switch (menuItem.getItemId()) {
                    case R.id.menu_item_rename:
                        builder = new AlertDialog.Builder(context);
                        builder.setTitle(getString(R.string.renameAction));
                        builder.setMessage(getString(R.string.inputName));
                        final EditText input = createInput(context, false);
                        builder.setView(input);
                        final int pos = getSelectedItems().get(0);
                        final Action a = mDataset.get(pos);
                        input.setText(a.getName());
                        input.setSelectAllOnFocus(true);
                        builder.setPositiveButton(getString(R.string.ok),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        SQLiteDatabase db = DatabaseHandler.getInstance(context).getWritableDatabase();
                                        Cursor c;
                                        String name = input.getText().toString().trim();

                                        c = db.rawQuery("update actions set name = ? where id = ?", new String[]{name, Integer.toString(a.getId())});
                                        if (c != null) {
                                            c.moveToFirst();
                                            c.close();
                                            a.setName(name);
                                            notifyItemChanged(pos);
                                        }
                                        actionMode.finish();
                                    }
                                });
                        builder.setNegativeButton(getString(R.string.cancel),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        actionMode.finish();
                                    }
                                });
                        AlertDialog dialog = builder.create();
                        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                        dialog.show();

                        return true;

                    case R.id.menu_item_select_all:
                        selectAll(mDataset.size());

                        return true;

                    case R.id.menu_item_copy:
                        copyRecord(false, context, mDataset, getSelectedItems(), "actions", -1, null, -1);

                        return true;

                    case R.id.menu_item_move:
                        copyRecord(true, context, mDataset, getSelectedItems(), "actions", -1, null, -1);

                        return true;

                    case R.id.menu_item_delete:
                        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case DialogInterface.BUTTON_POSITIVE:
                                        //Yes button clicked
                                        SQLiteDatabase db = DatabaseHandler.getInstance(context).getWritableDatabase();
                                        Cursor c;

                                        ArrayList<UniqueRecord> items = getRecordsFromSelection(mDataset, getSelectedItems());
                                        for (UniqueRecord ur : items) {
                                            c = db.rawQuery("delete from actions where id = ?", new String[]{Integer.toString(ur.getId())});
                                            if (c != null) {
                                                c.moveToFirst();
                                                c.close();
                                            }
                                        }
                                        // noinspection SuspiciousMethodCalls
                                        mDataset.removeAll(items);

                                        db.close();

                                        mAdapter.notifyDataSetChanged();
                                        break;

                                    case DialogInterface.BUTTON_NEGATIVE:
                                        //No button clicked
                                        break;
                                }
                                actionMode.finish();
                            }
                        };

                        builder = new AlertDialog.Builder(context);
                        builder.setMessage(getString(R.string.sureActions))
                                .setPositiveButton(getString(R.string.yes), dialogClickListener)
                                .setNegativeButton(getString(R.string.no), dialogClickListener).show();

                        return true;
                    default:
                        break;
                }
                return false;
            }

            @Override
            public void onItemDismiss(final int position) {
                final int removedId = mDataset.get(position).getId();
                final boolean wasSelected = isSelected(position);

                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SQLiteDatabase db = DatabaseHandler.getInstance(getActivity()).getWritableDatabase();
                        Cursor c = null;
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                // Yes button clicked

                                c = db.rawQuery("delete from actions where id = ?", new String[]{Integer.toString(removedId)});
                                if (c != null) c.moveToFirst();

                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                // No button clicked
                                c = db.rawQuery("select source_id, sort_nr, name from actions where id = ?", new String[]{Integer.toString(removedId)});
                                if (c != null) {
                                    if (c.moveToFirst()) {
                                        mDataset.add(position, new Action(removedId, c.getInt(0), c.getInt(1), c.getString(2)));
                                    }
                                }
                                undoReorgAfterDismiss(position, mDataset.size() - 1, wasSelected);
                                notifyItemInserted(position);

                                break;
                        }
                        if (c != null) c.close();
                        db.close();
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(getString(R.string.sureAction, mDataset.get(position).getName()))
                        .setPositiveButton(getString(R.string.yes), dialogClickListener)
                        .setNegativeButton(getString(R.string.no), dialogClickListener).show();

                mDataset.remove(position);
                reorgAfterDismiss(position, mDataset.size());
                notifyItemRemoved(position);
            }

            @Override
            public void onItemMove(int from, int to) {
                Collections.swap(mDataset, from, to);
                if (appActionMode != null) reorgAfterMove(from, to);
                notifyItemMoved(from, to);
            }
        }

        private class FunctionAdapter extends SelectableAdapter<ViewHolder> implements ItemTouchHelperAdapter, ClickListener, ActionMode.Callback {
            public ArrayList<Action> mDataset;
            private Menu menu;

            // Provide a suitable constructor (depends on the kind of dataset)
            public FunctionAdapter(ArrayList<Action> myDataset) {
                mDataset = myDataset;
            }

            // Create new views (invoked by the layout manager)
            @Override
            public ViewHolder onCreateViewHolder(ViewGroup parent,
                                                 int viewType) {
                // create a new view
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.view_main, parent, false);
                // set the view's size, margins, paddings and layout parameters
                return new ViewHolder((TextView) v, this);
            }

            // Replace the contents of a view (invoked by the layout manager)
            @Override
            public void onBindViewHolder(ViewHolder holder, int position) {
                // - get element from your dataset at this position
                // - replace the contents of the view with that element
                if (mDataset.get(position).getId() == -1) {
                    holder.mTextView.setText(Html.fromHtml("<i>" + mDataset.get(position).getName() + "</i>"));
                } else
                    holder.mTextView.setText(mDataset.get(position).getName());
                // Highlight the item if it's selected
                holder.itemView.setSelected(isSelected(position));
            }

            // Return the size of your dataset (invoked by the layout manager)
            @Override
            public int getItemCount() {
                return mDataset.size();
            }

            @Override
            public void onItemClicked(int position) {
                if (appActionMode == null) {
                    Intent intentUpdate = new Intent();
                    intentUpdate.setAction("com.ha81dn.webausleser.ASYNC_MAIN");
                    intentUpdate.addCategory(Intent.CATEGORY_DEFAULT);
                    intentUpdate.putExtra("TAPITEM", "FUNCTION");
                    intentUpdate.putExtra("ID", mDataset.get(position).getId());
                    intentUpdate.putExtra("NAME", mDataset.get(position).getName());
                    getActivity().sendBroadcast(intentUpdate);
                } else {
                    toggleSelection(position);
                }
            }

            @Override
            public void toggledSelection(int position) {
                if (appActionMode != null) {
                    int count = getSelectedItemCount();
                    if (count == 0) {
                        appActionMode.finish();
                    } else {
                        if (count >= 2)
                            menu.findItem(R.id.menu_item_rename).setEnabled(false);
                        else
                            menu.findItem(R.id.menu_item_rename).setEnabled(true);
                        appActionMode.setTitle(String.valueOf(count));
                        appActionMode.invalidate();
                    }
                }
            }

            @Override
            public boolean onItemLongClicked(int position) {
                if (appActionMode == null) {
                    appActionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(this);
                }
                toggleSelection(position);
                return true;
            }

            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                getActivity().getMenuInflater().inflate(R.menu.list_item_context, menu);
                menu.findItem(R.id.menu_item_move).setVisible(false);
                this.menu = menu;
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
                clearSelection();
                appActionMode = null;
                menu = null;
            }

            @Override
            public boolean onActionItemClicked(final ActionMode actionMode, MenuItem menuItem) {
                if (getSelectedItemCount() == 0) return false;
                AlertDialog.Builder builder;
                final Context context = getActivity();

                switch (menuItem.getItemId()) {
                    case R.id.menu_item_rename:
                        builder = new AlertDialog.Builder(context);
                        builder.setTitle(getString(R.string.renameFunction));
                        builder.setMessage(getString(R.string.inputName));
                        final EditText input = createInput(context, false);
                        builder.setView(input);
                        final int pos = getSelectedItems().get(0);
                        final Action a = mDataset.get(pos);
                        input.setText(a.getName());
                        input.setSelectAllOnFocus(true);
                        builder.setPositiveButton(getString(R.string.ok),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        SQLiteDatabase db = DatabaseHandler.getInstance(context).getWritableDatabase();
                                        Cursor c;
                                        String name = input.getText().toString().trim();

                                        c = db.rawQuery("update actions set name = ? where id = ?", new String[]{name, Integer.toString(a.getId())});
                                        if (c != null) {
                                            c.moveToFirst();
                                            c.close();
                                            a.setName(name);
                                            notifyItemChanged(pos);
                                        }
                                        actionMode.finish();
                                    }
                                });
                        builder.setNegativeButton(getString(R.string.cancel),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        actionMode.finish();
                                    }
                                });
                        AlertDialog dialog = builder.create();
                        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                        dialog.show();

                        return true;

                    case R.id.menu_item_select_all:
                        selectAll(mDataset.size());

                        return true;
                    case R.id.menu_item_delete:
                        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case DialogInterface.BUTTON_POSITIVE:
                                        //Yes button clicked
                                        SQLiteDatabase db = DatabaseHandler.getInstance(context).getWritableDatabase();
                                        Cursor c;

                                        ArrayList<UniqueRecord> items = getRecordsFromSelection(mDataset, getSelectedItems());
                                        for (UniqueRecord ur : items) {
                                            c = db.rawQuery("delete from actions where id = ?", new String[]{Integer.toString(ur.getId())});
                                            if (c != null) {
                                                c.moveToFirst();
                                                c.close();
                                            }
                                        }
                                        // noinspection SuspiciousMethodCalls
                                        mDataset.removeAll(items);

                                        db.close();

                                        mAdapter.notifyDataSetChanged();
                                        break;

                                    case DialogInterface.BUTTON_NEGATIVE:
                                        //No button clicked
                                        break;
                                }
                                actionMode.finish();
                            }
                        };

                        builder = new AlertDialog.Builder(context);
                        builder.setMessage(getString(R.string.sureFunctions))
                                .setPositiveButton(getString(R.string.yes), dialogClickListener)
                                .setNegativeButton(getString(R.string.no), dialogClickListener).show();

                        return true;
                    default:
                        break;
                }
                return false;
            }

            @Override
            public void onItemDismiss(final int position) {
                final int removedId = mDataset.get(position).getId();
                final boolean wasSelected = isSelected(position);

                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SQLiteDatabase db = DatabaseHandler.getInstance(getActivity()).getWritableDatabase();
                        Cursor c = null;
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                // Yes button clicked

                                c = db.rawQuery("delete from actions where id = ?", new String[]{Integer.toString(removedId)});
                                if (c != null) c.moveToFirst();

                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                // No button clicked
                                c = db.rawQuery("select source_id, sort_nr, name from actions where id = ?", new String[]{Integer.toString(removedId)});
                                if (c != null) {
                                    if (c.moveToFirst()) {
                                        mDataset.add(position, new Action(removedId, c.getInt(0), c.getInt(1), c.getString(2)));
                                    }
                                }
                                undoReorgAfterDismiss(position, mDataset.size() - 1, wasSelected);
                                notifyItemInserted(position);

                                break;
                        }
                        if (c != null) c.close();
                        db.close();
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(getString(R.string.sureFunction, mDataset.get(position).getName()))
                        .setPositiveButton(getString(R.string.yes), dialogClickListener)
                        .setNegativeButton(getString(R.string.no), dialogClickListener).show();

                mDataset.remove(position);
                reorgAfterDismiss(position, mDataset.size());
                notifyItemRemoved(position);
            }

            @Override
            public void onItemMove(int from, int to) {
                Collections.swap(mDataset, from, to);
                if (appActionMode != null) reorgAfterMove(from, to);
                notifyItemMoved(from, to);
            }
        }

        private class ParamAdapter extends SelectableAdapter<ViewHolder> implements ItemTouchHelperAdapter, ClickListener {
            public ArrayList<Param> mDataset;

            // Provide a suitable constructor (depends on the kind of dataset)
            public ParamAdapter(ArrayList<Param> myDataset) {
                mDataset = myDataset;
            }

            // Create new views (invoked by the layout manager)
            @Override
            public ViewHolder onCreateViewHolder(ViewGroup parent,
                                                 int viewType) {
                // create a new view
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.view_main, parent, false);
                // set the view's size, margins, paddings and layout parameters
                return new ViewHolder((TextView) v, this);
            }

            // Replace the contents of a view (invoked by the layout manager)
            @Override
            public void onBindViewHolder(ViewHolder holder, int position) {
                // - get element from your dataset at this position
                // - replace the contents of the view with that element
                holder.mTextView.setText(mDataset.get(position).getValue());
            }

            // Return the size of your dataset (invoked by the layout manager)
            @Override
            public int getItemCount() {
                return mDataset.size();
            }

            @Override
            public void onItemClicked(int position) {
                Intent intentUpdate = new Intent();
                intentUpdate.setAction("com.ha81dn.webausleser.ASYNC_MAIN");
                intentUpdate.addCategory(Intent.CATEGORY_DEFAULT);
                intentUpdate.putExtra("TAPITEM", "PARAM");
                intentUpdate.putExtra("ID", mDataset.get(position).getId());
                intentUpdate.putExtra("NAME", mDataset.get(position).getValue());
                getActivity().sendBroadcast(intentUpdate);
            }

            @Override
            public boolean onItemLongClicked(int position) {
                return false;
            }

            @Override
            public void onItemDismiss(final int position) {
            }

            @Override
            public void onItemMove(int from, int to) {
            }
        }

        private class SourceAdapter extends SelectableAdapter<ViewHolder> implements ItemTouchHelperAdapter, ClickListener, ActionMode.Callback {
            public ArrayList<Source> mDataset;
            private Menu menu;

            // Provide a suitable constructor (depends on the kind of dataset)
            public SourceAdapter(ArrayList<Source> myDataset) {
                mDataset = myDataset;
            }

            // Create new views (invoked by the layout manager)
            @Override
            public ViewHolder onCreateViewHolder(ViewGroup parent,
                                                 int viewType) {
                // create a new view
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.view_main, parent, false);
                // set the view's size, margins, paddings and layout parameters
                return new ViewHolder((TextView) v, this);
            }

            // Replace the contents of a view (invoked by the layout manager)
            @Override
            public void onBindViewHolder(ViewHolder holder, int position) {
                // - get element from your dataset at this position
                // - replace the contents of the view with that element
                if (mDataset.get(position).getId() == -1) {
                    holder.mTextView.setText(Html.fromHtml("<i>" + mDataset.get(position).getName() + "</i>"));
                } else
                    holder.mTextView.setText(mDataset.get(position).getName());
                // Highlight the item if it's selected
                holder.itemView.setSelected(isSelected(position));
            }

            // Return the size of your dataset (invoked by the layout manager)
            @Override
            public int getItemCount() {
                return mDataset.size();
            }

            @Override
            public void onItemClicked(int position) {
                if (appActionMode == null) {
                    Intent intentUpdate = new Intent();
                    intentUpdate.setAction("com.ha81dn.webausleser.ASYNC_MAIN");
                    intentUpdate.addCategory(Intent.CATEGORY_DEFAULT);
                    intentUpdate.putExtra("TAPITEM", "SOURCE");
                    intentUpdate.putExtra("ID", mDataset.get(position).getId());
                    intentUpdate.putExtra("NAME", mDataset.get(position).getName());
                    getActivity().sendBroadcast(intentUpdate);
                } else {
                    toggleSelection(position);
                }
            }

            @Override
            public void toggledSelection(int position) {
                if (appActionMode != null) {
                    int count = getSelectedItemCount();
                    if (count == 0) {
                        appActionMode.finish();
                    } else {
                        if (count >= 2)
                            menu.findItem(R.id.menu_item_rename).setEnabled(false);
                        else
                            menu.findItem(R.id.menu_item_rename).setEnabled(true);
                        if (count == mDataset.size())
                            menu.findItem(R.id.menu_item_select_all).setEnabled(false);
                        else
                            menu.findItem(R.id.menu_item_select_all).setEnabled(true);
                        appActionMode.setTitle(String.valueOf(count));
                        appActionMode.invalidate();
                    }
                }
            }

            @Override
            public boolean onItemLongClicked(int position) {
                if (appActionMode == null) {
                    appActionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(this);
                }
                toggleSelection(position);
                return true;
            }

            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                getActivity().getMenuInflater().inflate(R.menu.list_item_context, menu);
                menu.findItem(R.id.menu_item_move).setVisible(false);
                this.menu = menu;
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
                clearSelection();
                appActionMode = null;
                menu = null;
            }

            @Override
            public boolean onActionItemClicked(final ActionMode actionMode, MenuItem menuItem) {
                if (getSelectedItemCount() == 0) return false;
                AlertDialog.Builder builder;
                final Context context = getActivity();

                switch (menuItem.getItemId()) {
                    case R.id.menu_item_rename:
                        builder = new AlertDialog.Builder(context);
                        builder.setTitle(getString(R.string.renameSource));
                        builder.setMessage(getString(R.string.inputName));
                        final EditText input = createInput(context, false);
                        builder.setView(input);
                        final int pos = getSelectedItems().get(0);
                        final Source s = mDataset.get(pos);
                        input.setText(s.getName());
                        input.setSelectAllOnFocus(true);
                        builder.setPositiveButton(getString(R.string.ok),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        SQLiteDatabase db = DatabaseHandler.getInstance(context).getWritableDatabase();
                                        Cursor c;
                                        String name = input.getText().toString().trim();

                                        c = db.rawQuery("update sources set name = ? where id = ?", new String[]{name, Integer.toString(s.getId())});
                                        if (c != null) {
                                            c.moveToFirst();
                                            c.close();
                                            s.setName(name);
                                            notifyItemChanged(pos);
                                        }
                                        actionMode.finish();
                                    }
                                });
                        builder.setNegativeButton(getString(R.string.cancel),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        actionMode.finish();
                                    }
                                });
                        AlertDialog dialog = builder.create();
                        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                        dialog.show();

                        return true;

                    case R.id.menu_item_select_all:
                        selectAll(mDataset.size());

                        return true;

                    case R.id.menu_item_copy:
                        copyRecord(false, context, mDataset, getSelectedItems(), "sources", -1, null, -1);

                        return true;

                    case R.id.menu_item_delete:
                        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case DialogInterface.BUTTON_POSITIVE:
                                        //Yes button clicked
                                        SQLiteDatabase db = DatabaseHandler.getInstance(context).getWritableDatabase();
                                        Cursor c;

                                        ArrayList<UniqueRecord> items = getRecordsFromSelection(mDataset, getSelectedItems());
                                        for (UniqueRecord ur : items) {
                                            c = db.rawQuery("delete from sources where id = ?", new String[]{Integer.toString(ur.getId())});
                                            if (c != null) {
                                                c.moveToFirst();
                                                c.close();
                                            }
                                        }
                                        // noinspection SuspiciousMethodCalls
                                        mDataset.removeAll(items);

                                        db.close();

                                        mAdapter.notifyDataSetChanged();
                                        break;

                                    case DialogInterface.BUTTON_NEGATIVE:
                                        //No button clicked
                                        break;
                                }
                                actionMode.finish();
                            }
                        };

                        builder = new AlertDialog.Builder(context);
                        builder.setMessage(getString(R.string.sureSources))
                                .setPositiveButton(getString(R.string.yes), dialogClickListener)
                                .setNegativeButton(getString(R.string.no), dialogClickListener).show();

                        return true;
                    default:
                        break;
                }
                return false;
            }

            @Override
            public void onItemDismiss(final int position) {
                final int removedId = mDataset.get(position).getId();
                final boolean wasSelected = isSelected(position);

                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SQLiteDatabase db = DatabaseHandler.getInstance(getActivity()).getWritableDatabase();
                        Cursor c = null;
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                // Yes button clicked

                                c = db.rawQuery("delete from sources where id = ?", new String[]{Integer.toString(removedId)});
                                if (c != null) c.moveToFirst();

                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                // No button clicked
                                c = db.rawQuery("select name from sources where id = ?", new String[]{Integer.toString(removedId)});
                                if (c != null) {
                                    if (c.moveToFirst()) {
                                        mDataset.add(position, new Source(removedId, c.getString(0)));
                                    }
                                }
                                undoReorgAfterDismiss(position, mDataset.size() - 1, wasSelected);
                                notifyItemInserted(position);

                                break;
                        }
                        if (c != null) c.close();
                        db.close();
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(getString(R.string.sureSource, mDataset.get(position).getName()))
                        .setPositiveButton(getString(R.string.yes), dialogClickListener)
                        .setNegativeButton(getString(R.string.no), dialogClickListener).show();

                if (getSelectedItems().contains(position)) toggleSelection(position);
                mDataset.remove(position);
                notifyItemRemoved(position);
            }

            @Override
            public void onItemMove(int from, int to) {
                // muss die Methode implementieren, darf aber nicht verschoben werden
            }
        }

        private class StepAdapter extends SelectableAdapter<ViewHolder> implements ItemTouchHelperAdapter, ClickListener, ActionMode.Callback {
            public ArrayList<Step> mDataset;
            private Menu menu;

            // Provide a suitable constructor (depends on the kind of dataset)
            public StepAdapter(ArrayList<Step> myDataset) {
                mDataset = myDataset;
            }

            // Create new views (invoked by the layout manager)
            @Override
            public ViewHolder onCreateViewHolder(ViewGroup parent,
                                                 int viewType) {
                // create a new view
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.view_main, parent, false);
                // set the view's size, margins, paddings and layout parameters
                return new ViewHolder((TextView) v, this);
            }

            // Replace the contents of a view (invoked by the layout manager)
            @Override
            public void onBindViewHolder(ViewHolder holder, int position) {
                // - get element from your dataset at this position
                // - replace the contents of the view with that element
                if (mDataset.get(position).getId() == -1) {
                    holder.mTextView.setText(Html.fromHtml("<i>" + mDataset.get(position).getName() + "</i>"));
                } else
                    holder.mTextView.setText(mDataset.get(position).getName());
                // Highlight the item if it's selected
                holder.itemView.setSelected(isSelected(position));
            }

            // Return the size of your dataset (invoked by the layout manager)
            @Override
            public int getItemCount() {
                return mDataset.size();
            }

            @Override
            public void onItemClicked(int position) {
                if (appActionMode == null) {
                    Intent intentUpdate = new Intent();
                    intentUpdate.setAction("com.ha81dn.webausleser.ASYNC_MAIN");
                    intentUpdate.addCategory(Intent.CATEGORY_DEFAULT);
                    intentUpdate.putExtra("TAPITEM", "STEP");
                    intentUpdate.putExtra("ID", mDataset.get(position).getId());
                    intentUpdate.putExtra("NAME", mDataset.get(position).getName());
                    getActivity().sendBroadcast(intentUpdate);
                } else {
                    toggleSelection(position);
                }
            }

            @Override
            public void toggledSelection(int position) {
                if (appActionMode != null) {
                    int count = getSelectedItemCount();
                    if (count == 0) {
                        appActionMode.finish();
                    } else {
                        if (count == mDataset.size())
                            menu.findItem(R.id.menu_item_select_all).setEnabled(false);
                        else
                            menu.findItem(R.id.menu_item_select_all).setEnabled(true);
                        appActionMode.setTitle(String.valueOf(count));
                        appActionMode.invalidate();
                    }
                }
            }

            @Override
            public boolean onItemLongClicked(int position) {
                if (appActionMode == null) {
                    appActionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(this);
                }
                toggleSelection(position);
                return true;
            }

            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                getActivity().getMenuInflater().inflate(R.menu.list_item_context, menu);
                menu.findItem(R.id.menu_item_rename).setVisible(false);
                this.menu = menu;
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
                Log.d("DESTROY", Integer.toString(getSelectedItemCount()));
                clearSelection();
                appActionMode = null;
                menu = null;
            }

            @Override
            public boolean onActionItemClicked(final ActionMode actionMode, MenuItem menuItem) {
                if (getSelectedItemCount() == 0) return false;
                final Context context = getActivity();

                switch (menuItem.getItemId()) {
                    case R.id.menu_item_select_all:
                        selectAll(mDataset.size());

                        return true;
                    case R.id.menu_item_copy:
                        copyRecord(false, context, mDataset, getSelectedItems(), "steps", -1, null, -1);

                        return true;

                    case R.id.menu_item_move:
                        copyRecord(true, context, mDataset, getSelectedItems(), "steps", -1, null, -1);

                        return true;

                    case R.id.menu_item_delete:
                        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case DialogInterface.BUTTON_POSITIVE:
                                        //Yes button clicked
                                        SQLiteDatabase db = DatabaseHandler.getInstance(getActivity()).getWritableDatabase();
                                        Cursor c;

                                        ArrayList<UniqueRecord> items = getRecordsFromSelection(mDataset, getSelectedItems());
                                        for (UniqueRecord ur : items) {
                                            c = db.rawQuery("delete from steps where id = ?", new String[]{Integer.toString(ur.getId())});
                                            if (c != null) {
                                                c.moveToFirst();
                                                c.close();
                                            }
                                        }
                                        // noinspection SuspiciousMethodCalls
                                        mDataset.removeAll(items);

                                        db.close();

                                        mAdapter.notifyDataSetChanged();
                                        break;

                                    case DialogInterface.BUTTON_NEGATIVE:
                                        //No button clicked
                                        break;
                                }
                                actionMode.finish();
                            }
                        };

                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setMessage(getString(R.string.sureSteps))
                                .setPositiveButton(getString(R.string.yes), dialogClickListener)
                                .setNegativeButton(getString(R.string.no), dialogClickListener).show();

                        return true;
                    default:
                        break;
                }
                return false;
            }

            @Override
            public void onItemDismiss(final int position) {
                final int removedId = mDataset.get(position).getId();
                final boolean wasSelected = isSelected(position);

                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SQLiteDatabase db = DatabaseHandler.getInstance(getActivity()).getWritableDatabase();
                        Cursor c = null;
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                // Yes button clicked

                                c = db.rawQuery("delete from steps where id = ?", new String[]{Integer.toString(removedId)});
                                if (c != null) c.moveToFirst();

                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                // No button clicked
                                c = db.rawQuery("select action_id, sort_nr, function, call_flag, parent_id from steps where id = ?", new String[]{Integer.toString(removedId)});
                                if (c != null) {
                                    if (c.moveToFirst()) {
                                        String function = c.getString(2);
                                        String tmp;
                                        Cursor cF;
                                        int flag = c.getInt(3);
                                        if (flag == 1) {
                                            cF = db.rawQuery("select name from actions where id = ?", new String[]{function});
                                            tmp = "ERROR_FUNCTION_MISSING";
                                            if (cF != null) {
                                                if (cF.moveToFirst()) {
                                                    tmp = cF.getString(0);
                                                }
                                                cF.close();
                                            }
                                        } else tmp = function;
                                        mDataset.add(position, new Step(removedId, c.getInt(0), c.getInt(1), function, tmp, flag, c.getInt(4)));
                                    }
                                }
                                undoReorgAfterDismiss(position, mDataset.size() - 1, wasSelected);
                                notifyItemInserted(position);

                                break;
                        }
                        if (c != null) c.close();
                        db.close();
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(getString(R.string.sureStep, mDataset.get(position).getName()))
                        .setPositiveButton(getString(R.string.yes), dialogClickListener)
                        .setNegativeButton(getString(R.string.no), dialogClickListener).show();

                mDataset.remove(position);
                reorgAfterDismiss(position, mDataset.size());
                notifyItemRemoved(position);
            }

            @Override
            public void onItemMove(int from, int to) {
                Collections.swap(mDataset, from, to);
                if (appActionMode != null) reorgAfterMove(from, to);
                notifyItemMoved(from, to);
            }
        }

        private class mainBroadcastReceiver extends BroadcastReceiver {

            @Override
            public void onReceive(Context context, Intent intent) {
                String tmp = intent.getStringExtra("TAPITEM");
                if (tmp != null)
                    handleTaps(context, tmp, intent.getIntExtra("ID", -1), intent.getStringExtra("NAME"), intent.getIntExtra("FOCUS", -1));
                else {
                    tmp = intent.getStringExtra("INSERT");
                    //noinspection StatementWithEmptyBody
                    if (tmp != null)
                        handleInserts(context, tmp, intent.getStringExtra("NAME"), intent.getIntExtra("ID", -1));
                    else {

                    }
                }
            }

            private void handleInserts(Context context, String section, String name, int id) {
                SQLiteDatabase db = DatabaseHandler.getInstance(context).getWritableDatabase();
                Cursor c;
                ContentValues vals = new ContentValues();

                switch (section) {
                    case "SOURCE":
                        if (name.equals("")) return;

                        if (id == -1) {
                            c = db.rawQuery("select max(id) from sources", null);
                            if (c != null) {
                                if (c.moveToFirst()) {
                                    id = c.getInt(0) + 1;
                                }
                                c.close();
                            }
                            if (id == -1) id = 0;
                        }

                        vals.put("id", id);
                        vals.put("name", name);
                        try {
                            db.insert("sources", null, vals);
                        } catch (Exception ignored) {
                        }
                        break;
                    case "ACTION":
                    case "FUNCTION": {
                        if (name.equals("")) return;

                        if (id == -1) {
                            c = db.rawQuery("select max(id) from actions", null);
                            if (c != null) {
                                if (c.moveToFirst()) {
                                    id = c.getInt(0) + 1;
                                }
                                c.close();
                            }
                            if (id == -1) id = 0;
                        }

                        int source_id = section.equals("ACTION") ? sourceId : -1;
                        int sort_nr = -1;
                        c = db.rawQuery("select max(sort_nr) from actions where source_id = ?", new String[]{Integer.toString(source_id)});
                        if (c != null) {
                            if (c.moveToFirst()) {
                                sort_nr = c.getInt(0);
                            }
                            c.close();
                        }

                        vals.put("id", id);
                        vals.put("source_id", source_id);
                        vals.put("sort_nr", ++sort_nr);
                        vals.put("name", name);
                        try {
                            db.insert("actions", null, vals);
                        } catch (Exception ignored) {
                        }
                        break;
                    }
                    case "STEP": {
                        if (name.equals("") || insertParams == null) return;
                        if (insertParams.size() == 0) return;

                        if (id == -1) {
                            c = db.rawQuery("select max(id) from steps", null);
                            if (c != null) {
                                if (c.moveToFirst()) {
                                    id = c.getInt(0) + 1;
                                }
                                c.close();
                            }
                            if (id == -1) id = 0;
                        }

                        int sort_nr = -1;
                        c = db.rawQuery("select max(sort_nr) from steps where action_id = ?", new String[]{Integer.toString(actionId)});
                        if (c != null) {
                            if (c.moveToFirst()) {
                                sort_nr = c.getInt(0);
                            }
                            c.close();
                        }

                        vals.put("id", id);
                        vals.put("action_id", actionId);
                        vals.put("sort_nr", ++sort_nr);
                        vals.put("function", name);
                        vals.put("call_flag", 0);   // wird irgendwann natürlich Übergabeparameter
                        vals.put("parent_id", parentId);

                        try {
                            db.insert("steps", null, vals);
                        } catch (Exception ignored) {
                        }

                        int pId = -1;
                        c = db.rawQuery("select max(id) from params", null);
                        if (c != null) {
                            if (c.moveToFirst()) {
                                pId = c.getInt(0) + 1;
                            }
                            c.close();
                            if (pId == -1) pId = 0;
                        }

                        for (Param p : insertParams) {
                            vals.clear();
                            vals.put("id", pId++);
                            vals.put("step_id", id);
                            vals.put("idx", p.getIdx());
                            vals.put("value", p.getValue());
                            vals.put("variable_flag", p.getVariableFlag());
                            vals.put("list_flag", p.getListFlag());
                            try {
                                db.insert("params", null, vals);
                            } catch (Exception ignored) {
                            }
                        }
                        break;
                    }
                }
                db.close();

                displaySection(context, section, id, name);
            }

            private void handleTaps(Context context, String section, int id, String name, int focusId) {
                SQLiteDatabase db = DatabaseHandler.getInstance(context).getReadableDatabase();
                Cursor c;
                ItemTouchHelper.Callback callback;
                String tmp;
                int pKey, focusPos = -1;

                switch (section) {
                    case "ROOT":
                        ArrayList<Source> sourceDataset = new ArrayList<>();

                        db = DatabaseHandler.getInstance(context).getReadableDatabase();
                        c = db.rawQuery("select id, name from sources order by name", null);
                        if (c != null) {
                            if (c.moveToFirst()) {
                                do {
                                    pKey = c.getInt(0);
                                    if (pKey == focusId) focusPos = sourceDataset.size();
                                    sourceDataset.add(new Source(pKey, c.getString(1)));
                                } while (c.moveToNext());
                            }
                            c.close();
                        }
                        db.close();

                        mAdapter = new SourceAdapter(sourceDataset);
                        callback = new ItemTouchHelperCallback((SourceAdapter) mAdapter);
                        if (touchHelper != null) touchHelper.attachToRecyclerView(null);
                        touchHelper = new ItemTouchHelper(callback);
                        touchHelper.attachToRecyclerView(mRecyclerView);
                        activeSection = "SOURCES";
                        navTitle.setText(getString(R.string.navTitleSources));
                        fab.show();
                        break;
                    case "SOURCE":
                        if (id == -1) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setTitle(getString(R.string.newSource));
                            builder.setMessage(getString(R.string.inputName));
                            final EditText input = createInput(context, false);
                            builder.setView(input);
                            builder.setPositiveButton(getString(R.string.ok),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            MainActivity.insertRow(getActivity(), "SOURCE", input.getText().toString().trim(), -1);
                                        }
                                    });
                            builder.setNegativeButton(getString(R.string.cancel),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                        }
                                    });
                            AlertDialog dialog = builder.create();
                            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                            dialog.show();
                        } else {
                            ArrayList<Action> actionDataset = new ArrayList<>();

                            c = db.rawQuery("select id, source_id, sort_nr, name from actions where source_id = ? order by sort_nr", new String[]{Integer.toString(id)});
                            if (c != null) {
                                if (c.moveToFirst()) {
                                    do {
                                        pKey = c.getInt(0);
                                        if (pKey == focusId) focusPos = actionDataset.size();
                                        actionDataset.add(new Action(pKey, c.getInt(1), c.getInt(2), c.getString(3)));
                                    } while (c.moveToNext());
                                }
                                c.close();
                            }

                            mAdapter = new ActionAdapter(actionDataset);
                            callback = new ItemTouchHelperCallback((ActionAdapter) mAdapter);
                            if (touchHelper != null) touchHelper.attachToRecyclerView(null);
                            touchHelper = new ItemTouchHelper(callback);
                            touchHelper.attachToRecyclerView(mRecyclerView);
                            activeSection = "ACTIONS";
                            sourceId = id;
                            if (name == null) {
                                c = db.rawQuery("select name from sources where id = ?", new String[]{Integer.toString(id)});
                                if (c != null) {
                                    if (c.moveToFirst()) sourceName = c.getString(0);
                                    c.close();
                                }
                            } else sourceName = name;
                            navTitle.setText(getString(R.string.actionsFor, sourceName));
                            fab.show();
                            db.close();
                        }
                        break;
                    case "ACTION":
                        if (id == -1) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setTitle(getString(R.string.newAction));
                            builder.setMessage(getString(R.string.inputName));
                            final EditText input = createInput(context, false);
                            builder.setView(input);
                            builder.setPositiveButton(getString(R.string.ok),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            MainActivity.insertRow(getActivity(), "ACTION", input.getText().toString().trim(), -1);
                                        }
                                    });
                            builder.setNegativeButton(getString(R.string.cancel),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                        }
                                    });
                            AlertDialog dialog = builder.create();
                            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                            dialog.show();
                        } else {
                            ArrayList<Step> stepDataset = new ArrayList<>();
                            Cursor cF;
                            boolean gotParent = parentId >= 0;

                            db = DatabaseHandler.getInstance(context).getReadableDatabase();
                            c = db.rawQuery("select id, action_id, sort_nr, function, call_flag, parent_id from steps where " + (gotParent ? "parent_id = ?" : "parent_id = -1 and action_id = ?") + " order by sort_nr", new String[]{gotParent ? Integer.toString(parentId) : Integer.toString(id)});
                            if (c != null) {
                                if (c.moveToFirst()) {
                                    do {
                                        pKey = c.getInt(0);
                                        if (pKey == focusId) focusPos = stepDataset.size();
                                        String function = c.getString(3);
                                        int flag = c.getInt(4);
                                        if (flag == 1) {
                                            cF = db.rawQuery("select name from actions where id = ?", new String[]{function});
                                            tmp = "ERROR_FUNCTION_MISSING";
                                            if (cF != null) {
                                                if (cF.moveToFirst()) tmp = cF.getString(0);
                                                cF.close();
                                            }
                                        } else tmp = function;
                                        stepDataset.add(new Step(pKey, c.getInt(1), c.getInt(2), function, tmp, flag, c.getInt(5)));
                                    } while (c.moveToNext());
                                }
                                c.close();
                            }

                            mAdapter = new StepAdapter(stepDataset);
                            callback = new ItemTouchHelperCallback((StepAdapter) mAdapter);
                            if (touchHelper != null) touchHelper.attachToRecyclerView(null);
                            touchHelper = new ItemTouchHelper(callback);
                            touchHelper.attachToRecyclerView(mRecyclerView);
                            activeSection = "STEPS";
                            if (!gotParent) {
                                actionId = id;
                                if (name == null) {
                                    c = db.rawQuery("select name from actions where id = ?", new String[]{Integer.toString(id)});
                                    if (c != null) {
                                        if (c.moveToFirst()) actionName = c.getString(0);
                                        c.close();
                                    }
                                } else actionName = name;
                            }
                            setTextViewHTML(navTitle, gotParent ? DatabaseHandler.getNavTitleHTML(context, db, "params", parentId) : getString(R.string.stepsFor, actionName) + " (<a href='SRC'>" + sourceName + "</a>)");
                            fab.show();
                            db.close();
                        }
                        break;
                    case "STEP":
                        if (id == -1) {
                            stepId = -1;
                            stepName = "";
                            createStep(new ArrayList<Param>());
                        } else {
                            ArrayList<Param> paramDataset = new ArrayList<>();

                            c = db.rawQuery("select id, step_id, idx, value, variable_flag, list_flag from params where step_id = ? order by idx", new String[]{Integer.toString(id)});
                            if (c != null) {
                                if (c.moveToFirst()) {
                                    do {
                                        paramDataset.add(new Param(c.getInt(0), c.getInt(1), c.getInt(2), c.getString(3), c.getInt(4), c.getInt(5)));
                                    } while (c.moveToNext());
                                }
                                c.close();
                            }

                            mAdapter = new ParamAdapter(paramDataset);
                            callback = new ItemTouchHelperCallback((ParamAdapter) mAdapter);
                            if (touchHelper != null) touchHelper.attachToRecyclerView(null);
                            touchHelper = new ItemTouchHelper(callback);
                            touchHelper.attachToRecyclerView(mRecyclerView);
                            activeSection = "PARAMS";
                            stepId = id;
                            if (name == null) {
                                c = db.rawQuery("select function,call_flag from steps where id = ?", new String[]{Integer.toString(id)});
                                if (c != null) {
                                    if (c.moveToFirst()) {
                                        String function = c.getString(0);
                                        int flag = c.getInt(1);
                                        if (flag == 1) {
                                            Cursor cF;
                                            cF = db.rawQuery("select name from actions where id = ?", new String[]{function});
                                            tmp = "ERROR_FUNCTION_MISSING";
                                            if (cF != null) {
                                                if (cF.moveToFirst()) tmp = cF.getString(0);
                                                cF.close();
                                            }
                                        } else tmp = function;
                                        stepName = tmp;
                                    }
                                    c.close();
                                }
                            } else stepName = name;
                            setTextViewHTML(navTitle, DatabaseHandler.getNavTitleHTML(context, db, "steps", id));
                            fab.hide();
                            db.close();
                        }
                        break;
                    case "PARAM":
                        switch (name) {
                            case "then":
                            case "else":
                                parentId = id;
                                handleTaps(context, "ACTION", -2, null, -1);
                                break;
                        }
                        break;
                }

                if (mAdapter != null) {
                    mRecyclerView.setAdapter(mAdapter);
                    if (focusPos >= 0) mLayoutManager.scrollToPosition(focusPos);
                }
            }

            private void createStep(final ArrayList<Param> params) {
                final Context context = getActivity();
                final SQLiteDatabase db = DatabaseHandler.getInstance(context).getReadableDatabase();

                DialogInterface.OnClickListener backFromFirstParam = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        stepId = -1;
                        dialog.dismiss();
                        createStep(params);
                    }
                };
                final DialogInterface.OnClickListener backFromFurtherParam = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        params.remove(params.size() - 1);
                        dialog.dismiss();
                        createStep(params);
                    }
                };
                final DialogInterface.OnClickListener backFromInnerDialog = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        createStep(params);
                    }
                };
                final DialogInterface.OnClickListener cancelWizard = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        stepId = -1;
                        db.close();
                    }
                };

                if (stepId == -1) {

                    // Auswahl aus integrierten Schritten

                    // String-Arrays sortieren
                    final String names[] = getResources().getStringArray(R.array.builtInStepsNames);
                    final String steps[] = getResources().getStringArray(R.array.builtInSteps);
                    sortDictArray(names, steps);

                    showCreateStepWizard(context, getString(R.string.newStep), null, -1, names, null,
                            null,
                            null,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    if (selectedId >= 0) {
                                        stepId = DatabaseHandler.getNewId(db, "steps");
                                        stepName = steps[selectedId];
                                    }
                                    createStep(params);
                                }
                            });

                } else {

                    switch (stepName) {
                        case "set":

                            switch (params.size()) {
                                case 0:

                                    // set, Quelle

                                    createParamByWizard(
                                            context,
                                            db,
                                            params,
                                            false,
                                            true,
                                            false,
                                            false,
                                            null,
                                            0,
                                            false,
                                            null,
                                            -1,
                                            backFromFirstParam,
                                            backFromFurtherParam,
                                            backFromInnerDialog,
                                            cancelWizard);

                                    break;
                                case 1:

                                    // set, Ziel

                                    createParamByWizard(
                                            context,
                                            db,
                                            params,
                                            false,
                                            false,
                                            true,
                                            false,
                                            null,
                                            1,
                                            false,
                                            null,
                                            1,
                                            backFromFirstParam,
                                            backFromFurtherParam,
                                            backFromInnerDialog,
                                            cancelWizard);

                                    break;
                            }
                            break;

                        case "add":

                            switch (params.size()) {
                                case 0:

                                    break;

                                case 1:

                                    break;
                            }
                            break;

                        case "subtract":

                            switch (params.size()) {
                                case 0:

                                    break;

                                case 1:

                                    break;
                            }
                            break;

                        case "multiply":

                            switch (params.size()) {
                                case 0:

                                    break;

                                case 1:

                                    break;
                            }
                            break;

                        case "divide":

                            switch (params.size()) {
                                case 0:

                                    break;

                                case 1:

                                    break;
                            }
                            break;

                        case "incr":

                            switch (params.size()) {
                                case 0:

                                    break;

                                case 1:

                                    break;
                            }
                            break;

                        case "decr":

                            switch (params.size()) {
                                case 0:

                                    break;

                                case 1:

                                    break;
                            }
                            break;

                        case "substring":

                            switch (params.size()) {
                                case 0:

                                    break;

                                case 1:

                                    break;
                            }
                            break;

                        case "trim":

                            switch (params.size()) {
                                case 0:

                                    break;

                                case 1:

                                    break;
                            }
                            break;

                        case "ltrim":

                            switch (params.size()) {
                                case 0:

                                    break;

                                case 1:

                                    break;
                            }
                            break;

                        case "rtrim":

                            switch (params.size()) {
                                case 0:

                                    break;

                                case 1:

                                    break;
                            }
                            break;

                        case "replace":

                            switch (params.size()) {
                                case 0:

                                    break;

                                case 1:

                                    break;
                            }
                            break;

                        case "putlist":

                            switch (params.size()) {
                                case 0:

                                    break;

                                case 1:

                                    break;
                            }
                            break;

                        case "foreach":

                            switch (params.size()) {
                                case 0:

                                    break;

                                case 1:

                                    break;
                            }
                            break;

                        case "if":

                            switch (params.size()) {
                                case 0:

                                    // if, Operator

                                    final String opNames[] = getResources().getStringArray(R.array.ifOperationsNames);
                                    final String ops[] = getResources().getStringArray(R.array.ifOperations);
                                    sortDictArray(opNames, ops);

                                    showCreateStepWizard(context, getString(R.string.newParameter, getString(R.string.ifParam1)), null, -1, opNames, null,
                                            backFromFirstParam,
                                            cancelWizard,
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    if (selectedId >= 0) {
                                                        params.add(new Param(-1, stepId, 0, ops[selectedId], 0, 0));
                                                        createStep(params);
                                                    }
                                                }
                                            });

                                    break;

                                case 1:

                                    // if, Wert1

                                    createParamByWizard(
                                            context,
                                            db,
                                            params,
                                            params.get(0).getValue().contains("list"),
                                            false,
                                            false,
                                            false,
                                            null,
                                            1,
                                            false,
                                            null,
                                            -1,
                                            backFromFirstParam,
                                            backFromFurtherParam,
                                            backFromInnerDialog,
                                            cancelWizard);

                                    break;

                                case 2:

                                    // if, Wert2

                                    // schon mal die Dann-Sonst-Params dabei tun
                                    params.add(new Param(-1, stepId, 3, "then", 0, 0));
                                    params.add(new Param(-1, stepId, 4, "else", 0, 0));

                                    createParamByWizard(
                                            context,
                                            db,
                                            params,
                                            false,
                                            true,
                                            false,
                                            false,
                                            null,
                                            2,
                                            false,
                                            null,
                                            1,
                                            backFromFirstParam,
                                            backFromFurtherParam,
                                            backFromInnerDialog,
                                            cancelWizard);

                                    break;

                            }
                            break;

                        case "doloop":

                            switch (params.size()) {
                                case 0:

                                    break;

                                case 1:

                                    break;
                            }
                            break;

                        case "for":

                            switch (params.size()) {
                                case 0:

                                    break;

                                case 1:

                                    break;
                            }
                            break;

                        case "split":

                            switch (params.size()) {
                                case 0:

                                    break;

                                case 1:

                                    break;
                            }
                            break;

                        case "concat":

                            switch (params.size()) {
                                case 0:

                                    break;

                                case 1:

                                    break;
                            }
                            break;

                        case "left":

                            switch (params.size()) {
                                case 0:

                                    break;

                                case 1:

                                    break;
                            }
                            break;

                        case "right":

                            switch (params.size()) {
                                case 0:

                                    break;

                                case 1:

                                    break;
                            }
                            break;

                        case "instr":

                            switch (params.size()) {
                                case 0:

                                    break;

                                case 1:

                                    break;
                            }
                            break;

                        case "len":

                            switch (params.size()) {
                                case 0:

                                    break;

                                case 1:

                                    break;
                            }
                            break;

                        case "instrrev":

                            switch (params.size()) {
                                case 0:

                                    break;

                                case 1:

                                    break;
                            }
                            break;

                        case "httpget":

                            switch (params.size()) {
                                case 0:

                                    break;

                                case 1:

                                    break;
                            }
                            break;

                        case "httpput":

                            switch (params.size()) {
                                case 0:

                                    break;

                                case 1:

                                    break;
                            }
                            break;

                        case "execjs":

                            switch (params.size()) {
                                case 0:

                                    break;

                                case 1:

                                    break;
                            }
                            break;

                        case "makemsg":

                            switch (params.size()) {
                                case 0:

                                    break;

                                case 1:

                                    break;
                            }
                            break;

                        case "reverse":

                            switch (params.size()) {
                                case 0:

                                    break;

                                case 1:

                                    break;
                            }
                            break;

                        case "exitfor":

                            switch (params.size()) {
                                case 0:

                                    break;

                                case 1:

                                    break;
                            }
                            break;

                        case "exitforeach":

                            switch (params.size()) {
                                case 0:

                                    break;

                                case 1:

                                    break;
                            }
                            break;

                        case "exitdoloop":

                            switch (params.size()) {
                                case 0:

                                    break;

                                case 1:

                                    break;
                            }
                            break;

                        case "exitifelse":

                            switch (params.size()) {
                                case 0:

                                    break;

                                case 1:

                                    break;
                            }
                            break;

                        case "end":

                            switch (params.size()) {
                                case 0:

                                    break;

                                case 1:

                                    break;
                            }
                            break;
                    }
                }
            }

            private void sortDictArray(String[] names, String[] steps) {
                TreeMap<String, String> stepMap = new TreeMap<>();
                int i;
                for (i = 0; i < steps.length; i++)
                    stepMap.put(names[i], steps[i]);
                i = 0;
                for (Map.Entry<String, String> entry : stepMap.entrySet()) {
                    names[i] = entry.getKey();
                    steps[i] = entry.getValue();
                    i++;
                }
            }

            private void getChoosables(ArrayList<String> list, int lstCount[], SQLiteDatabase db, int sourceId, boolean showLists, boolean showFixedValue, boolean showNewVariable, boolean showNewList) {
                ArrayList<String> lst = showLists ? DatabaseHandler.getListsBySourceId(db, sourceId) : DatabaseHandler.getVariablesBySourceId(db, sourceId);
                if (showFixedValue) list.add(getString(R.string.fixedValue));
                if (showNewVariable) list.add(getString(R.string.newVariable));
                else if (showNewList) list.add(getString(R.string.newList));
                list.addAll(lst);
                lstCount[0] = list.size();
            }

            private void createParamByWizard(final Context context,
                                             final SQLiteDatabase db,
                                             final ArrayList<Param> params,
                                             final boolean showLists,
                                             final boolean fixedValue,
                                             final boolean newVariable,
                                             final boolean newList,
                                             String optionList[],
                                             final int paramIdx,
                                             final boolean numericOnly,
                                             final String singleInputMessage,
                                             final int finalDialogStartingAtId,
                                             final DialogInterface.OnClickListener backFromFirstParam,
                                             final DialogInterface.OnClickListener backFromFurtherParam,
                                             final DialogInterface.OnClickListener backFromInnerDialog,
                                             final DialogInterface.OnClickListener cancelWizard) {

                int cnt[] = new int[1];
                cnt[0] = paramIdx + 1;
                final EditText singleInput;
                final ArrayList<String> lst = new ArrayList<>();
                final String title = getString(R.string.newParameter, getString(getResources().getIdentifier(stepName + "Param" + cnt[0], "string", getActivity().getPackageName())));

                if (singleInputMessage != null) {
                    singleInput = createInput(context, numericOnly);
                } else if (optionList == null) {
                    getChoosables(lst, cnt, db, sourceId, showLists, fixedValue, newVariable, newList);
                    optionList = lst.toArray(new String[cnt[0]]);
                    singleInput = null;
                } else {
                    lst.addAll(Arrays.asList(optionList));
                    singleInput = null;
                }

                showCreateStepWizard(context, title, singleInputMessage, finalDialogStartingAtId, optionList, singleInput, paramIdx == 0 ? backFromFirstParam : backFromFurtherParam, cancelWizard,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                String msg = "";
                                final int varFlag[] = new int[1];
                                final int lstFlag[] = new int[1];
                                boolean showInnerDialog = false;
                                if (selectedId >= 0) {
                                    if (singleInput != null)
                                        // einzelne Eingabe
                                        proceedCreateStep(context, db, params, paramIdx, singleInput.getText().toString().trim(), 0, 0, finalDialogStartingAtId);
                                    else {
                                        // eine Optionsliste wurde angeboten
                                        if (fixedValue && selectedId == 0) {
                                            // "bestimmter Wert" wurde ausgewählt
                                            msg = getString(R.string.inputFixedValue);
                                            varFlag[0] = 0;
                                            lstFlag[0] = 0;
                                            showInnerDialog = true;
                                        } else if (newVariable && (!fixedValue && selectedId == 0 || fixedValue && selectedId == 1)) {
                                            // "neue Variable" wurde ausgewählt
                                            msg = getString(R.string.inputNewVariable);
                                            varFlag[0] = 1;
                                            lstFlag[0] = 0;
                                            showInnerDialog = true;
                                        } else if (newList && (!fixedValue && selectedId == 0 || fixedValue && selectedId == 1)) {
                                            // "neue Liste" wurde ausgewählt
                                            msg = getString(R.string.inputNewList);
                                            varFlag[0] = 0;
                                            lstFlag[0] = 1;
                                            showInnerDialog = true;
                                        } else {
                                            // bekannte Variable/Liste wurde ausgewählt
                                            proceedCreateStep(context, db, params, paramIdx, lst.get(selectedId), showLists ? 0 : 1, showLists ? 1 : 0, finalDialogStartingAtId);
                                        }
                                        if (showInnerDialog) {
                                            // Eingabedialog für bestimmten Wert bzw. neuen Variablen-/Listennamen zeigen
                                            final EditText input = createInput(context, numericOnly);
                                            showCreateStepWizard(context, title, msg, finalDialogStartingAtId == -1 ? -1 : 0, null, input, backFromInnerDialog, cancelWizard,
                                                    new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int id) {
                                                            proceedCreateStep(context, db, params, paramIdx, input.getText().toString().trim(), varFlag[0], lstFlag[0], finalDialogStartingAtId);
                                                        }
                                                    }
                                            );
                                        }
                                    }
                                }
                            }
                        });
            }

            private void proceedCreateStep(Context context, SQLiteDatabase db, ArrayList<Param> params, int paramIdx, String paramValue, int varFlag, int lstFlag, int finalDialogStartingAtId) {
                params.add(new Param(-1, stepId, paramIdx, paramValue, varFlag, lstFlag));
                if (finalDialogStartingAtId == -1)
                    createStep(params);
                else {
                    db.close();
                    insertRow(context, "STEP", stepName, stepId, params);
                }
            }

            private void showCreateStepWizard(final Context context,
                                              String title,
                                              String message,
                                              final int finalDialogStartingAtId,
                                              final String optionList[],
                                              final View inputView,
                                              DialogInterface.OnClickListener onClickBack,
                                              DialogInterface.OnClickListener onClickCancel,
                                              DialogInterface.OnClickListener onClickNext) {
                AlertDialog.Builder builder;
                AlertDialog dialog;
                int cnt;

                builder = new AlertDialog.Builder(context);
                builder.setTitle(title);
                if (message != null) builder.setMessage(message);
                if (optionList != null) {
                    cnt = optionList.length;
                    selectedId = cnt == 1 ? 0 : -1;
                    builder.setSingleChoiceItems(optionList, selectedId, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Button posButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                            posButton.setEnabled(true);
                            if (finalDialogStartingAtId >= 0 && (id >= finalDialogStartingAtId ^ selectedId >= finalDialogStartingAtId)) {
                                if (id >= finalDialogStartingAtId)
                                    posButton.setText(getString(R.string.finish));
                                else
                                    posButton.setText(getString(R.string.next));
                            }
                            selectedId = id;
                        }
                    });
                } else {
                    builder.setView(inputView);
                }
                builder.setPositiveButton(finalDialogStartingAtId == -1 || selectedId == -1 || selectedId < finalDialogStartingAtId ? getString(R.string.next) : getString(R.string.finish), onClickNext);
                builder.setNegativeButton(getString(R.string.cancel), onClickCancel);
                if (onClickBack != null)
                    builder.setNeutralButton(getString(R.string.back), onClickBack);
                dialog = builder.create();
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                dialog.show();
                if (optionList != null && selectedId == -1)
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            }
        }
    }
}