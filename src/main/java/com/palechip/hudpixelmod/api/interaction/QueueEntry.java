package com.palechip.hudpixelmod.api.interaction;

import java.util.ArrayList;

import net.hypixel.api.HypixelAPI;
import net.hypixel.api.reply.BoostersReply;
import net.hypixel.api.reply.FriendsReply;
import net.hypixel.api.reply.PlayerReply;
import net.hypixel.api.reply.SessionReply;
import net.hypixel.api.util.Callback;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.palechip.hudpixelmod.api.interaction.callbacks.BoosterResponseCallback;
import com.palechip.hudpixelmod.api.interaction.callbacks.FriendResponseCallback;
import com.palechip.hudpixelmod.api.interaction.callbacks.PlayerResponseCallback;
import com.palechip.hudpixelmod.api.interaction.callbacks.SessionResponseCallback;
import com.palechip.hudpixelmod.api.interaction.representations.Booster;
import com.palechip.hudpixelmod.api.interaction.representations.Friend;
import com.palechip.hudpixelmod.api.interaction.representations.Player;
import com.palechip.hudpixelmod.api.interaction.representations.Session;

public class QueueEntry {
    private boolean isSecondTry;
    private long creationTime;
    
    private BoosterResponseCallback boosterCallback;
    private SessionResponseCallback sessionCallback;
    private FriendResponseCallback friendCallback;
    private PlayerResponseCallback playerCallback;
    private String player;
    
    /**
     * This queue entry will perform a getBoosters request
     * @param callback
     */
    public QueueEntry(BoosterResponseCallback callback) {
        this.boosterCallback = callback;
        this.creationTime = System.currentTimeMillis();
    }
    
    /**
     * This queue entry will perform a getSession request
     * @param callback
     */
    public QueueEntry(SessionResponseCallback callback, String player) {
        this.sessionCallback = callback;
        this.player = player;
        this.creationTime = System.currentTimeMillis();
    }
    
    /**
     * This queue entry will perform a getFriends request
     * @param callback
     */
    public QueueEntry(FriendResponseCallback callback, String player) {
        this.friendCallback = callback;
        this.player = player;
        this.creationTime = System.currentTimeMillis();
    }
    
    public QueueEntry(PlayerResponseCallback callback, String player) {
        this.playerCallback = callback;
        this.player = player;
        this.creationTime = System.currentTimeMillis();
    }
    
    public void run() {
        // is it a booster request?
        if(this.boosterCallback != null) {
            this.doBoosterRequest();
        } else if(this.sessionCallback != null) {
            this.doSessionRequest();
        } else if(this.friendCallback != null) {
            this.doFriendRequest();
        } else if(this.playerCallback != null) {
            this.doPlayerRequest();
        }
    }
    
    private void failed(Throwable failCause) {
        Queue.getInstance().reportFailure(failCause, this.isSecondTry);
        if(this.isSecondTry) {
            this.cancel();
        } else {
            // retry
            this.isSecondTry = true;
            this.run();
        }
    }
    
    public void cancel() {
        if(this.boosterCallback != null) {
            this.boosterCallback.onBoosterResponse(null);
        } else if(this.sessionCallback != null) {
            this.sessionCallback.onSessionRespone(null);
        } else if(this.friendCallback != null) {
            this.friendCallback.onFriendResponse(null);
        }
        // open the way for the next request
        Queue.getInstance().unlockQueue();
    }
    
    public long getCreationTime() {
        return this.creationTime;
    }
    
    private void doBoosterRequest() {
        HypixelAPI api = Queue.getInstance().getAPI();
        // do the request
        api.getBoosters(new Callback<BoostersReply>(BoostersReply.class) {
            @Override
            public void callback(Throwable failCause, BoostersReply result) {
                if(failCause != null) {
                    // if something went wrong, handle it
                    failed(failCause);
                } else {
                    // assemble the response
                    ArrayList<Booster> boosters = new ArrayList<Booster>();
                    Gson gson = Queue.getInstance().getGson();
                    // the response for the booster query is an array with objects
                    // these objects are represented by the class Booster
                    for(JsonElement e : result.getBoosters()) {
                        Booster b = gson.fromJson(e, Booster.class);
                        boosters.add(b);
                    }
                    // pass the result
                    boosterCallback.onBoosterResponse(boosters);
                    // open the way for the next request
                    Queue.getInstance().unlockQueue();
                }
            }
        });
    }
    
    private void doSessionRequest() {
        HypixelAPI api = Queue.getInstance().getAPI();
        // do the request
        api.getSession(this.player, new Callback<SessionReply>(SessionReply.class) {
            @Override
            public void callback(Throwable failCause, SessionReply result) {
                if (failCause != null) {
                    // if something went wrong, handle it
                    failed(failCause);
                } else {
                    // assemble the response
                    Gson gson = Queue.getInstance().getGson();
                    // the class session represents the entire request
                    Session s = gson.fromJson(result.getSession(), Session.class);
                    if(s == null) {
                        // there was no session, so lets return a session with everything null except the player
                        s = new Session();
                    }
                    s.setSessionOwner(player);
                    // pass the result
                    sessionCallback.onSessionRespone(s);
                    // open the way for the next request
                    Queue.getInstance().unlockQueue();
                }
            }
        });
    }
    
    private void doFriendRequest() {
        HypixelAPI api = Queue.getInstance().getAPI();
        // do the request
        api.getFriends(this.player, new Callback<FriendsReply>(FriendsReply.class) {
            
            @Override
            public void callback(Throwable failCause, FriendsReply result) {
                if (failCause != null) {
                    // if something went wrong, handle it
                    failed(failCause);
                } else {
                    // assemble the response
                    ArrayList<Friend> friends = new ArrayList<Friend>();
                    Gson gson = Queue.getInstance().getGson();
                    // the response for the booster query is an array with objects
                    // these objects are represented by the class Booster
                    for(JsonElement e : result.getRecords()) {
                        Friend f = gson.fromJson(e, Friend.class);
                        f.setPlayer(player);
                        friends.add(f);
                    }
                    // pass the result
                    friendCallback.onFriendResponse(friends);
                    // open the way for the next request
                    Queue.getInstance().unlockQueue();
                }
            }
        });
    }
    
    private void doPlayerRequest() {
        HypixelAPI api = Queue.getInstance().getAPI();
        // do the request
        api.getPlayer(player, null ,new Callback<PlayerReply>(PlayerReply.class) {
            @Override
            public void callback(Throwable failCause, PlayerReply result) {
                if(failCause != null) {
                    // if something went wrong, handle it
                    failed(failCause);
                } else {
                    // assemble the response
                    Gson gson = Queue.getInstance().getGson();
                    Player player = gson.fromJson(result.getPlayer(), Player.class);
                    // pass the result
                    playerCallback.onPlayerResponse(player);
                    // open the way for the next request
                    Queue.getInstance().unlockQueue();
                }
            }
        });
    }
}
