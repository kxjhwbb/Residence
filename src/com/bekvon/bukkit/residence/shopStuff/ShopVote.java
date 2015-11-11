package com.bekvon.bukkit.residence.shopStuff;

public class ShopVote {

    String name = null;
    int vote = -1;
    long time = 0L;

    public ShopVote(String name, int vote, long time) {
	this.name = name;
	this.vote = vote;
	this.time = time;
    }

    public String getName() {
	return this.name;
    }

    public int getVote() {
	return this.vote;
    }

    public void setVote(int vote) {
	this.vote = vote;
    }

    public long getTime() {
	if (this.time == 0)
	    this.time = System.currentTimeMillis();
	return this.time;
    }

    public void setTime(long time) {
	this.time = time;
    }
}
