package com.truphone.es9plus;

public class HttpResponse {
    private String content;
    private int statusCode;

    public String getContent() {

        return content;
    }

    public void setContent(String content) {

        this.content = content;
    }

    public int getStatusCode() {

        return statusCode;
    }

    public void setStatusCode(int statusCode) {

        this.statusCode = statusCode;
    }
}
