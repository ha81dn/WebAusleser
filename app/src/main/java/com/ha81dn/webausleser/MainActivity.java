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
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Html;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/* ToDo-Liste
- Anlegerei mit Assistent
- Umbenennen
- Löschen mit Animationen, dazu am besten am Dataset selbst rumhantieren
  und dann auf den RecyclerView-Adapter ein notify loshetzen
- Kopieren
- Testcenter
- kein Menü, nur Action-Icons für Fragment-Wechsel zu Funktionen, Einstellungen und Testcenter
- Absturzbehandlung wie beim MioKlicker, nur mit eigener Activity statt Tabpage
- ERL Pfad-Textfeld in einer oberen Zeile über dem RecyclerView
- ERL Pfad könnte in einer Version 2.0 aus Textfeldern pro Hierarchieebene bestehen, wobei pro Ebene
  ERL ein onKlick eine Popup-Auflistung der jew. Datensätze liefert, um direkt dorthin zu springen
- ERL ein und derselbe RecyclerView wird für alle Tabellen benutzt, Adapter werden an- und abgetatcht
- ERL BackButton geht eine Ebene höher in der Pfadhierarchie
- ERL Kopfzeile: <Name des Angezeigten> (... / ParentParent / Parent)

*/

public class MainActivity extends AppCompatActivity {

