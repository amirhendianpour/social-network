package com.socialnetwork.social.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class BatchInfoRequest {
    private List<String> usernames;

}