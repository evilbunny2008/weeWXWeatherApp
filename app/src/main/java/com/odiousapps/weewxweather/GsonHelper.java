package com.odiousapps.weewxweather;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.util.List;

import androidx.annotation.NonNull;

class GsonHelper
{
	List<Day> days;
	String desc;
	long timestamp;
	boolean isDaily;

	public GsonHelper(List<Day> days, String desc, long timestamp, boolean isDaily)
	{
		this.days = days;
		this.desc = desc;
		this.timestamp = timestamp;
		this.isDaily = isDaily;
	}

	public GsonHelper(String json)
	{
		Gson gson = new Gson();
		JsonObject obj = gson.fromJson(json, JsonObject.class);

		this.desc = obj.get("desc").getAsString();
		this.timestamp = obj.get("timestamp").getAsLong();
		this.isDaily = obj.get("isDaily").getAsBoolean();
		this.days = gson.fromJson(obj.get("days"), new TypeToken<List<Day>>(){}.getType());
	}

	@NonNull
	public String toString()
	{
		return new Gson().toJson(this);
	}
}