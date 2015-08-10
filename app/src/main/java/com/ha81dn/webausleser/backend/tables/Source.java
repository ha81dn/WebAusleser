package com.ha81dn.webausleser.backend.tables;

/**
 * Created by har on 04.01.2015.
 */
public class Source implements UniqueRecord {

    private int id;
    private String name;

    public Source(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
