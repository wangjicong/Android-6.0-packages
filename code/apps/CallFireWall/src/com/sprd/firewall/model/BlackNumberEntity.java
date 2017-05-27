
package com.sprd.firewall.model;

public class BlackNumberEntity {

    private Integer id;

    private String number;

    private Integer type;

    private String notes;

    private String name;

    private String min_match;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMinmatch() {
        return min_match;
    }

    public void setMinmatch(String number) {
        this.min_match = number;
    }
}
