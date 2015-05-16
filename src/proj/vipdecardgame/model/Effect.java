package proj.vipdecardgame.model;

import org.json.JSONArray;

public class Effect {
	private int playId;
	private int playRow, playColumn;
	private JSONArray script;
	private boolean isTriggered = false;

	private int[] store;

	public Effect(JSONArray script) {
		this.script = script;
		this.store = new int[100];
	}

	public JSONArray triggered(int playId, int row, int column) {
		this.playId = playId;
		this.playRow = row;
		this.playColumn = column;
		this.isTriggered = true;
		return this.script;
	}

	public JSONArray againTriggerd() {
		if (isTriggered)
			return this.script;
		else
			return null;
	}

	public void initStore(int index, int val) {
		this.store[index] = val;
	}

	public void addStore(int index, int val) {
		this.store[index] += val;
	}

	public int getStore(int index) {
		return this.store[index];
	}

	public int getPlayId() {
		return this.playId;
	}

	public int getTriggeredRow() {
		return this.playRow;
	}

	public int getTriggeredColumn() {
		return this.playColumn;
	}
}