    private static final long delay = 3000L;
    private boolean mRecentlyBackPressed = false;
    private Handler mExitHandler = new Handler();
    private Runnable mExitRunnable = new Runnable() {
        @Override
        public void run() {
            mRecentlyBackPressed = false;
        }
    };
    static String activeSection = "SOURCES";
    static String sourceName, actionName, stepName;
    static int sourceId, actionId, stepId, selectedId;
    static boolean actionModeActive = false;
    static ArrayList<Param> insertParams;

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
        return true;
    }

    @Override
    public void onBackPressed() {
        if (actionModeActive)
            super.onBackPressed();
        else {
            if (activeSection.equals("SOURCES") || activeSection.equals("FUNCTIONS")) {
                if (mRecentlyBackPressed) {
                    if (mExitHandler != null) {
                        mExitHandler.removeCallbacks(mExitRunnable);
                        mExitHandler = null;
                    }
                    super.onBackPressed();
                }
                else
                {
                    mRecentlyBackPressed = true;
                    Toast.makeText(this, "zum Beenden ein zweites Mal drücken", Toast.LENGTH_SHORT).show();
                    mExitHandler.postDelayed(mExitRunnable, delay);
                }
            } else if (activeSection.equals("ACTIONS")) {
                if (sourceId == -1) {
                    displaySection(this, "FUNCTIONS", -1, null);
                } else {
                    displaySection(this, "ROOT", -1, null);
                    sourceId = -1;
                    sourceName = null;
                }
            } else if (activeSection.equals("STEPS")) {
                displaySection(this, "SOURCE", sourceId, sourceName);
                actionId = -1;
                actionName = null;
            } else if (activeSection.equals("PARAMS")) {
                displaySection(this, "ACTION", actionId, actionName);
                stepId = -1;
                stepName = null;
            }
        }
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

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        private RecyclerView.LayoutManager mLayoutManager;
        private RecyclerView.Adapter mAdapter = null;
        private mainBroadcastReceiver mainBR;
        private boolean initializationFinished = false;
        static TextView navTitle;
        static RecyclerView mRecyclerView;

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

        private void makeLinkClickable(SpannableStringBuilder strBuilder, final URLSpan span)
        {
            int start = strBuilder.getSpanStart(span);
            int end = strBuilder.getSpanEnd(span);
            int flags = strBuilder.getSpanFlags(span);
            ClickableSpan clickable = new ClickableSpan() {
                public void onClick(View view) {
                    String url = span.getURL();
                    if (url != null) {
                        if (url.equals("SRC")) {
                            MainActivity.displaySection(view.getContext(), "SOURCE", sourceId, sourceName);
                        } else if (url.equals("ACT")) {
                            MainActivity.displaySection(view.getContext(), "ACTION", actionId, actionName);
                        } else if (url.equals("STP")) {
                            MainActivity.displaySection(view.getContext(), "STEP", stepId, stepName);
                        }
                    }
                }
            };
            strBuilder.setSpan(clickable, start, end, flags);
            strBuilder.removeSpan(span);
        }

        private void setTextViewHTML(TextView text, String html)
        {
            CharSequence sequence = Html.fromHtml(html);
            SpannableStringBuilder strBuilder = new SpannableStringBuilder(sequence);
            URLSpan[] urls = strBuilder.getSpans(0, sequence.length(), URLSpan.class);
            for(URLSpan span : urls) {
                makeLinkClickable(strBuilder, span);
            }
            text.setText(strBuilder);
            text.setMovementMethod(LinkMovementMethod.getInstance());
        }

        private ItemTouchHelperCallbackCallback deleteMode = new ModalMultiSelectorCallback(singleSelector) {

            private boolean movedAround = false;

            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                getActivity().getMenuInflater().inflate(R.menu.list_item_context, menu);
                actionModeActive = true;
                movedAround = false;
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                if (!(activeSection.equals("ACTIONS") || activeSection.equals("STEPS"))) {
                    menu.getItem(0).setVisible(false);
                    menu.getItem(1).setVisible(false);
                }
                return super.onPrepareActionMode(actionMode, menu);
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                if (singleSelector.getSelectedPositions().size() == 0) return false;
                final int pos = singleSelector.getSelectedPositions().get(0);
                switch (menuItem.getItemId()) {
                    case R.id.menu_item_up:
                        if (activeSection.equals("ACTIONS")) {
                            ArrayList<Action> list = ((ActionAdapter) mAdapter).mDataset;
                            if (pos >= 1) {
                                Collections.swap(list, pos, pos - 1);
                                singleSelector.setSelected(pos, 0, false);
                                singleSelector.setSelected(pos - 1, 0, true);
                                movedAround = true;
                                mAdapter.notifyDataSetChanged();
                            }
                        } else if (activeSection.equals("STEPS")) {
                            ArrayList<Step> list = ((StepAdapter) mAdapter).mDataset;
                            if (pos >= 1) {
                                Collections.swap(list, pos, pos - 1);
                                singleSelector.setSelected(pos, 0, false);
                                singleSelector.setSelected(pos - 1, 0, true);
                                movedAround = true;
                                mAdapter.notifyDataSetChanged();
                            }
                        }
                        return true;

                    case R.id.menu_item_down:
                        if (activeSection.equals("ACTIONS")) {
                            ArrayList<Action> list = ((ActionAdapter) mAdapter).mDataset;
                            if (pos < list.size()-2) {
                                Collections.swap(list, pos, pos + 1);
                                singleSelector.setSelected(pos, 0, false);
                                singleSelector.setSelected(pos + 1, 0, true);
                                movedAround = true;
                                mAdapter.notifyDataSetChanged();
                            }
                        } else if (activeSection.equals("STEPS")) {
                            ArrayList<Step> list = ((StepAdapter) mAdapter).mDataset;
                            if (pos < list.size()-2) {
                                Collections.swap(list, pos, pos + 1);
                                singleSelector.setSelected(pos, 0, false);
                                singleSelector.setSelected(pos + 1, 0, true);
                                movedAround = true;
                                mAdapter.notifyDataSetChanged();
                            }
                        }
                        return true;

                    case R.id.menu_item_delete:
                        actionMode.finish();

                        String tmp = null;
                        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which){
                                    case DialogInterface.BUTTON_POSITIVE:
                                        //Yes button clicked
                                        SQLiteDatabase db = DatabaseHandler.getInstance(getActivity()).getWritableDatabase();
                                        Cursor c;

                                        if (activeSection.equals("SOURCES")) {
                                            ArrayList<Source> list = ((SourceAdapter) mAdapter).mDataset;
                                            c = db.rawQuery("delete from sources where id = ?", new String[]{Integer.toString(list.get(pos).getId())});
                                            c.moveToFirst();
                                            c.close();
                                            list.remove(pos);
                                        } else if (activeSection.equals("ACTIONS")) {
                                            ArrayList<Action> list = ((ActionAdapter) mAdapter).mDataset;
                                            c = db.rawQuery("delete from actions where id = ?", new String[]{Integer.toString(list.get(pos).getId())});
                                            c.moveToFirst();
                                            c.close();
                                            list.remove(pos);
                                        } else if (activeSection.equals("STEPS")) {
                                            ArrayList<Step> list = ((StepAdapter) mAdapter).mDataset;
                                            c = db.rawQuery("delete from steps where id = ?", new String[]{Integer.toString(list.get(pos).getId())});
                                            c.moveToFirst();
                                            c.close();
                                            list.remove(pos);
                                        } else if (activeSection.equals("FUNCTIONS")) {
                                            ArrayList<Action> list = ((FunctionAdapter) mAdapter).mDataset;
                                            c = db.rawQuery("delete from actions where id = ?", new String[]{Integer.toString(list.get(pos).getId())});
                                            c.moveToFirst();
                                            c.close();
                                            list.remove(pos);
                                        }
                                        db.close();

                                        mAdapter.notifyDataSetChanged();
                                        break;

                                    case DialogInterface.BUTTON_NEGATIVE:
                                        //No button clicked
                                        break;
                                }
                            }
                        };

                        if (activeSection.equals("SOURCES"))
                            tmp = getString(R.string.sureSource, ((SourceAdapter) mAdapter).mDataset.get(pos).getName());
                        else if (activeSection.equals("ACTIONS"))
                            tmp = getString(R.string.sureAction, ((ActionAdapter) mAdapter).mDataset.get(pos).getName());
                        else if (activeSection.equals("STEPS"))
                            tmp = getString(R.string.sureStep, ((StepAdapter) mAdapter).mDataset.get(pos).getName());
                        else if (activeSection.equals("FUNCTIONS"))
                            tmp = getString(R.string.sureFunction, ((FunctionAdapter) mAdapter).mDataset.get(pos).getName());

                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setMessage(tmp).setPositiveButton(getString(R.string.yes), dialogClickListener)
                                .setNegativeButton(getString(R.string.no), dialogClickListener).show();

                        singleSelector.clearSelections();
                        return true;
                    default:
                        break;
                }
                return false;
            }
        };

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
            public int getMovementFlags(RecyclerView recyclerView, ViewHolder viewHolder) {
                // ToDo: hier je nach ViewHolder einschreiten, wenn nicht drag- oder swipebar
                int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
                return makeMovementFlags(dragFlags, swipeFlags);
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, ViewHolder viewHolder,
                                  ViewHolder target) {
                mAdapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }

            @Override
            public void onMoved(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, int fromPos, RecyclerView.ViewHolder target, int toPos, int x, int y) {
                SQLiteDatabase db = DatabaseHandler.getInstance(getActivity()).getWritableDatabase();
                Cursor c;

                if (activeSection.equals("ACTIONS")) {
                    ArrayList<Action> list = ((ActionAdapter) mAdapter).mDataset;
                    for (int i=Math.min(fromPos, toPos); i<=Math.max(fromPos, toPos); i++) {
                        c = db.rawQuery("update actions set sort_nr = ? where id = ?", new String[]{Integer.toString(i), Integer.toString(list.get(i).getId())});
                        c.moveToFirst();
                        c.close();
                    }
                } else if (activeSection.equals("STEPS")) {
                    ArrayList<Step> list = ((StepAdapter) mAdapter).mDataset;
                    for (int i=Math.min(fromPos, toPos); i<=Math.max(fromPos, toPos); i++) {
                        c = db.rawQuery("update steps set sort_nr = ? where id = ?", new String[]{Integer.toString(i), Integer.toString(list.get(i).getId())});
                        c.moveToFirst();
                        c.close();
                    }
                }
                db.close();
            }

            @Override
            public void onSwiped(ViewHolder viewHolder, int direction) {
                mAdapter.onItemDismiss(viewHolder.getAdapterPosition());
            }
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
             *
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
             *
             * @see RecyclerView#getAdapterPositionFor(RecyclerView.ViewHolder)
             * @see RecyclerView.ViewHolder#getAdapterPosition()
             */
            void onItemDismiss(int position);
        }

        private class ActionAdapter extends RecyclerView.Adapter<ActionAdapter.ViewHolder> implements ItemTouchHelperAdapter {
            public ArrayList<Action> mDataset;

            // Provide a reference to the views for each data item
            // Complex data items may need more than one view per item, and
            // you provide access to all the views for a data item in a view holder
            protected class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
                // each data item is just a string in this case
                public TextView mTextView;

                public ViewHolder(TextView v) {
                    super(v);
                    v.setOnClickListener(this);
                    v.setOnLongClickListener(this);
                    v.setLongClickable(true);
                    mTextView = v;
                }

                @Override
                public void onClick(View view) {

                    // ToDo: es wird eine selectedItems-Liste geben, deren Size man hier evtl. abfrägt

                    if (!singleSelector.isSelectable() || !singleSelector.tapSelection(this)) {
                        Intent intentUpdate = new Intent();
                        intentUpdate.setAction("com.ha81dn.webausleser.ASYNC_MAIN");
                        intentUpdate.addCategory(Intent.CATEGORY_DEFAULT);
                        intentUpdate.putExtra("TAPITEM", "ACTION");
                        intentUpdate.putExtra("ID", mDataset.get(getAdapterPosition()).getId());
                        intentUpdate.putExtra("NAME", mDataset.get(getAdapterPosition()).getName());
                        mTextView.getContext().sendBroadcast(intentUpdate);
                    }
                }

                @Override
                public boolean onLongClick(View v) {
                    if (isSelectable()) {
                        AppCompatActivity activity = (AppCompatActivity) getActivity();
                        activity.startSupportActionMode(deleteMode);
                        singleSelector.setSelected(this, true);
                        return true;
                    }
                    return false;
                }
            }

            // Provide a suitable constructor (depends on the kind of dataset)
            public ActionAdapter(ArrayList<Action> myDataset) {
                mDataset = myDataset;
            }

            // Create new views (invoked by the layout manager)
            @Override
            public ActionAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                               int viewType) {
                // create a new view
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.view_main, parent, false);
                // set the view's size, margins, paddings and layout parameters
                ViewHolder vh = new ViewHolder((TextView) v);
                return vh;
            }

            // Replace the contents of a view (invoked by the layout manager)
            @Override
            public void onBindViewHolder(ActionAdapter.ViewHolder holder, int position) {
                // - get element from your dataset at this position
                // - replace the contents of the view with that element
                if (mDataset.get(position).getId() == -1) {
                    holder.mTextView.setText(Html.fromHtml("<i>" + mDataset.get(position).getName() + "</i>"));
                    holder.isSelectable = false;
                }
                else
                    holder.mTextView.setText(mDataset.get(position).getName());
            }

            // Return the size of your dataset (invoked by the layout manager)
            @Override
            public int getItemCount() {
                return mDataset.size();
            }

            @Override
            public void onItemDismiss(int position) {
                final int removedId = mDataset.get(position).getId();

                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SQLiteDatabase db = DatabaseHandler.getInstance(getActivity()).getWritableDatabase();
                        Cursor c = null;
                        switch (which){
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
                                notifyItemInserted(position);

                                break;
                        }
                        if (c != null) c.close();
                        db.close();
                    }
                };

                mDataset.remove(position);
                notifyItemRemoved(position);

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(getString(R.string.sureAction, mDataset.get(position).getName())).setPositiveButton(getString(R.string.yes), dialogClickListener)
                        .setNegativeButton(getString(R.string.no), dialogClickListener).show();
            }

            @Override
            public void onItemMove(int from, int to) {
                Collections.swap(mDataset, from, to);
                notifyItemMoved(from, to);
            }
        }

        private class FunctionAdapter extends RecyclerView.Adapter<FunctionAdapter.ViewHolder> implements ItemTouchHelperAdapter {
            public ArrayList<Action> mDataset;

            // Provide a reference to the views for each data item
            // Complex data items may need more than one view per item, and
            // you provide access to all the views for a data item in a view holder
            protected class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
                // each data item is just a string in this case
                public TextView mTextView;
                public boolean isSelectable = true;

                public ViewHolder(TextView v) {
                    super(v, singleSelector);
                    v.setOnClickListener(this);
                    v.setOnLongClickListener(this);
                    v.setLongClickable(true);
                    mTextView = v;
                }

                @Override
                public void onClick(View view) {
                    if (!singleSelector.isSelectable() || !singleSelector.tapSelection(this)) {
                        Intent intentUpdate = new Intent();
                        intentUpdate.setAction("com.ha81dn.webausleser.ASYNC_MAIN");
                        intentUpdate.addCategory(Intent.CATEGORY_DEFAULT);
                        intentUpdate.putExtra("TAPITEM", "FUNCTION");
                        intentUpdate.putExtra("ID", mDataset.get(getPosition()).getId());
                        intentUpdate.putExtra("NAME", mDataset.get(getPosition()).getName());
                        mTextView.getContext().sendBroadcast(intentUpdate);
                    }
                }

                @Override
                public boolean onLongClick(View v) {
                    if (isSelectable()) {
                        AppCompatActivity activity = (AppCompatActivity) getActivity();
                        activity.startSupportActionMode(deleteMode);
                        singleSelector.setSelected(this, true);
                        return true;
                    }
                    return false;
                }

                @Override
                public boolean isSelectable() {
                    return isSelectable;
                }
            }

            // Provide a suitable constructor (depends on the kind of dataset)
            public FunctionAdapter(ArrayList<Action> myDataset) {
                mDataset = myDataset;
            }

            // Create new views (invoked by the layout manager)
            @Override
            public FunctionAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                 int viewType) {
                // create a new view
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.view_main, parent, false);
                // set the view's size, margins, paddings and layout parameters
                ViewHolder vh = new ViewHolder((TextView) v);
                return vh;
            }

            // Replace the contents of a view (invoked by the layout manager)
            @Override
            public void onBindViewHolder(ViewHolder holder, int position) {
                // - get element from your dataset at this position
                // - replace the contents of the view with that element
                if (mDataset.get(position).getId() == -1) {
                    holder.mTextView.setText(Html.fromHtml("<i>" + mDataset.get(position).getName() + "</i>"));
                    holder.isSelectable = false;
                }
                else
                    holder.mTextView.setText(mDataset.get(position).getName());
            }

            // Return the size of your dataset (invoked by the layout manager)
            @Override
            public int getItemCount() {
                return mDataset.size();
            }

            @Override
            public void onItemDismiss(int position) {
                mDataset.remove(position);
                notifyItemRemoved(position);
            }

            @Override
            public void onItemMove(int from, int to) {
                Collections.swap(mDataset, from, to);
                notifyItemMoved(from, to);
            }
        }

        private class ParamAdapter extends RecyclerView.Adapter<ParamAdapter.ViewHolder> implements ItemTouchHelperAdapter {
            public ArrayList<Param> mDataset;

            // Provide a reference to the views for each data item
            // Complex data items may need more than one view per item, and
            // you provide access to all the views for a data item in a view holder
            protected class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
                // each data item is just a string in this case
                public TextView mTextView;
                public boolean isSelectable = true;

                public ViewHolder(TextView v) {
                    super(v, singleSelector);
                    v.setOnClickListener(this);
                    v.setOnLongClickListener(this);
                    v.setLongClickable(false);
                    mTextView = v;
                }

                @Override
                public void onClick(View view) {
                    if (!singleSelector.isSelectable() || !singleSelector.tapSelection(this)) {
                        Intent intentUpdate = new Intent();
                        intentUpdate.setAction("com.ha81dn.webausleser.ASYNC_MAIN");
                        intentUpdate.addCategory(Intent.CATEGORY_DEFAULT);
                        intentUpdate.putExtra("TAPITEM", "PARAM");
                        intentUpdate.putExtra("ID", mDataset.get(getPosition()).getId());
                        mTextView.getContext().sendBroadcast(intentUpdate);
                    }
                }

                @Override
                public boolean onLongClick(View v) {
                    if (isSelectable()) {
                        AppCompatActivity activity = (AppCompatActivity) getActivity();
                        activity.startSupportActionMode(deleteMode);
                        singleSelector.setSelected(this, true);
                        return true;
                    }
                    return false;
                }

                @Override
                public boolean isSelectable() {
                    return isSelectable;
                }
            }

            // Provide a suitable constructor (depends on the kind of dataset)
            public ParamAdapter(ArrayList<Param> myDataset) {
                mDataset = myDataset;
            }

            // Create new views (invoked by the layout manager)
            @Override
            public ParamAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                              int viewType) {
                // create a new view
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.view_main, parent, false);
                // set the view's size, margins, paddings and layout parameters
                ViewHolder vh = new ViewHolder((TextView) v);
                return vh;
            }

            // Replace the contents of a view (invoked by the layout manager)
            @Override
            public void onBindViewHolder(ViewHolder holder, int position) {
                // - get element from your dataset at this position
                // - replace the contents of the view with that element
//        if (mDataset.get(position).getId() == -1)
//            holder.mTextView.setText(Html.fromHtml("<i>" + mDataset.get(position).getValue() + "</i>"));
//        else
                holder.mTextView.setText(mDataset.get(position).getValue());
            }

            // Return the size of your dataset (invoked by the layout manager)
            @Override
            public int getItemCount() {
                return mDataset.size();
            }

            @Override
            public void onItemDismiss(int position) {
                mDataset.remove(position);
                notifyItemRemoved(position);
            }

            @Override
            public void onItemMove(int from, int to) {
                Collections.swap(mDataset, from, to);
                notifyItemMoved(from, to);
            }
        }

        private class SourceAdapter extends RecyclerView.Adapter<SourceAdapter.ViewHolder> implements ItemTouchHelperAdapter {
            public ArrayList<Source> mDataset;

            // Provide a reference to the views for each data item
            // Complex data items may need more than one view per item, and
            // you provide access to all the views for a data item in a view holder
            protected class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
                // each data item is just a string in this case
                public TextView mTextView;
                public boolean isSelectable = true;

                public ViewHolder(TextView v) {
                    super(v, singleSelector);
                    v.setOnClickListener(this);
                    v.setOnLongClickListener(this);
                    v.setLongClickable(true);
                    mTextView = v;
                }

                @Override
                public void onClick(View view) {
                    if (!singleSelector.isSelectable() || !singleSelector.tapSelection(this)) {
                        Intent intentUpdate = new Intent();
                        intentUpdate.setAction("com.ha81dn.webausleser.ASYNC_MAIN");
                        intentUpdate.addCategory(Intent.CATEGORY_DEFAULT);
                        intentUpdate.putExtra("TAPITEM", "SOURCE");
                        intentUpdate.putExtra("ID", mDataset.get(getPosition()).getId());
                        intentUpdate.putExtra("NAME", mDataset.get(getPosition()).getName());
                        mTextView.getContext().sendBroadcast(intentUpdate);
                    }
                }

                @Override
                public boolean onLongClick(View v) {
                    if (isSelectable()) {
                        AppCompatActivity activity = (AppCompatActivity) getActivity();
                        activity.startSupportActionMode(deleteMode);
                        singleSelector.setSelected(this, true);
                        return true;
                    }
                    return false;
                }

                @Override
                public boolean isSelectable() {
                    return isSelectable;
                }
            }

            // Provide a suitable constructor (depends on the kind of dataset)
            public SourceAdapter(ArrayList<Source> myDataset) {
                mDataset = myDataset;
            }

            // Create new views (invoked by the layout manager)
            @Override
            public SourceAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                               int viewType) {
                // create a new view
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.view_main, parent, false);
                // set the view's size, margins, paddings and layout parameters
                ViewHolder vh = new ViewHolder((TextView) v);
                return vh;
            }

            // Replace the contents of a view (invoked by the layout manager)
            @Override
            public void onBindViewHolder(ViewHolder holder, int position) {
                // - get element from your dataset at this position
                // - replace the contents of the view with that element
                if (mDataset.get(position).getId() == -1) {
                    holder.mTextView.setText(Html.fromHtml("<i>" + mDataset.get(position).getName() + "</i>"));
                    holder.isSelectable = false;
                }
                else
                    holder.mTextView.setText(mDataset.get(position).getName());
            }

            // Return the size of your dataset (invoked by the layout manager)
            @Override
            public int getItemCount() {
                return mDataset.size();
            }

            @Override
            public void onItemDismiss(int position) {
                mDataset.remove(position);
                notifyItemRemoved(position);
            }

            @Override
            public void onItemMove(int from, int to) {
                Collections.swap(mDataset, from, to);
                notifyItemMoved(from, to);
            }
        }

        private class StepAdapter extends RecyclerView.Adapter<StepAdapter.ViewHolder> implements ItemTouchHelperAdapter {
            public ArrayList<Step> mDataset;

            // Provide a reference to the views for each data item
            // Complex data items may need more than one view per item, and
            // you provide access to all the views for a data item in a view holder
            protected class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
                // each data item is just a string in this case
                public TextView mTextView;
                public boolean isSelectable = true;

                public ViewHolder(TextView v) {
                    super(v, singleSelector);
                    v.setOnClickListener(this);
                    v.setOnLongClickListener(this);
                    v.setLongClickable(true);
                    mTextView = v;
                }

                @Override
                public void onClick(View view) {
                    if (!singleSelector.isSelectable() || !singleSelector.tapSelection(this)) {
                        Intent intentUpdate = new Intent();
                        intentUpdate.setAction("com.ha81dn.webausleser.ASYNC_MAIN");
                        intentUpdate.addCategory(Intent.CATEGORY_DEFAULT);
                        intentUpdate.putExtra("TAPITEM", "STEP");
                        intentUpdate.putExtra("ID", mDataset.get(getPosition()).getId());
                        intentUpdate.putExtra("CALL", mDataset.get(getPosition()).getCallFlag());
                        intentUpdate.putExtra("NAME", mDataset.get(getPosition()).getName());
                        mTextView.getContext().sendBroadcast(intentUpdate);
                    }
                }

                @Override
                public boolean onLongClick(View v) {
                    if (isSelectable()) {
                        AppCompatActivity activity = (AppCompatActivity) getActivity();
                        activity.startSupportActionMode(deleteMode);
                        singleSelector.setSelected(this, true);
                        return true;
                    }
                    return false;
                }

                @Override
                public boolean isSelectable() {
                    return isSelectable;
                }
            }

            // Provide a suitable constructor (depends on the kind of dataset)
            public StepAdapter(ArrayList<Step> myDataset) {
                mDataset = myDataset;
            }

            // Create new views (invoked by the layout manager)
            @Override
            public StepAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                             int viewType) {
                // create a new view
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.view_main, parent, false);
                // set the view's size, margins, paddings and layout parameters
                ViewHolder vh = new ViewHolder((TextView) v);
                return vh;
            }

            // Replace the contents of a view (invoked by the layout manager)
            @Override
            public void onBindViewHolder(ViewHolder holder, int position) {
                // - get element from your dataset at this position
                // - replace the contents of the view with that element
                if (mDataset.get(position).getId() == -1) {
                    holder.mTextView.setText(Html.fromHtml("<i>" + mDataset.get(position).getName() + "</i>"));
                    holder.isSelectable = false;
                }
                else
                    holder.mTextView.setText(mDataset.get(position).getName());
            }

            // Return the size of your dataset (invoked by the layout manager)
            @Override
            public int getItemCount() {
                return mDataset.size();
            }

            @Override
            public void onItemDismiss(int position) {
                mDataset.remove(position);
                notifyItemRemoved(position);
            }

            @Override
            public void onItemMove(int from, int to) {
                Collections.swap(mDataset, from, to);
                notifyItemMoved(from, to);
            }
        }

        private class mainBroadcastReceiver extends BroadcastReceiver {

            @Override
            public void onReceive(Context context, Intent intent) {
                int id;

                String tmp = intent.getStringExtra("TAPITEM");
                if (tmp != null)
                    handleTaps(context, tmp, intent.getIntExtra("ID", -1), intent.getStringExtra("NAME"));
                else {
                    tmp = intent.getStringExtra("INSERT");
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

                if (section.equals("SOURCE")) {
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
                    }
                    catch (Exception e){}
                } else if (section.equals("ACTION") || section.equals("FUNCTION")) {
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
                    }
                    catch (Exception e){}
                } else if (section.equals("STEP")) {
                    if (name.equals("") || insertParams==null) return;
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
                    vals.put("parent_id", -1);  // wird irgendwann kompliziert
                    try {
                        db.insert("steps", null, vals);
                    }
                    catch (Exception e){}

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
                        }
                        catch (Exception e){}
                    }
                }
                db.close();

                displaySection(context, section, id, name);
            }

            private void handleTaps(Context context, String section, int id, String name) {
                SQLiteDatabase db = DatabaseHandler.getInstance(context).getReadableDatabase();
                Cursor c;
                ItemTouchHelper touchHelper;
                ItemTouchHelper.Callback callback;
                String tmp = null;

                if (section.equals("ROOT")) {
                    ArrayList<Source> sourceDataset = new ArrayList<>();

                    db = DatabaseHandler.getInstance(context).getReadableDatabase();
                    c = db.rawQuery("select id, name from sources order by name", null);
                    if (c != null) {
                        if (c.moveToFirst()) {
                            do {
                                sourceDataset.add(new Source(c.getInt(0), c.getString(1)));
                            } while (c.moveToNext());
                        }
                        c.close();
                    }
                    db.close();
                    sourceDataset.add(new Source(-1, getString(R.string.addSource)));

                    mAdapter = new SourceAdapter(sourceDataset);
                    callback = new ItemTouchHelperCallback((SourceAdapter) mAdapter);
                    touchHelper = new ItemTouchHelper(callback);
                    touchHelper.attachToRecyclerView(mRecyclerView);
                    activeSection = "SOURCES";
                    navTitle.setText(getString(R.string.navTitleSources));
                } else if (section.equals("SOURCE")) {
                    if (id == -1) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle(getString(R.string.newSource));
                        builder.setMessage(getString(R.string.inputName));
                        final EditText input = new EditText(context);
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
                                    actionDataset.add(new Action(c.getInt(0), c.getInt(1), c.getInt(2), c.getString(3)));
                                } while (c.moveToNext());
                            }
                            c.close();
                        }
                        db.close();
                        actionDataset.add(new Action(-1, -1, -1, getString(R.string.addAction)));

                        mAdapter = new ActionAdapter(actionDataset);
                        callback = new ItemTouchHelperCallback((ActionAdapter) mAdapter);
                        touchHelper = new ItemTouchHelper(callback);
                        touchHelper.attachToRecyclerView(mRecyclerView);
                        activeSection = "ACTIONS";
                        sourceId = id;
                        sourceName = name;
                        navTitle.setText(getString(R.string.actionsFor, sourceName));
                    }
                } else if (section.equals("ACTION")) {
                    if (id == -1) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle(getString(R.string.newAction));
                        builder.setMessage(getString(R.string.inputName));
                        final EditText input = new EditText(context);
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

                        db = DatabaseHandler.getInstance(context).getReadableDatabase();
                        c = db.rawQuery("select id, action_id, sort_nr, function, call_flag, parent_id from steps where parent_id = -1 and action_id = ? order by sort_nr", new String[]{Integer.toString(id)});
                        if (c != null) {
                            if (c.moveToFirst()) {
                                do {
                                    String function = c.getString(3);
                                    int flag = c.getInt(4);
                                    if (flag == 1) {
                                        cF = db.rawQuery("select name from actions where action_id = ?", new String[]{function});
                                        tmp = "ERROR_FUNCTION_MISSING";
                                        if (cF != null) {
                                            if (cF.moveToFirst()) {
                                                tmp = cF.getString(0);
                                            }
                                            cF.close();
                                        }
                                    } else tmp = function;
                                    stepDataset.add(new Step(c.getInt(0), c.getInt(1), c.getInt(2), function, tmp, flag, c.getInt(5)));
                                } while (c.moveToNext());
                            }
                            c.close();
                        }
                        db.close();
                        stepDataset.add(new Step(-1, -1, -1, null, getString(R.string.addStep), -1, -1));

                        mAdapter = new StepAdapter(stepDataset);
                        callback = new ItemTouchHelperCallback((StepAdapter) mAdapter);
                        touchHelper = new ItemTouchHelper(callback);
                        touchHelper.attachToRecyclerView(mRecyclerView);
                        activeSection = "STEPS";
                        actionId = id;
                        actionName = name;
                        setTextViewHTML(navTitle, getString(R.string.stepsFor, actionName) + " (<a href='SRC'>" + sourceName + "</a>)");
                    }
                } else if (section.equals("STEP")) {
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
                        db.close();

                        mAdapter = new ParamAdapter(paramDataset);
                        callback = new ItemTouchHelperCallback((ParamAdapter) mAdapter);
                        touchHelper = new ItemTouchHelper(callback);
                        touchHelper.attachToRecyclerView(mRecyclerView);
                        activeSection = "PARAMS";
                        stepId = id;
                        stepName = name;
                        setTextViewHTML(navTitle, getString(R.string.paramsFor, stepName) + " (<a href='SRC'>" + sourceName + "</a> / <a href='ACT'>" + actionName + "</a>)");
                    }
                }

                if (mAdapter != null) mRecyclerView.setAdapter(mAdapter);
            }

            private void createStep(final ArrayList<Param> params) {
                final Context context = getActivity();
                final SQLiteDatabase db = DatabaseHandler.getInstance(context).getReadableDatabase();
                AlertDialog.Builder builder;
                AlertDialog dialog;
                int i;

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
                    }
                };

                if (stepId == -1) {

                    // Auswahl aus integrierten Schritten

                    // String-Arrays sortieren
                    final String steps[] = getResources().getStringArray(R.array.builtInSteps);
                    final String names[] = getResources().getStringArray(R.array.builtInStepsNames);
                    TreeMap<String, String> stepMap = new TreeMap<>();
                    for (i=0; i<steps.length; i++) {
                        stepMap.put(names[i], steps[i]);
                    }
                    i = 0;
                    for (Map.Entry<String, String> entry : stepMap.entrySet()) {
                        names[i] = entry.getKey();
                        steps[i] = entry.getValue();
                        i++;
                    }

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
                                            true,
                                            false,
                                            false,
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
                                            true,
                                            false,
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

                                    break;

                                case 1:

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

            private EditText createInput(Context context, boolean numericOnly) {
                EditText input = new EditText(context);
                if (numericOnly) input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                return input;
            }

            private void getChoosables(ArrayList<String> list, int lstCount[], SQLiteDatabase db, int sourceId, boolean showFixedValue, boolean showNewVariable, boolean showNewList) {
                ArrayList<String> lst = showNewList ? DatabaseHandler.getListsBySourceId(db, sourceId) : DatabaseHandler.getVariablesBySourceId(db, sourceId);
                if (showFixedValue) list.add(getString(R.string.fixedValue));
                if (showNewVariable) list.add(getString(R.string.newVariable));
                else if (showNewList) list.add(getString(R.string.newList));
                list.addAll(lst);
                lstCount[0] = list.size();
            }

            private void createParamByWizard(final Context context,
                                             SQLiteDatabase db,
                                             final ArrayList<Param> params,
                                             final boolean fixedValue,
                                             final boolean newVariable,
                                             final boolean newList,
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
                String optionList[] = null;

                if (fixedValue || newVariable || newList) {
                    getChoosables(lst, cnt, db, sourceId, fixedValue, newVariable, newList);
                    optionList = lst.toArray(new String[cnt[0]]);
                    singleInput = null;
                } else if (singleInputMessage != null) {
                    singleInput = createInput(context, numericOnly);
                } else {
                    singleInput = null;
                }
                showCreateStepWizard(context, title, singleInputMessage, finalDialogStartingAtId, optionList, singleInput, paramIdx==0 ? backFromFirstParam : backFromFurtherParam, cancelWizard,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                String msg="";
                                final int varFlag[] = new int[1];
                                final int lstFlag[] = new int[1];
                                boolean showInnerDialog=false;
                                if (selectedId >= 0) {
                                    if (fixedValue || newVariable || newList) {
                                        // eine Optionsliste wurde angeboten
                                        if (fixedValue && selectedId==0) {
                                            // "bestimmter Wert" wurde ausgewählt
                                            msg = getString(R.string.inputFixedValue);
                                            varFlag[0] = 0;
                                            lstFlag[0] = 0;
                                            showInnerDialog = true;
                                        } else if (newVariable && (!fixedValue && selectedId==0 || fixedValue && selectedId==1)) {
                                            // "neue Variable" wurde ausgewählt
                                            msg = getString(R.string.inputNewVariable);
                                            varFlag[0] = 1;
                                            lstFlag[0] = 0;
                                            showInnerDialog = true;
                                        } else if (newList && (!fixedValue && selectedId==0 || fixedValue && selectedId==1)) {
                                            // "neue Liste" wurde ausgewählt
                                            msg = getString(R.string.inputNewList);
                                            varFlag[0] = 0;
                                            lstFlag[0] = 1;
                                            showInnerDialog = true;
                                        } else {
                                            // bekannte Variable/Liste wurde ausgewählt
                                            proceedCreateStep(context, params, paramIdx, lst.get(selectedId), newVariable ? 1 : 0, newList ? 1 : 0, finalDialogStartingAtId);
                                        }
                                        if (showInnerDialog) {
                                            // Eingabedialog für bestimmten Wert bzw. neuen Variablen-/Listennamen zeigen
                                            final EditText input = createInput(context, numericOnly);
                                            showCreateStepWizard(context, title, msg, finalDialogStartingAtId==-1 ? -1 : 0, null, input, backFromInnerDialog, cancelWizard,
                                                    new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int id) {
                                                            proceedCreateStep(context, params, paramIdx, input.getText().toString().trim(), varFlag[0], lstFlag[0], finalDialogStartingAtId);
                                                        }
                                                    }
                                            );
                                        }
                                    } else if (singleInput != null) {
                                        // einzelne Eingabe
                                        proceedCreateStep(context, params, paramIdx, singleInput.getText().toString().trim(), 0, 0, finalDialogStartingAtId);
                                    }
                                } else {
                                    createStep(params);
                                }
                            }
                        });
            }

            private void proceedCreateStep(Context context, ArrayList<Param> params, int paramIdx, String paramValue, int varFlag, int lstFlag, int finalDialogStartingAtId) {
                params.add(new Param(-1, stepId, paramIdx, paramValue, varFlag, lstFlag));
                if (finalDialogStartingAtId == -1)
                    createStep(params);
                else
                    insertRow(context, "STEP", stepName, stepId, params);
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
                    selectedId = cnt==1 ? 0 : -1;
                    builder.setSingleChoiceItems(optionList, selectedId, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Button posButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                            posButton.setEnabled(true);
                            if (finalDialogStartingAtId>=0 && (id>=finalDialogStartingAtId ^ selectedId>=finalDialogStartingAtId)) {
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
                builder.setPositiveButton(finalDialogStartingAtId==-1 || selectedId==-1 || selectedId<finalDialogStartingAtId ? getString(R.string.next) : getString(R.string.finish), onClickNext);
                builder.setNegativeButton(getString(R.string.cancel), onClickCancel);
                if (onClickBack != null) builder.setNeutralButton(getString(R.string.back), onClickBack);
                dialog = builder.create();
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                dialog.show();
                if (optionList!=null && selectedId==-1) dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            }
        }
    }
}