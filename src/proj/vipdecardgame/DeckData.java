package proj.vipdecardgame;

import java.io.Serializable;
import java.util.ArrayList;

public class DeckData implements Serializable {
	private static final long serialVersionUID = 1L;
	String deckName;
	ArrayList<String> cardIDStrings;

	public DeckData() {
		cardIDStrings = new ArrayList<String>();
	}
}
