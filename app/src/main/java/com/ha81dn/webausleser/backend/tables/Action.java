package com.ha81dn.webausleser.backend.tables;

/**
 * Created by har on 04.01.2015.
 */
public class Action implements UniqueRecord {

    private int id;
    private int source_id;
    private int sort_nr;
    private String name;

    public Action(int id, int source_id, int sort_nr, String name) {
        this.id = id;
        this.source_id = source_id;
        this.sort_nr = sort_nr;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public int getSourceId() {
        return source_id;
    }

    public void setSourceId(int source_id) {
        this.source_id = source_id;
    }

    public int getSortNr() {
        return sort_nr;
    }

    public void setSortNr(int sort_nr) {
        this.sort_nr = sort_nr;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
