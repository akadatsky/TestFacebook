package net.anber.testfacebook.model;

public class FacebookPost {

    private String message;
    private String picture;
    private String id;
    private String created_time;
    private String type;
    private String object_id;

    public FacebookPost() {
    }

    public String getMessage() {
        return message;
    }

    public String getPicture() {
        return picture;
    }

    public String getId() {
        return id;
    }

    public String getCreated_time() {
        return created_time;
    }

    public String getType() {
        return type;
    }

    public String getObject_id() {
        return object_id;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }
}
