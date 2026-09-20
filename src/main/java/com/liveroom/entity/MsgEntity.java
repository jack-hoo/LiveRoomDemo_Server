package com.liveroom.entity;

import java.io.Serializable;
import java.util.Calendar;

/**
 * 一条聊天消息，通过 STOMP 广播给直播间里的所有人。
 */
public class MsgEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private String creator;
    private String msgBody;
    private Calendar sTime;

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getMsgBody() {
        return msgBody;
    }

    public void setMsgBody(String msgBody) {
        this.msgBody = msgBody;
    }

    public Calendar getsTime() {
        return sTime;
    }

    public void setsTime(Calendar sTime) {
        this.sTime = sTime;
    }

    @Override
    public String toString() {
        return "MsgEntity{creator='" + creator + "', msgBody='" + msgBody + "', sTime=" + sTime + "}";
    }
}
