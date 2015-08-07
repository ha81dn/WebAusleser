package com.ha81dn.webausleser;

import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Holger Arndt on 26.07.2015.
 */
public abstract class SelectableAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    @SuppressWarnings("unused")
    private static final String TAG = SelectableAdapter.class.getSimpleName();

    private SparseBooleanArray selectedItems;

    public SelectableAdapter() {
        selectedItems = new SparseBooleanArray();
    }

    /**
     * Indicates if the item at position position is selected
     *
     * @param position Position of the item to check
     * @return true if the item is selected, false otherwise
     */
    public boolean isSelected(int position) {
        return getSelectedItems().contains(position);
    }

    /**
     * Toggle the selection status of the item at a given position
     *
     * @param position Position of the item to toggle the selection status for
     */
    public void toggleSelection(int position) {
        doToggleSelection(position, true);
    }

    public void toggleSelection(int position, boolean suppressNotify) {
        doToggleSelection(position, !suppressNotify);
    }

    private void doToggleSelection(int position, boolean notify) {
        if (selectedItems.get(position, false)) {
            selectedItems.delete(position);
        } else {
            selectedItems.put(position, true);
        }
        if (notify) {
            notifyItemChanged(position);
            toggledSelection(position);
        }
    }

    public void selectAll(int size) {
        for (int i = 0; i < size; i++) {
            if (selectedItems.indexOfKey(i) < 0) toggleSelection(i);
        }
    }

    public void reorgAfterMove(int from, int to) {
        int i;
        boolean valFrom = selectedItems.get(from, false);
        if (from < to)
            for (i = from; i < to; i++)
                if (selectedItems.get(i + 1, false))
                    selectedItems.put(i, true);
                else
                    selectedItems.delete(i);
        else
            for (i = from; i > to; i--)
                if (selectedItems.get(i - 1, false))
                    selectedItems.put(i, true);
                else
                    selectedItems.delete(i);
        if (valFrom)
            selectedItems.put(to, true);
        else
            selectedItems.delete(to);
    }

    public void toggledSelection(int position) {
    }

    /**
     * Clear the selection status for all items
     */
    public void clearSelection() {
        List<Integer> selection = getSelectedItems();
        selectedItems.clear();
        for (Integer i : selection) {
            notifyItemChanged(i);
        }
    }

    /**
     * Count the selected items
     *
     * @return Selected items count
     */
    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    /**
     * Indicates the list of selected items
     *
     * @return List of selected items ids
     */
    public List<Integer> getSelectedItems() {
        List<Integer> items = new ArrayList<>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); ++i) {
            items.add(selectedItems.keyAt(i));
        }
        return items;
    }
}
