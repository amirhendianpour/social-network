package com.socialnetwork.social.dto;

public class CallSignal {
    private String type;      // OFFER, ANSWER, ICE_CANDIDATE, END, REJECT, BUSY
    private String from;
    private String to;
    private String sdp;       // برای OFFER و ANSWER
    private String candidate; // برای ICE_CANDIDATE (JSON رشته‌ای شده)
    private String callId;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getSdp() { return sdp; }
    public void setSdp(String sdp) { this.sdp = sdp; }

    public String getCandidate() { return candidate; }
    public void setCandidate(String candidate) { this.candidate = candidate; }

    public String getCallId() { return callId; }
    public void setCallId(String callId) { this.callId = callId; }
}