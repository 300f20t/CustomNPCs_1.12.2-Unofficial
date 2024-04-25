package noppes.npcs.client.util;

public class CrashesData {

	public boolean isActive = false, isFading = true, vector = false;
	public int time = 0, maxTime = 0, amplitude = 0, type = 0;
	public long endTime = 0;

	public float get(long time) {
		if (this.endTime == 0) {
			time *= -1;
			this.endTime = time + this.time;
		}
		if (time <= 0 || this.maxTime == 0 || this.amplitude == 0) {
			this.endTime = 0;
			this.isActive = false;
			return 0.0f;
		}
		float value;
		if (this.isFading) {
			value = (float) time * (float) this.amplitude / (float) this.maxTime;
		} else {
			value = (float) this.amplitude;
		}
		value *= this.vector ? 1.0f : -1.0f;
		this.vector = !this.vector;
		if (value == 0.0f) {
			this.endTime = 0;
			this.isActive = false;
		}
		return value;
	}

	public void set(int time, int amplitude, int type, boolean isFading) {
		if (time < 0) {
			time *= -1;
		}
		if (time > 1200) {
			time = 1200;
		}
		this.time = time;
		this.maxTime = time;
		if (amplitude < 0) {
			amplitude *= -1;
		}
		if (amplitude > 25) {
			amplitude = 25;
		}
		this.amplitude = amplitude;
		if (type < 0) {
			type *= -1;
		}
		this.type = type % 6;
		this.isFading = isFading;
		this.isActive = true;
		this.endTime = 0;
	}

}
