package com.ha81dn.webausleser.backend.tables;

/**
 * Created by har on 04.01.2015.
 */
public class Param implements UniqueRecord {

    private int id;
    private int step_id;
    private int idx;
    private String value;
    private boolean variable_flag;
    private boolean list_flag;

    public Param(int id, int step_id, int idx, String value, int variable_flag, int list_flag) {
        this.id = id;
        this.step_id = step_id;
        this.idx = idx;
        this.value = value;
        this.variable_flag = variable_flag!=0;
        this.list_flag = list_flag!=0;
    }

    public int getId() {
        return id;
    }

    public int getStepId() {
        return step_id;
    }

    public void setStepId(int step_id) {
        this.step_id = step_id;
    }

    public int getIdx() {
        return idx;
    }

    public void setIdx(int idx) {
        this.idx = idx;
    }

    public String getValue() {
        return value;
    }

    public boolean getVariableFlag() {
        return variable_flag;
    }

    public boolean getListFlag() {
        return list_flag;
    }

    public void setValue(String value, boolean variable_flag, boolean list_flag) {
        this.value = value;
        this.variable_flag = variable_flag;
        this.list_flag = list_flag;
    }
}
