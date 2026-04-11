package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@SuppressWarnings("unused")
@StateSet({"photoMode", "videoMode", "noMode"})
class ErrorEnumUsage {
	enum Mode {
		Photo, Video, Unknown
	}

	Mode mode;
	@StateRefinement(to="noMode(this)")
	public ErrorEnumUsage() {}

	@StateRefinement(from="noMode(this) && mode == Mode.Photo", to="photoMode(this)")
	@StateRefinement(from="noMode(this) && mode == Mode.Video", to="videoMode(this)")
	public void setMode(Mode mode) {
		this.mode = mode;
	}

	@StateRefinement(from="photoMode(this)")
	public void takePhoto() {}
	
	
	public static void main(String[] args) {
		// Correct
		ErrorEnumUsage st = new ErrorEnumUsage();
		st.setMode(Mode.Video);
		st.takePhoto(); // State Refinement Error
	}
}