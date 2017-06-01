package common.task;

public interface TimeoutAction {
	void apply(int forwardSeconds, int remainedSeconds);
}
