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
		st.takePhoto(); // Expect: State Refinement Error
	}

	// Video is not in photoMode: only Photo falls through to it
	public static void capture(Mode mode) {
		ErrorEnumUsage st = new ErrorEnumUsage();
		switch (mode) {
			case Photo:
				st.setMode(Mode.Photo);
			case Video:
				st.takePhoto(); // Expect: State Refinement Error
				break;
			default:
				break;
		}
	}

	// The break inside the if leaves the switch before setMode
	public static void captureUnlessSkipped(Mode mode, boolean skip) {
		ErrorEnumUsage st = new ErrorEnumUsage();
		switch (mode) {
			case Video:
				if (skip)
					break;
				st.setMode(Mode.Photo);
				break;
			default:
				st.setMode(Mode.Photo);
		}
		st.takePhoto(); // Expect: State Refinement Error
	}
}