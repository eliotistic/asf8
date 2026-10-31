package computingmusic.chordapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import computingmusic.AbcPitch;
import computingmusic.chord.ChordBoard;
import computingmusic.hand.Geometry;
import computingmusic.instrument.InstrumentSpec;

/**
 * One fingerboard, nut at the top, the strings from the lowest at the left; the height of a stop is drawn at its true distance from the nut
 * (semitones close up towards the bridge, as on the instrument).  A stopped note is a red disc with its finger; an open string a ring
 * above the nut; fingers that share a height (a finger laid across strings) are joined by a bar.
 */
final class BoardView extends View {
	private InstrumentSpec spec = InstrumentSpec.VIOLIN;
	private ChordBoard.Option option;
	private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final float dp;

	BoardView(Context c) {
		super(c);
		dp = c.getResources().getDisplayMetrics().density;
	}

	void show(InstrumentSpec spec, ChordBoard.Option option) {
		this.spec = spec;
		this.option = option;
		invalidate();
	}

	@Override
	protected void onDraw(Canvas g) {
		int w = getWidth(), h = getHeight();
		Geometry geo = spec.geometry();
		int n = spec.openPitches.length;
		float left = 28 * dp, right = w - 28 * dp, top = 62 * dp, bottom = h - 12 * dp;
		int hMax = 12;
		if (option != null) {
			for (ChordBoard.Place pl : option.places) {
				hMax = Math.max(hMax, pl.height + 2);
			}
		}
		float scale = (bottom - top) / (float) geo.distance(hMax);
		float[] x = new float[n];
		for (int s = 0; s < n; s++) {
			x[s] = left + (right - left) * s / (n - 1);
		}
		p.setStyle(Paint.Style.FILL);
		p.setColor(Color.rgb(246, 226, 190));
		g.drawRect(left - 12 * dp, top, right + 12 * dp, bottom, p);
		p.setStyle(Paint.Style.STROKE);
		p.setColor(Color.rgb(200, 180, 150));
		p.setStrokeWidth(1);
		for (int k = 1; k <= hMax; k++) {
			float y = top + (float) geo.distance(k) * scale;
			p.setColor(k % 12 == 0 ? Color.rgb(90, 70, 40) : Color.rgb(222, 200, 165));
			g.drawLine(left - 12 * dp, y, right + 12 * dp, y, p);
		}
		p.setColor(Color.BLACK);
		p.setStrokeWidth(2 * dp);
		g.drawLine(left - 12 * dp, top, right + 12 * dp, top, p);
		p.setStrokeWidth(1.5f * dp);
		for (int s = 0; s < n; s++) {
			g.drawLine(x[s], top, x[s], bottom, p);
		}
		p.setStyle(Paint.Style.FILL);
		p.setTextAlign(Paint.Align.CENTER);
		p.setTextSize(16 * dp);
		for (int s = 0; s < n; s++) {
			g.drawText(spec.stringNames[s], x[s], 18 * dp, p);
		}
		if (option == null) {
			return;
		}
		float r = 15 * dp;
		java.util.Map<Integer, float[]> bars = new java.util.TreeMap<Integer, float[]>();
		for (ChordBoard.Place pl : option.places) {
			if (pl.height > 0) {
				float[] b = bars.get(pl.height);
				bars.put(pl.height, b == null ? new float[] { x[pl.string], x[pl.string] } : new float[] { Math.min(b[0], x[pl.string]), Math.max(b[1], x[pl.string]) });
			}
		}
		p.setColor(Color.rgb(200, 40, 40));
		for (java.util.Map.Entry<Integer, float[]> e : bars.entrySet()) {
			if (e.getValue()[1] > e.getValue()[0]) {
				float y = top + (float) geo.distance(e.getKey()) * scale;
				g.drawRoundRect(new RectF(e.getValue()[0] - r, y - r * 0.8f, e.getValue()[1] + r, y + r * 0.8f), r, r, p);
			}
		}
		for (int k = 0; k < option.places.length; k++) {
			ChordBoard.Place pl = option.places[k];
			float cx = x[pl.string];
			String name = AbcPitch.name(pl.midi, isBlack(pl.midi) ? 1 : 0);
			if (pl.height == 0) {
				p.setStyle(Paint.Style.STROKE);
				p.setColor(Color.BLACK);
				p.setStrokeWidth(2 * dp);
				g.drawCircle(cx, top - 16 * dp, 8 * dp, p);
				p.setStyle(Paint.Style.FILL);
				continue;
			}
			float cy = top + (float) geo.distance(pl.height) * scale;
			p.setColor(Color.rgb(200, 40, 40));
			g.drawCircle(cx, cy, r, p);
			p.setColor(Color.WHITE);
			p.setTextSize(17 * dp);
			g.drawText(String.valueOf(option.fingers[k]), cx, cy + 6 * dp, p);
			p.setColor(Color.rgb(60, 60, 60));
			p.setTextSize(12 * dp);
			g.drawText(name, cx, cy + r + 13 * dp, p);
		}
	}

	private static boolean isBlack(int midi) {
		int pc = midi % 12;
		return pc == 1 || pc == 3 || pc == 6 || pc == 8 || pc == 10;
	}
}
