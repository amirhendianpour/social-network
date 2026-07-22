package com.socialnetwork.social.dto;

import java.util.List;

public class BatchInfoRequest {
    private List<String> usernames;

    public List<String> getUsernames() { return usernames; }
    public void setUsernames(List<String> usernames) { this.usernames = usernames; }
}