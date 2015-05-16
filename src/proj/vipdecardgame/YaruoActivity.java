package proj.vipdecardgame;

import proj.vipdecardgame.model.GameModel;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public class YaruoActivity extends ActionBarActivity {

	GameModel gm;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_game);

		Intent i = this.getIntent();
		int tutorialNum = i.getIntExtra("tutorialNum", 0);
		this.prepareTutorial(tutorialNum);
	}

	/*** 画面更新 ***/
	public void updateView() {

	}

	/*** チュートリアル準備 ***/
	public void prepareTutorial(int tutorialNum) {
		switch (tutorialNum) {
		case 0:
			break;
		case 1:
			break;
		case 2:
			break;
		case 3:
			break;
		case 4:
			break;
		case 5:
			break;
		case 6:
			break;
		case 7:
			break;
		case 8:
			break;
		}
	}
}
