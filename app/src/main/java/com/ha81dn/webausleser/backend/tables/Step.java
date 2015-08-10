package com.ha81dn.webausleser.backend.tables;

/**
 * Created by har on 04.01.2015.
 */
public class Step implements UniqueRecord {

    private int id;
    private int action_id;
    private int sort_nr;
    private int parent_id;
    private String function;
    private String name;
    private boolean call_flag;

    public Step(int id, int action_id, int sort_nr, String function, String name, int call_flag, int parent_id) {
        this.id = id;
        this.action_id = action_id;
        this.sort_nr = sort_nr;
        this.function = function;
        if (name == null)
            this.name = this.function;
        else
            this.name = name;
        this.call_flag = call_flag!=0;
        this.parent_id = parent_id;
    }

    public int getId() {
        return id;
    }

    public int getActionId() {
        return action_id;
    }

    public void setActionId(int action_id) {
        this.action_id = action_id;
    }

    public int getSortNr() {
        return sort_nr;
    }

    public void setSortNr(int sort_nr) {
        this.sort_nr = sort_nr;
    }

    public String getFunction() {
        return function;
    }

    public String getName() {
        return name;
    }

    public int getParentId() {
        return parent_id;
    }

    public void setParentId(int parent_id) {
        this.sort_nr = sort_nr;
    }

    public boolean getCallFlag() {
        return call_flag;
    }

    public void setFunction(String function, boolean call_flag) {
        this.function = function;
        this.call_flag = call_flag;
    }
}
